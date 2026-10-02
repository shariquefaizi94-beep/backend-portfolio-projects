package com.portfolio.api.gateway.routing;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines a route configuration for the API gateway.
 */
public class RouteDefinition {
    
    private final String id;
    private final String service;
    private final String pathPattern;
    private final Set<String> methods;
    private final String backendUrl;
    private final boolean enabled;
    private final int priority;
    private final Map<String, String> headerTransformations;
    private final List<String> requiredHeaders;
    private final boolean authRequired;
    private final int timeoutMs;
    private final int retryCount;
    
    public RouteDefinition(String id, String service, String pathPattern, 
                           Set<String> methods, String backendUrl, boolean enabled,
                           int priority, Map<String, String> headerTransformations,
                           List<String> requiredHeaders, boolean authRequired,
                           int timeoutMs, int retryCount) {
        this.id = id;
        this.service = service;
        this.pathPattern = pathPattern;
        this.methods = methods;
        this.backendUrl = backendUrl;
        this.enabled = enabled;
        this.priority = priority;
        this.headerTransformations = headerTransformations;
        this.requiredHeaders = requiredHeaders;
        this.authRequired = authRequired;
        this.timeoutMs = timeoutMs;
        this.retryCount = retryCount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean matches(String service, String path, String method) {
        if (!this.service.equals(service)) {
            return false;
        }
        if (!methods.contains("*") && !methods.contains(method)) {
            return false;
        }
        return matchesPath(path);
    }
    
    private boolean matchesPath(String path) {
        if (pathPattern.equals("/**") || pathPattern.equals("/*")) {
            return true;
        }
        
        // Simple pattern matching (supports ** wildcard at end)
        if (pathPattern.endsWith("/**")) {
            String prefix = pathPattern.substring(0, pathPattern.length() - 3);
            return path.startsWith(prefix);
        }
        
        return pathPattern.equals(path);
    }
    
    // Getters
    public String getId() { return id; }
    public String getService() { return service; }
    public String getPathPattern() { return pathPattern; }
    public Set<String> getMethods() { return methods; }
    public String getBackendUrl() { return backendUrl; }
    public boolean isEnabled() { return enabled; }
    public int getPriority() { return priority; }
    public Map<String, String> getHeaderTransformations() { return headerTransformations; }
    public List<String> getRequiredHeaders() { return requiredHeaders; }
    public boolean isAuthRequired() { return authRequired; }
    public int getTimeoutMs() { return timeoutMs; }
    public int getRetryCount() { return retryCount; }
    
    public static class Builder {
        private String id;
        private String service;
        private String pathPattern = "/**";
        private Set<String> methods = Set.of("*");
        private String backendUrl;
        private boolean enabled = true;
        private int priority = 0;
        private Map<String, String> headerTransformations = Map.of();
        private List<String> requiredHeaders = List.of();
        private boolean authRequired = false;
        private int timeoutMs = 30000;
        private int retryCount = 3;
        
        public Builder id(String id) { this.id = id; return this; }
        public Builder service(String service) { this.service = service; return this; }
        public Builder pathPattern(String pathPattern) { this.pathPattern = pathPattern; return this; }
        public Builder methods(Set<String> methods) { this.methods = methods; return this; }
        public Builder backendUrl(String backendUrl) { this.backendUrl = backendUrl; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder headerTransformations(Map<String, String> transformations) { 
            this.headerTransformations = transformations; return this; 
        }
        public Builder requiredHeaders(List<String> headers) { this.requiredHeaders = headers; return this; }
        public Builder authRequired(boolean authRequired) { this.authRequired = authRequired; return this; }
        public Builder timeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; return this; }
        public Builder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
        
        public RouteDefinition build() {
            return new RouteDefinition(id, service, pathPattern, methods, backendUrl,
                enabled, priority, headerTransformations, requiredHeaders, 
                authRequired, timeoutMs, retryCount);
        }
    }
}
