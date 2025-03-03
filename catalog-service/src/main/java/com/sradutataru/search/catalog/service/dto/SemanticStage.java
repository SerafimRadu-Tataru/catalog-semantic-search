package com.sradutataru.search.catalog.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class SemanticStage {

    private String name;

    private Map<String, Float> fields;

    @JsonProperty("dynamic_fields")
    private Map<String, Float> dynamicFields;

    private float minMatchPercent;
}
