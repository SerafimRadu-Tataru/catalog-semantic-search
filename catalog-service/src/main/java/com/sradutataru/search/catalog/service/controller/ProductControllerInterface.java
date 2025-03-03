package com.sradutataru.search.catalog.service.controller;

import com.sradutataru.search.catalog.service.dto.ProductResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Api(value = "Product API", tags = {"Products"})
public interface ProductControllerInterface {

    @ApiOperation(value = "Keyword search",
            notes = "Performs a simple keyword search across product name, description, and searchKeywords fields.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved products", response = ProductResponse.class),
            @ApiResponse(code = 400, message = "Invalid input provided"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    ResponseEntity<ProductResponse> keywordSearch(
            @ApiParam(value = "Search keyword", required = true, example = "wireless earbuds")
            @RequestParam String q,
            @ApiParam(value = "Number of results per page", required = true, example = "10")
            @RequestParam Integer count,
            @ApiParam(value = "Page number, starts at 1", required = true, example = "1")
            @RequestParam Integer page,
            @ApiParam(value = "Additional attribute filters. Keys must be prefixed with 'attributes.' (e.g., attributes.color=black)", required = false)
            @RequestParam Map<String, String> allParams);

    @ApiOperation(value = "Perform semantic search",
            notes = "This endpoint returns a list of products that semantically match the provided query. " +
                    "Use 'q' for the search term, 'count' for the number of results per page, 'page' for the page number, " +
                    "and additional parameters (prefixed with 'attributes.') to filter results by specific attributes.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved products", response = ProductResponse.class),
            @ApiResponse(code = 400, message = "Invalid input provided"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @GetMapping("/semantic-search")
    ResponseEntity<ProductResponse> semanticSearch(
            @ApiParam(value = "Search query string", required = true, example = "wireless earbuds")
            @RequestParam String q,
            @ApiParam(value = "Number of results per page", required = true, example = "10")
            @RequestParam Integer count,
            @ApiParam(value = "Page number, starts at 1", required = true, example = "1")
            @RequestParam Integer page,
            @ApiParam(value = "Additional attribute filters. Keys must be prefixed with 'attributes.' (e.g., attributes.color=black)", required = false)
            @RequestParam Map<String, String> allParams
    );

    @ApiOperation(value = "Typeahead search",
            notes = "This endpoint returns a list of product name suggestions matching the provided prefix. " +
                    "It searches only on the product name field from the live collection and does not accept attribute filters.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved suggestions",
                    examples = @Example(value = {
                            @ExampleProperty(mediaType = "application/json", value = "[\"iPhone 13\", \"iPhone 13 Pro\", \"iPhone 14\"]")
                    })),
            @ApiResponse(code = 400, message = "Invalid input provided"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @GetMapping("/typeahead")
    ResponseEntity<List<String>> typeaheadSearch(
            @ApiParam(value = "Search prefix string for typeahead suggestions", required = true, example = "iph")
            @RequestParam String q
    );
}
