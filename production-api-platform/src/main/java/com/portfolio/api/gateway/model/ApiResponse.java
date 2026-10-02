package com.portfolio.api.gateway.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an API response from the gateway.
 */
public record ApiResponse(
    boolean success,
    Object data,
    String errorMessage,
    String errorCode,
    Map<String, Object> metadata,
    Instant timestamp
) {
    
    public static ApiResponse success(Object data) {
        return new ApiResponse(true, data, null, null, Map.of(), Instant.now());
    }
    
    public static ApiResponse success(Object data, Map<String, Object> metadata) {
        return new ApiResponse(true, data, null, null, metadata, Instant.now());
    }
    
    public static ApiResponse error(String message, String code) {
        return new ApiResponse(false, null, message, code, Map.of(), Instant.now());
    }
    
    public static ApiResponse error(String message, String code, Map<String, Object> metadata) {
        return new ApiResponse(false, null, message, code, metadata, Instant.now());
    }
    
    public ApiResponse withMetadata(Map<String, Object> newMetadata) {
        return new ApiResponse(success, data, errorMessage, errorCode, newMetadata, timestamp);
    }
    
    public ApiResponse withData(Object newData) {
        return new ApiResponse(success, newData, errorMessage, errorCode, metadata, timestamp);
    }
}
