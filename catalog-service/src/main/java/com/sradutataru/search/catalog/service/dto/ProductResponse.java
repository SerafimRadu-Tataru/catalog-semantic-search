package com.sradutataru.search.catalog.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response for product search containing query, products, count, and page information.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    @Schema(description = "Search query string", example = "smartphone")
    private String q;

    @Schema(description = "The total number of docs found", example = "13")
    private long numFound;

    @Schema(description = "Search filters based on product attributes", example = "{ \"connectivity\": \"wireless\" }")
    private Map<String, String> attributes;

    @ArraySchema(arraySchema = @Schema(description = "List of products matching the search query"))
    private List<ProductDto> products;

    @Schema(description = "Total number of products in the response", example = "20")
    private Integer count;

    @Schema(description = "Current page number", example = "1")
    private Integer page;

    @Schema(description = "The stage that returned results", example = "exact")
    private String stage;

}
