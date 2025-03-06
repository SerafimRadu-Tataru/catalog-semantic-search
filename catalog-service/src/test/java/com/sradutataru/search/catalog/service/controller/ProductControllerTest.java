package com.sradutataru.search.catalog.service.controller;

import com.sradutataru.search.catalog.service.dto.ProductResponse;
import com.sradutataru.search.catalog.service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @Test
    void testKeywordSearch_ValidInput() {
        ProductResponse mockResponse = new ProductResponse();
        when(productService.keywordSearch(anyString(), anyInt(), anyInt(), anyMap()))
                .thenReturn(mockResponse);

        Map<String, String> params = new HashMap<>();
        params.put("attributes.color", "black");

        ResponseEntity<ProductResponse> response = productController.keywordSearch("wireless", 10, 1, params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testKeywordSearch_NoAttributes() {
        ProductResponse mockResponse = new ProductResponse();
        when(productService.keywordSearch(anyString(), anyInt(), anyInt(), anyMap()))
                .thenReturn(mockResponse);

        ResponseEntity<ProductResponse> response = productController.keywordSearch("wireless", 10, 1, new HashMap<>());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testSemanticSearch_ValidInput() {
        ProductResponse mockResponse = new ProductResponse();
        when(productService.semanticSearchV2(anyString(), anyInt(), anyInt(), anyMap()))
                .thenReturn(mockResponse);

        Map<String, String> params = new HashMap<>();
        params.put("attributes.color", "black");

        ResponseEntity<ProductResponse> response = productController.semanticSearch("wireless", 10, 1, params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testTypeaheadSearch_ValidInput() {
        List<String> mockSuggestions = Arrays.asList("iPhone 13", "iPhone 13 Pro", "iPhone 14");
        when(productService.typeaheadSearch(anyString()))
                .thenReturn(mockSuggestions);

        ResponseEntity<List<String>> response = productController.typeaheadSearch("iph");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSuggestions, response.getBody());
    }

    @Test
    void testExtractAttributes() throws Exception {
        Map<String, String> allParams = new HashMap<>();
        allParams.put("attributes.color", "black");
        allParams.put("attributes.size", "large");
        allParams.put("nonAttribute", "value");

        var method = ProductController.class.getDeclaredMethod("extractAttributes", Map.class);
        method.setAccessible(true);
        Map<String, String> result = (Map<String, String>) method.invoke(null, allParams);

        assertEquals(2, result.size());
        assertEquals("black", result.get("attributes.color"));
        assertEquals("large", result.get("attributes.size"));
    }
}
