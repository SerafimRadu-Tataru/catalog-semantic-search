package com.sradutataru.search.catalog.indexer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.spark.sql.Encoders.javaSerialization;

@Component
@RequiredArgsConstructor
@Slf4j
public class TagService {

    private static final String TAGS_INDEX = "semantic-tags";

    private final RestHighLevelClient restHighLevelClient;

    private final Map<String, Map<String, Boolean>> tagConfig = loadTagConfig();

    public void indexTags(Dataset<Row> dataset) {
        List<Map<String, Object>> allTags = new ArrayList<>();
        for (Map.Entry<String, Map<String, Boolean>> entry : tagConfig.entrySet()) {
            String field = entry.getKey();
            Dataset<Row> fieldDs = dataset.select(field).distinct();
            List<Row> rows = fieldDs.collectAsList();

            if (TRUE.equals(entry.getValue().get("asMap"))) {
                for (Row row : rows) {
                    Object fieldValue = row.getAs(field);
                    if (fieldValue instanceof Row) {
                        Row mapRow = (Row) fieldValue;
                        Map<String, String> attributesMap = new HashMap<>();
                        for (String fieldName : mapRow.schema().fieldNames()) {
                            Object value = mapRow.getAs(fieldName);
                            if (value != null) {
                                attributesMap.put(fieldName, value.toString());
                            }
                        }
                        allTags.addAll(updateFieldSemanticTags(field, attributesMap));
                    } else if (fieldValue instanceof Map) {
                        allTags.addAll(updateFieldSemanticTags(field, (Map<String, String>) fieldValue));
                    }
                }
            } else {
                for (Row row : rows) {
                    allTags.addAll(updateFieldSemanticTags(field, (String) row.getAs(field)));
                }
            }
        }
        bulkIndexTags(List.of(allTags
                .stream()
                .collect(toMap(tag -> tag.get("source_id"), Function.identity(), (a1, a2) -> a1))
                .values().toArray(Map[]::new)));
    }

    public List<Map<String, Object>> updateFieldSemanticTags(String fieldName, String fieldValue) {
        if (isNull(fieldValue) || isNull(tagConfig.get(fieldName))) {
            return List.of();
        }
        fieldName = toSnake(fieldName);
        return extractFieldSemanticTag(fieldName, fieldValue, fieldName);
    }

    public List<Map<String, Object>> updateFieldSemanticTags(String fieldName, Map<String, String> fieldValue) {
        if (isNull(fieldValue) || isNull(tagConfig.get(fieldName))) {
            return List.of();
        }
        String snakeField = toSnake(fieldName);
        if(TRUE.equals(tagConfig.get(fieldName).get("asMap"))) {
            return fieldValue.entrySet().stream()
                    .flatMap(entry -> extractFieldSemanticTag(snakeField + "." + toSnake(entry.getKey()), entry.getValue(), snakeField).stream())
                    .toList();
        }
        return List.of();
    }

    public void bulkIndexTags(List<Map<String, Object>> docs) {
        if (docs.isEmpty()) return;
        try {
            var bulkRequest = new org.elasticsearch.action.bulk.BulkRequest();
            for (Map<String, Object> doc : docs) {
                String tag = (String) doc.get("tag");
                String field = (String) doc.get("field");
                String tagType = (String) doc.get("type");
                String tagKey = tag.replace(" ", "_") + "_" + field + "_" + tagType;
                bulkRequest.add(new UpdateRequest(TAGS_INDEX, tagKey)
                        .doc(doc)
                        .upsert(doc));
            }
            BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            log.debug("Bulk update semantic tags response status: {}", bulkResponse.status());
        } catch (IOException e) {
            log.error("Error bulk updating semantic tags: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to bulk update semantic tags", e);
        }
    }

    private List<Map<String, Object>> extractFieldSemanticTag(String fieldName, String fieldValue, String configName) {
        List<Map<String, Object>> docs = new ArrayList<>();
        if (TRUE.equals(tagConfig.get(configName).get("concept"))) {
            docs.addAll(indexConceptTags(fieldValue, fieldName));
        }
        if (TRUE.equals(tagConfig.get(configName).get("text"))) {
            docs.addAll(indexTextTags(fieldValue, fieldName));
        }
        return docs;
    }

    private List<Map<String, Object>> indexTextTags(String fieldValue, String fieldName) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String[] textTokens = fieldValue.toLowerCase().split("\\s+");
        for (String token : textTokens) {
            docs.add(indexOrUpdateSemanticTag(token, fieldName, "text"));
        }
        return docs;
    }

    private List<Map<String, Object>> indexConceptTags(String fieldValue, String fieldName) {
        return List.of(indexOrUpdateSemanticTag(fieldValue, fieldName, "concept"));
    }

    private Map<String, Object> indexOrUpdateSemanticTag(String tag, String field, String tagType) {
            String tagKey = tag.toLowerCase() + "_" + field + "_" + tagType;
            Map<String, Object> doc = new HashMap<>();
            doc.put("tag", tag);
            doc.put("field", field);
            doc.put("type", tagType);
            doc.put("weight", "1");
            doc.put("source_id", tagKey);
            return doc;
    }

    private Map<String, Map<String, Boolean>> loadTagConfig() {
        try (InputStream is = TagService.class.getResourceAsStream("/tag-config.json")) {
            return new ObjectMapper().readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tag configuration", e);
        }
    }

    private static String toSnake(String fieldName) {
        return fieldName
                .replaceAll("([A-Z])(?=[A-Z])", "$1_")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
    }

}
