package com.sradutataru.search.catalog.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sradutataru.search.catalog.service.dto.ProductDto;
import com.sradutataru.search.catalog.service.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.http.util.TextUtils.isBlank;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final String INDEX = "catalog-index_live";
    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;
    private final SemanticService semanticService;

    public ProductResponse keywordSearch(String query, Integer count, Integer page, Map<String, String> attributes) {
        List<ProductDto> products = new ArrayList<>();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if(query == null || isBlank(query)) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.multiMatchQuery(query, "name", "description", "searchKeywords"));
        }
        if (attributes != null && !attributes.isEmpty()) {
            attributes.forEach((attrKey, attrValue) ->
                    boolQuery.filter(QueryBuilders.termQuery(attrKey, attrValue))
            );
        }

        return getProductResponse(query, count, page, attributes, products, boolQuery);
    }

    public ProductResponse semanticSearchV1(String query, Integer count, Integer page, Map<String, String> attributes) {
        List<ProductDto> products = new ArrayList<>();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.moreLikeThisQuery(
                                new String[]{"name", "description", "searchKeywords"},
                                new String[]{query},
                                null)
                        .minTermFreq(1)
                        .minDocFreq(1)
                );
        if (attributes != null && !attributes.isEmpty()) {
            attributes.forEach((key, value) -> boolQuery.filter(QueryBuilders.termQuery(key, value)));
        }

        return getProductResponse(query, count, page, attributes, products, boolQuery);
    }

    public ProductResponse semanticSearchV2(String query, Integer count, Integer page, Map<String, String> attributes) {
        ProductResponse semanticSearch = semanticService.semanticSearch(query, count, page, attributes);
        if(semanticSearch == null) {
            return keywordSearch(query, count, page, attributes);
        }
        return semanticSearch;
    }

    private ProductResponse getProductResponse(String query, Integer count, Integer page, Map<String, String> attributes,
                                               List<ProductDto> products, BoolQueryBuilder boolQuery) {
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(boolQuery)
                    .size(count)
                    .from((page - 1) * count);

            SearchRequest searchRequest = new SearchRequest(INDEX);
            searchRequest.source(sourceBuilder);

            SearchResponse response = client.search(searchRequest, DEFAULT);

            long numFound = response.getHits().getTotalHits().value;
            for (SearchHit hit : response.getHits().getHits()) {
                ProductDto product = objectMapper.readValue(hit.getSourceAsString(), ProductDto.class);
                products.add(product);
            }
            return new ProductResponse(query, numFound, attributes, products, count, page, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to perform semantic search", e);
        }
    }

    public List<String> typeaheadSearch(String query) {
        Set<String> suggestions = new HashSet<>();
        try {
            String[] tokens = query.toLowerCase().split(" ");
            BoolQueryBuilder boolQuery =
                    QueryBuilders.boolQuery()
                            .should(getPrefixPhraseForField(tokens, "name"))
                            .should(getPrefixPhraseForField(tokens, "searchKeywords"));
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(boolQuery)
                    .size(10);

            SearchRequest searchRequest = new SearchRequest(INDEX);
            searchRequest.source(sourceBuilder);

            SearchResponse response = client.search(searchRequest, DEFAULT);

            for (SearchHit hit : response.getHits().getHits()) {
                JsonNode node = objectMapper.readTree(hit.getSourceAsString());
                if (node.has("name")) {
                    suggestions.add(node.get("name").asText());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to perform typeahead search", e);
        }
        return new ArrayList<>(suggestions);
    }

    static BoolQueryBuilder getPrefixPhraseForField(String[] tokens, String field) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (tokens.length == 1) {
            boolQuery.should(QueryBuilders.prefixQuery(field, tokens[0]));
        } else {
            for (int i = 0; i < tokens.length - 1; i++) {
                boolQuery.must(QueryBuilders.matchQuery(field, tokens[i]));
            }
            boolQuery.must(QueryBuilders.prefixQuery(field, tokens[tokens.length - 1]));
        }
        return boolQuery;
    }




}
