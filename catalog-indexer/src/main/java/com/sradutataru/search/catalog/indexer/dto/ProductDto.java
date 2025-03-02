package com.sradutataru.search.catalog.indexer.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Product details including ID, name, brand, category, and other attributes.")
public class ProductDto {

    @Schema(description = "Unique identifier for the product", example = "12345")
    private String productId;

    @NotBlank(message = "Name cannot be blank")
    @Schema(description = "Name of the product", example = "Apple iPhone 14 Pro", required = true)
    private String name;

    @NotBlank(message = "Brand cannot be blank")
    @Schema(description = "Brand name of the product", example = "Apple", required = true)
    private String brandName;

    @NotBlank(message = "Category cannot be blank")
    @Schema(description = "Category name of the product", example = "Smartphones", required = true)
    private String categoryName;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Schema(description = "Price of the product", example = "999.99", required = true)
    private Float price;

    @NotBlank(message = "Description cannot be blank")
    @Schema(description = "Description of the product", example = "The latest iPhone model with A16 Bionic chip.", required = true)
    private String description;

    @Size(max = 10, message = "Maximum of 10 search keywords allowed")
    @ArraySchema(arraySchema = @Schema(description = "Keywords for search optimization"),
            schema = @Schema(example = "Apple Iphone 14 Pro Apple Electronics Smartphones"))
    private String searchKeywords;

    @Schema(description = "Additional attributes of the product", example = "{\"color\": \"black\", \"storage\": \"128GB\"}")
    private Map<String, Object> attributes;

    @NotNull(message = "Release date cannot be null")
    @Schema(description = "Release date of the product", example = "2023-09-12", type = "string", required = true)
    private String releaseDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "Rating cannot be negative")
    @DecimalMax(value = "5.0", inclusive = true, message = "Rating cannot exceed 5.0")
    @Schema(description = "Average user rating", example = "4.5")
    private Float rating;

    @Min(value = 0, message = "Stock cannot be negative")
    @Schema(description = "Available stock count", example = "150")
    private Integer stock;

    @ArraySchema(arraySchema = @Schema(description = "Tags for the product"),
            schema = @Schema(example = "['new', 'discount', 'popular']"))
    private List<String> tags;
}
