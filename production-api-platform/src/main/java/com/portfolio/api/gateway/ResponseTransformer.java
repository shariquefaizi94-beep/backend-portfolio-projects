package com.portfolio.api.gateway;

import com.portfolio.api.gateway.model.ApiResponse;
import com.portfolio.api.gateway.routing.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Transforms outgoing API responses according to route configuration.
 * Handles response filtering, enrichment, and standardization.
 */
@Component
public class ResponseTransformer {
    
    private static final Logger log = LoggerFactory.getLogger(ResponseTransformer.class);
    
    /**
     * Transforms an API response according to route rules.
     */
    public ApiResponse transform(ApiResponse response, RouteDefinition route) {
        log.debug("Transforming response for route: {}", route.getId());
        
        // Build enriched metadata
        Map<String, Object> metadata = new HashMap<>(response.metadata());
        metadata.put("routeId", route.getId());
        metadata.put("backend", route.getBackendUrl());
        metadata.put("processedAt", Instant.now().toString());
        
        return response.withMetadata(metadata);
    }
    
    /**
     * Transforms error response with standardized format.
     */
    public ApiResponse transformError(Exception e, RouteDefinition route) {
        log.error("Transforming error response for route: {}", route.getId(), e);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("routeId", route.getId());
        metadata.put("processedAt", Instant.now().toString());
        
        return ApiResponse.error(
            sanitizeErrorMessage(e.getMessage()),
            "BACKEND_ERROR",
            metadata
        );
    }
    
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "An unexpected error occurred";
        }
        // Remove sensitive information from error messages
        return message
            .replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[IP_HIDDEN]")
            .replaceAll("password.*?[\"'].*?[\"']", "password=[HIDDEN]");
    }
}
