package com.sradutataru.search.catalog.service.controller;

import com.sradutataru.search.catalog.service.ProductService;
import com.sradutataru.search.catalog.service.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    @GetMapping("/semantic-search")
    public ResponseEntity<List<ProductDto>> semanticSearch(
            @RequestParam String q,
            @RequestParam(required = false) Map<String, String> attributes
    ) {
        List<ProductDto> products = productService.semanticSearch(q, attributes);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/typeahead")
    public ResponseEntity<List<String>> typeaheadSearch(@RequestParam String q) {
        List<String> suggestions = productService.typeaheadSearch(q);
        return ResponseEntity.ok(suggestions);
    }
}
