package com.portfolio.api.gateway;

import com.portfolio.api.gateway.model.ApiRequest;
import com.portfolio.api.gateway.routing.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Transforms incoming API requests according to route configuration.
 * Handles header manipulation, path rewriting, and request enrichment.
 */
@Component
public class RequestTransformer {
    
    private static final Logger log = LoggerFactory.getLogger(RequestTransformer.class);
    
    /**
     * Transforms an API request according to route rules.
     */
    public ApiRequest transform(ApiRequest request, RouteDefinition route) {
        log.debug("Transforming request for route: {}", route.getId());
        
        // Apply header transformations
        Map<String, String> transformedHeaders = new HashMap<>(request.headers());
        
        // Add configured header transformations
        route.getHeaderTransformations().forEach((key, value) -> {
            if (value.startsWith("$")) {
                // Variable substitution
                String resolved = resolveVariable(value, request);
                if (resolved != null) {
                    transformedHeaders.put(key, resolved);
                }
            } else {
                transformedHeaders.put(key, value);
            }
        });
        
        // Add standard gateway headers
        transformedHeaders.put("X-Gateway-Request-Id", UUID.randomUUID().toString());
        transformedHeaders.put("X-Gateway-Timestamp", request.timestamp().toString());
        transformedHeaders.put("X-Forwarded-Service", request.service());
        
        if (request.clientIp() != null) {
            transformedHeaders.put("X-Forwarded-For", request.clientIp());
        }
        
        // Generate trace ID if not present
        String traceId = request.traceId();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        transformedHeaders.put("X-Trace-Id", traceId);
        
        // Transform path if needed
        String transformedPath = transformPath(request.path(), route);
        
        return ApiRequest.builder()
            .service(request.service())
            .path(transformedPath)
            .method(request.method())
            .headers(transformedHeaders)
            .queryParams(request.queryParams())
            .body(request.body())
            .clientIp(request.clientIp())
            .timestamp(request.timestamp())
            .traceId(traceId)
            .build();
    }
    
    private String resolveVariable(String variable, ApiRequest request) {
        return switch (variable) {
            case "$clientIp" -> request.clientIp();
            case "$timestamp" -> request.timestamp().toString();
            case "$service" -> request.service();
            case "$path" -> request.path();
            case "$method" -> request.method();
            default -> {
                if (variable.startsWith("$header.")) {
                    String headerName = variable.substring(8);
                    yield request.headers().get(headerName);
                }
                if (variable.startsWith("$query.")) {
                    String paramName = variable.substring(7);
                    yield request.queryParams().get(paramName);
                }
                yield null;
            }
        };
    }
    
    private String transformPath(String path, RouteDefinition route) {
        // Simple path transformation - in production, this would support
        // regex-based rewrites, prefix stripping, etc.
        return path;
    }
}
