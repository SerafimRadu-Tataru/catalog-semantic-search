package com.sradutataru.search.catalog.service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogExceptionHandlerTest {

    @InjectMocks
    private CatalogExceptionHandler catalogExceptionHandler;

    @Test
    void testHandleIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("/api/v1/products");

        ResponseEntity<Object> response = catalogExceptionHandler.handleIllegalArgumentException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Invalid argument", responseBody.get("message"));
        assertEquals(400, responseBody.get("status"));
        assertEquals("Bad Request", responseBody.get("error"));
        assertEquals("/api/v1/products", responseBody.get("path"));
    }

    @Test
    void testHandleRuntimeException() {
        RuntimeException exception = new RuntimeException("Unexpected error");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("/api/v1/products");

        ResponseEntity<Object> response = catalogExceptionHandler.handleRuntimeException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Unexpected error occurred", responseBody.get("message"));
        assertEquals(500, responseBody.get("status"));
        assertEquals("Internal Server Error", responseBody.get("error"));
        assertEquals("/api/v1/products", responseBody.get("path"));
    }

    @Test
    void testHandleMethodArgumentNotValid() {
        var fieldError = mock(FieldError.class);
        when(fieldError.getField()).thenReturn("name");
        when(fieldError.getDefaultMessage()).thenReturn("Name is required");
        var bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        HttpHeaders headers = new HttpHeaders();
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> response = catalogExceptionHandler.handleMethodArgumentNotValid(ex, headers, HttpStatus.BAD_REQUEST, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("Name is required", responseBody.get("name"));
    }

    @Test
    void testBuildErrorResponseThroughHandler() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid request");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("/api/v1/test");

        ResponseEntity<Object> response = catalogExceptionHandler.handleIllegalArgumentException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

        assertEquals("Invalid request", responseBody.get("message"));
        assertEquals(400, responseBody.get("status"));
        assertEquals("Bad Request", responseBody.get("error"));
        assertEquals("/api/v1/test", responseBody.get("path"));
        assertEquals(LocalDateTime.class, responseBody.get("timestamp").getClass());
    }
}
