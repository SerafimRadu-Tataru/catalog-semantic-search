package com.sradutataru.search.catalog.indexer.controller;

import com.sradutataru.search.catalog.indexer.dto.ProductDto;
import com.sradutataru.search.catalog.indexer.service.SparkIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/index")
@RequiredArgsConstructor
public class SparkIndexController implements SparkIndexControllerInterface {

    private final SparkIndexService service;

    @Override
    @GetMapping
    public ResponseEntity<String> indexData() {
        service.indexAllDataAsync();
        return ResponseEntity.ok("Index cleaned and data indexing initiated.");
    }

    @Override
    @PostMapping("/doc")
    public ResponseEntity<ProductDto> singleDoc(@RequestBody ProductDto productDto) {
        ProductDto indexedProduct = service.indexSingleDocument(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(indexedProduct);
    }

    @Override
    @GetMapping("/swap")
    public ResponseEntity<String> swap() {
        service.swapCollections();
        return ResponseEntity.ok("Swapped live alias to preview index.");
    }
}
