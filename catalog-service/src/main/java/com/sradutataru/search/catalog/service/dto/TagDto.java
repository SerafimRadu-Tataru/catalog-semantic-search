package com.sradutataru.search.catalog.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Data Transfer Object representing a semantic tag")
public class TagDto {

    private String tag;

    private String field;

    private String type;

    @JsonProperty("source_id")
    private String sourceId;

    private float weight;

    @JsonProperty("original_token")
    private String originalToken;

    private MatchType matchType;

    public enum MatchType {
        EXACT, SPELLCHECK, UNRECOGNISED
    }
}
