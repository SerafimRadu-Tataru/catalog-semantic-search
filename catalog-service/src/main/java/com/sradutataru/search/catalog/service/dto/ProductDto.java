package com.sradutataru.search.catalog.service.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductDto {
    private String productId;
    private String name;
    private String brand;
    private String category;
    private double price;
    private String description;
    private List<String> searchKeywords;
    private Map<String, Object> attributes;
    private String releaseDate;
    private float rating;
    private int stock;
    private List<String> tags;
}
