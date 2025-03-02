package com.sradutataru.search.catalog.service.controller;

import com.sradutataru.search.catalog.service.ProductService;
import com.sradutataru.search.catalog.service.dto.ProductDto;
import com.sradutataru.search.catalog.service.dto.ProductResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController implements ProductControllerInterface {

    private final ProductService productService;

    @Override
    @GetMapping("/semantic-search")
    public ResponseEntity<ProductResponse> semanticSearch(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "10") Integer count,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam Map<String, String> allParams
    ) {
        ProductResponse products = productService.semanticSearch(q, count, page, extractAttributes(allParams));
        return ResponseEntity.ok(products);
    }

    @Override
    @GetMapping("/typeahead")
    public ResponseEntity<List<String>> typeaheadSearch(
            @RequestParam String q
    ) {
        List<String> suggestions = productService.typeaheadSearch(q);
        return ResponseEntity.ok(suggestions);
    }

    private static Map<String, String> extractAttributes(Map<String, String> allParams) {
        return allParams.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("attributes."))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
