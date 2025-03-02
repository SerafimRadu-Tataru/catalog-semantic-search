package com.sradutataru.search.catalog.indexer.controller;

import com.sradutataru.search.catalog.indexer.dto.ProductDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Api(value = "Indexing API", tags = {"Indexing"})
public interface SparkIndexControllerInterface {
    @ApiOperation(value = "Bulk index products", notes = "Cleans the target index and initiates bulk indexing of product data.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Index cleaned and data indexing initiated", examples = @Example(value = {
                    @ExampleProperty(mediaType = "application/json", value = "Index cleaned and data indexing initiated.")
            }))
    })
    ResponseEntity<String> indexData();

    @ApiOperation(value = "Index single document", notes = "Indexes a single product document. If no productId is provided, one is generated automatically. Returns the indexed product.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Document indexed successfully", response = ProductDto.class),
            @ApiResponse(code = 400, message = "Invalid product data")
    })
    ResponseEntity<ProductDto> singleDoc(@ApiParam(value = "Product data to index", required = true) @RequestBody ProductDto productDto);

    @ApiOperation(value = "Swap indices aliases", notes = "Swaps the live alias with the preview index alias to promote new data to production.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Swapped live alias to preview index successfully", examples = @Example(value = {
                    @ExampleProperty(mediaType = "application/json", value = "Swapped live alias to preview index.")
            }))
    })
    ResponseEntity<String> swap();
}
