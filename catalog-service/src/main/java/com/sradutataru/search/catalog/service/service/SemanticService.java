package com.sradutataru.search.catalog.service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sradutataru.search.catalog.service.dto.ProductDto;
import com.sradutataru.search.catalog.service.dto.ProductResponse;
import com.sradutataru.search.catalog.service.dto.SemanticStage;
import com.sradutataru.search.catalog.service.dto.TagDto;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sradutataru.search.catalog.service.dto.TagDto.MatchType.EXACT;
import static com.sradutataru.search.catalog.service.dto.TagDto.MatchType.SPELLCHECK;
import static com.sradutataru.search.catalog.service.dto.TagDto.MatchType.UNRECOGNISED;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

@Service
@RequiredArgsConstructor
public class SemanticService {

    private static final String TAGS_INDEX = "semantic-tags";
    public static final HighlightBuilder HIGHLIGHT_BUILDER = new HighlightBuilder().field("tag").preTags("<em>").postTags("</em>");
    private static final String INDEX = "catalog-index_live";

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    public ProductResponse semanticSearch(String query, Integer count, Integer page, Map<String, String> attributeFilters) {
        try {
            List<TagDto> tags = getRecognisedTags(query);
            List<SemanticStage> stages = loadSemanticConfig();
            float matchPercent = (float) tags.stream().filter(tag -> !UNRECOGNISED.equals(tag.getMatchType())).count() / tags.size();
            for (SemanticStage stage : stages) {
                if(matchPercent < stage.getMinMatchPercent()) {
                    continue;
                }
                DisMaxQueryBuilder stageQuery = buildStageQuery(tags, stage, attributeFilters);
                if(stageQuery == null) {
                    continue;
                }
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                        .query(stageQuery)
                        .from((page - 1) * count)
                        .size(count)
                        .explain(true);
                SearchRequest searchRequest = new SearchRequest(INDEX).source(sourceBuilder);
                SearchResponse sr = client.search(searchRequest, DEFAULT);
                long totalHits = sr.getHits().getTotalHits().value;
                if (totalHits > 0) {
                    List<ProductDto> products = new ArrayList<>();
                    for (SearchHit hit : sr.getHits().getHits()) {
                        ProductDto product = objectMapper.readValue(hit.getSourceAsString(), ProductDto.class);
                        products.add(product);
                    }
                    return new ProductResponse(query, totalHits, attributeFilters, products, count, page, stage.getName());
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Semantic search failed", e);
        }
    }

    private DisMaxQueryBuilder buildStageQuery(List<TagDto> recognizedTags, SemanticStage stage, Map<String, String> attributeFilters) {
        Map<String, List<TagDto>> tagPerKeyword = recognizedTags.stream().collect(Collectors.toMap(tag->tag.getOriginalToken().toLowerCase(), List::of, (t1, t2) -> {
            List<TagDto> tags = new ArrayList<>();
            tags.addAll(t1);
            tags.addAll(t2);
            return tags;
        }));
        DisMaxQueryBuilder boolQuery = QueryBuilders.disMaxQuery().tieBreaker(0.5f);
        float matchedTagsInStage = 0;
        List<TagDto> unrecognizedTags = new ArrayList<>();
        for (Map.Entry<String, List<TagDto>> tag : tagPerKeyword.entrySet()) {
            DisMaxQueryBuilder innerDisMax = QueryBuilders.disMaxQuery().tieBreaker(0.2f);
            boolean matched = false;
            for(TagDto tagDto : tag.getValue()) {
                if(tagDto.getMatchType().equals(UNRECOGNISED)) {
                    unrecognizedTags.add(tagDto);
                }
                String fieldVariant = tagDto.getField();
                if(!fieldVariant.startsWith("attribute")) {
                    fieldVariant += "." + tagDto.getType();
                }
                float fieldBoostFromStage = getFieldBoostFromStage(stage, fieldVariant);
                if (fieldBoostFromStage != -1) {
                    matched = true;
                    float boost = fieldBoostFromStage;
                    if(tagDto.getMatchType().equals(SPELLCHECK)) {
                        boost /= 2;
                    }
                    innerDisMax.add(QueryBuilders.termQuery(fieldVariant, tagDto.getTag()).boost(boost));
                }
            }
            if(matched) {
                matchedTagsInStage++;
                boolQuery.add(innerDisMax);
            }
        }

        if (attributeFilters != null && !attributeFilters.isEmpty()) {
            attributeFilters.forEach((key, value) ->
                    boolQuery.add(QueryBuilders.termQuery("attributes." + key, value))
            );
        }
        if(matchedTagsInStage / tagPerKeyword.keySet().size() < stage.getMinMatchPercent()) {
            return null;
        }
        if(!unrecognizedTags.isEmpty()) {
            boolQuery.add(QueryBuilders.moreLikeThisQuery(stage.getFields().keySet().toArray(String[]::new), unrecognizedTags.stream().map(TagDto::getOriginalToken).toArray(String[]::new), null));
        }
        return boolQuery;
    }

    private static float getFieldBoostFromStage(SemanticStage stage, String fieldVariant) {
        if(stage.getFields().containsKey(fieldVariant)) {
            return stage.getFields().get(fieldVariant);
        }
        Optional<Map.Entry<String, Float>> dynamicBoost = stage.getDynamicFields().entrySet().stream().filter(entry -> Regex.compile(entry.getKey(), null).matcher(fieldVariant).matches()).findFirst();
        if(dynamicBoost.isPresent()) {
            return dynamicBoost.get().getValue();
        }
        return -1;
    }

    public List<TagDto> getRecognisedTags(String query) {
        List<TagDto> recognizedTags = new ArrayList<>();
        Set<String> matchedTokens = new HashSet<>();
        String[] tokens = query.trim().split("\\s+");

        try {
            Arrays.stream(tokens).forEach(token -> {
                try {
                    BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
                    queryBuilder.should(QueryBuilders.matchQuery("tag", token));
                    SearchSourceBuilder exactSource = new SearchSourceBuilder()
                            .query(queryBuilder)
                            .size(100)
                            .highlighter(HIGHLIGHT_BUILDER);
                    SearchRequest exactRequest = new SearchRequest(TAGS_INDEX).source(exactSource);
                    SearchResponse exactResponse = client.search(exactRequest, RequestOptions.DEFAULT);
                    for (SearchHit hit : exactResponse.getHits().getHits()) {
                        TagDto tag = objectMapper.readValue(hit.getSourceAsString(), TagDto.class);
                        Map<String, HighlightField> highlights = hit.getHighlightFields();
                        if (highlights.containsKey("tag")) {
                            removeMatchedKeywordsFromQuery(matchedTokens, tag, highlights);
                        } else {
                            matchedTokens.add(tag.getTag().toLowerCase());
                        }
                        tag.setMatchType(EXACT);
                        String matchedTag = tag.getTag().replaceAll("<em>", "").replaceAll("</em>", "");
                        tag.setTag(matchedTag);
                        tag.setOriginalToken(token);
                        recognizedTags.add(tag);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            SearchSourceBuilder phraseSource = new SearchSourceBuilder()
                    .query(QueryBuilders.matchPhraseQuery("tag", query))
                    .size(100)
                    .highlighter(HIGHLIGHT_BUILDER);
            SearchRequest phraseRequest = new SearchRequest(TAGS_INDEX).source(phraseSource);
            SearchResponse phraseResponse = client.search(phraseRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : phraseResponse.getHits().getHits()) {
                TagDto tag = objectMapper.readValue(hit.getSourceAsString(), TagDto.class);
                Map<String, HighlightField> highlights = hit.getHighlightFields();
                if (highlights.containsKey("tag")) {
                    removeMatchedKeywordsFromQuery(matchedTokens, tag, highlights);
                }
                tag.setMatchType(EXACT);
                String matchedTag = tag.getTag().replaceAll("<em>", "").replaceAll("</em>", "");
                tag.setTag(matchedTag);
                tag.setOriginalToken(matchedTag);
                recognizedTags.add(tag);
            }
            Arrays.stream(tokens).forEach(token -> {
                try {
                    BoolQueryBuilder fuzzyQueryBuilder = QueryBuilders.boolQuery();
                    fuzzyQueryBuilder.must(QueryBuilders.matchQuery("tag", token).fuzziness("AUTO"));
                    SearchSourceBuilder fuzzySource = new SearchSourceBuilder()
                            .query(fuzzyQueryBuilder)
                            .size(100)
                            .highlighter(HIGHLIGHT_BUILDER);
                    SearchRequest fuzzyRequest = new SearchRequest(TAGS_INDEX).source(fuzzySource);
                    SearchResponse fuzzyResponse = client.search(fuzzyRequest, RequestOptions.DEFAULT);
                    for (SearchHit hit : fuzzyResponse.getHits().getHits()) {
                        TagDto tag = objectMapper.readValue(hit.getSourceAsString(), TagDto.class);
                        Map<String, HighlightField> highlights = hit.getHighlightFields();
                        tag.setMatchType(SPELLCHECK);
                        tag.setTag(tag.getTag().replaceAll("<em>", "").replaceAll("</em>", ""));
                        tag.setOriginalToken(token);
                        matchedTokens.add(token.toLowerCase());
                        recognizedTags.add(tag);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve recognized tags", e);
        }
        for (String token : tokens) {
            if (!matchedTokens.contains(token.toLowerCase())) {
                TagDto tag = new TagDto();
                tag.setTag(token.toLowerCase());
                tag.setField("unrecognised");
                tag.setType("concept");
                tag.setSourceId(token.toLowerCase() + "_unrecognised");
                tag.setWeight(1.0f);
                tag.setMatchType(UNRECOGNISED);
                tag.setOriginalToken(token);
                recognizedTags.add(tag);
            }
        }

        Set<String> seen = new HashSet<>();
        return recognizedTags.stream()
                .filter(t -> seen.add(t.getTag().toLowerCase() + "_" + t.getType()))
                .collect(Collectors.toList());
    }

    private void removeMatchedKeywordsFromQuery(Set<String> matchedTokens, TagDto tag, Map<String, HighlightField> highlights) {
        String highlighted = Arrays.stream(highlights.get("tag").getFragments())
                .map(Object::toString)
                .collect(Collectors.joining(" "));
        tag.setTag(highlighted);
        String cleaned = highlighted.replaceAll("<em>", "").replaceAll("</em>", "");
        for (String word : cleaned.split("\\s+")) {
            matchedTokens.add(word.toLowerCase());
        }
    }

    private List<SemanticStage> loadSemanticConfig() {
        try (InputStream is = ProductService.class.getResourceAsStream("/semantic-config.json")) {
            Map<String, List<SemanticStage>> config = new ObjectMapper().readValue(is, new TypeReference<>() {});
            return config.get("stages");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load semantic configuration", e);
        }
    }


}
