package com.sradutataru.search.catalog.indexer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sradutataru.search.catalog.indexer.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentType.JSON;

@Service
@RequiredArgsConstructor
@Slf4j
public class SparkIndexService {

    private static final String CATALOG_INDEX = "catalog-index";
    private static final String PREVIEW = "_preview";
    public static final String PREVIEW_ALIAS = CATALOG_INDEX + PREVIEW;
    private static final String LIVE = "_live";
    public static final String LIVE_ALIAS = CATALOG_INDEX + LIVE;

    private final SparkSession sparkSession;
    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;

    @Async
    public void indexAllDataAsync() {
        try {
            cleanIndex();

            Dataset<Row> brandsDF = loadJsonDataset("data/brands.json");
            Dataset<Row> categoriesDF = loadJsonDataset("data/categories.json");
            Dataset<Row> productsDF = loadJsonDataset("data/products.json");
            Dataset<Row> joinedDF = productsDF
                    .join(brandsDF, productsDF.col("brand").equalTo(brandsDF.col("brand_id")), "left")
                    .join(categoriesDF, productsDF.col("category").equalTo(categoriesDF.col("category_id")), "left");
            joinedDF = joinedDF.drop("brand", "category", "brand_id", "category_id");
            joinedDF = joinedDF.withColumn("search_keywords", functions.concat_ws(" ", joinedDF.col("name"), joinedDF.col("brand_name"), joinedDF.col("category_name")));
            joinedDF.write().format("org.elasticsearch.spark.sql").option("es.resource", PREVIEW_ALIAS + "/_doc").mode("append").save();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private Dataset<Row> loadJsonDataset(String resourcePath) throws Exception {
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (resourceUrl == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
        return sparkSession.read().option("multiLine", "true").option("mode", "DROPMALFORMED").json(resourceUrl.getPath());
    }

    public ProductDto indexSingleDocument(@Valid ProductDto productDto) {
        try {
            if (productDto.getProductId() == null || productDto.getProductId().isEmpty()) {
                productDto.setProductId(UUID.randomUUID().toString());
            }
            IndexRequest indexRequest = new IndexRequest(PREVIEW_ALIAS)
                    .id(productDto.getProductId())
                    .source(objectMapper.writeValueAsString(productDto), JSON);
            restHighLevelClient.index(indexRequest, DEFAULT);
            return productDto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting ProductDto to JSON", e);
        } catch (IOException e) {
            throw new RuntimeException("Error indexing document in Elasticsearch", e);
        }
    }

    public void swapCollections() {
        GetAliasesRequest getReq = new GetAliasesRequest(PREVIEW_ALIAS, LIVE_ALIAS);
        GetAliasesResponse getRes;
        try {
            getRes = restHighLevelClient.indices().getAlias(getReq, DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String liveIndex = null;
        String previewIndex = null;
        for (Map.Entry<String, Set<AliasMetaData>> entry : getRes.getAliases().entrySet()) {
            if (!entry.getKey().startsWith(CATALOG_INDEX)) continue;
            for (AliasMetaData amd : entry.getValue()) {
                if (LIVE_ALIAS.equals(amd.alias())) liveIndex = entry.getKey();
                if (PREVIEW_ALIAS.equals(amd.alias())) previewIndex = entry.getKey();
            }
        }
        if (liveIndex == null || previewIndex == null) return;
        IndicesAliasesRequest req = new IndicesAliasesRequest();
        req.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE).alias(LIVE_ALIAS).index(liveIndex));
        req.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE).alias(PREVIEW_ALIAS).index(previewIndex));
        req.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD).alias(LIVE_ALIAS).index(previewIndex));
        req.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD).alias(PREVIEW_ALIAS).index(liveIndex));
        try {
            restHighLevelClient.indices().updateAliases(req, DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void cleanIndex() {
        try {
            DeleteByQueryRequest request = new DeleteByQueryRequest(PREVIEW_ALIAS);
            request.setQuery(QueryBuilders.matchAllQuery());
            restHighLevelClient.deleteByQuery(request, RequestOptions.DEFAULT);
            System.out.println("Cleaned index: " + PREVIEW_ALIAS);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to clean index", e);
        }
    }

}
