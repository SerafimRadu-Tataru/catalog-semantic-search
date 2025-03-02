package com.sradutataru.search.catalog.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Product details including ID, name, brand, category, and other attributes.")
public class ProductDto {

    @JsonProperty("product_id")
    @Schema(description = "Unique identifier for the product", example = "12345")
    private String productId;

    @JsonProperty("name")
    @Schema(description = "Name of the product", example = "Apple iPhone 14 Pro")
    private String name;

    @JsonProperty("brand_name")
    @Schema(description = "Brand name of the product", example = "Apple")
    private String brandName;

    @JsonProperty("category_name")
    @Schema(description = "Category name of the product", example = "Smartphones")
    private String categoryName;

    @JsonProperty("price")
    @Schema(description = "Price of the product", example = "999.99")
    private double price;

    @JsonProperty("description")
    @Schema(description = "Description of the product", example = "The latest iPhone model with A16 Bionic chip.")
    private String description;

    @JsonProperty("search_keywords")
    @ArraySchema(arraySchema = @Schema(description = "Keywords for search optimization", example = "Apple Iphone 14 Pro Apple Electronics Smartphones"))
    private String searchKeywords;

    @JsonProperty("attributes")
    @Schema(description = "Additional attributes of the product", example = "{\"color\": \"black\", \"storage\": \"128GB\"}")
    private Map<String, String> attributes;

    @JsonProperty("release_date")
    @Schema(description = "Release date of the product", example = "2023-09-12")
    private String releaseDate;

    @JsonProperty("rating")
    @Schema(description = "Average user rating", example = "4.5")
    private float rating;

    @JsonProperty("stock")
    @Schema(description = "Available stock count", example = "150")
    private int stock;

    @ArraySchema(arraySchema = @Schema(description = "Tags for the product", example = "[\"new\", \"discount\", \"popular\"]"))
    private List<String> tags;
}
