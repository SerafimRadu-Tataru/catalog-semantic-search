package com.sradutataru.search.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sradutataru.search.catalog.service.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
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

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String INDEX = "catalog-index_live";

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    public List<ProductDto> semanticSearch(String query, Map<String, String> attributes) {
        List<ProductDto> products = new ArrayList<>();
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.moreLikeThisQuery(
                                    new String[]{"name", "description", "searchKeywords"},
                                    new String[]{query},
                                    null)
                            .minTermFreq(1)
                            .minDocFreq(1)
                    );

            if (attributes != null && !attributes.isEmpty()) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    String attributePath = "attributes." + entry.getKey();
                    boolQuery.filter(QueryBuilders.termQuery(attributePath, entry.getValue()));
                }
            }

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(boolQuery)
                    .size(20);

            SearchRequest searchRequest = new SearchRequest(INDEX);
            searchRequest.source(sourceBuilder);

            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            // Parse search results into ProductDto list
            for (SearchHit hit : response.getHits().getHits()) {
                ProductDto product = objectMapper.readValue(hit.getSourceAsString(), ProductDto.class);
                products.add(product);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to perform semantic search", e);
        }
        return products;
    }

    public List<String> typeaheadSearch(String query) {
        Set<String> suggestions = new HashSet<>();
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.prefixQuery("name", query))
                    .should(QueryBuilders.prefixQuery("description", query))
                    .should(QueryBuilders.prefixQuery("searchKeywords", query));

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(boolQuery)
                    .size(10);

            SearchRequest searchRequest = new SearchRequest(INDEX);
            searchRequest.source(sourceBuilder);

            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

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
}
