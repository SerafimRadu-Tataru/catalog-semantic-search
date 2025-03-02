package com.sradutataru.search.catalog.indexer.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ProductDto {

    private String productId;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Brand cannot be blank")
    private String brand;

    @NotBlank(message = "Category cannot be blank")
    private String category;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Float price;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @Size(max = 10, message = "Maximum of 10 search keywords allowed")
    private List<String> searchKeywords;

    private Map<String, Object> attributes;

    @NotNull(message = "Release date cannot be null")
    private Date releaseDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "Rating cannot be negative")
    @DecimalMax(value = "5.0", inclusive = true, message = "Rating cannot exceed 5.0")
    private Float rating;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private List<String> tags;
}
