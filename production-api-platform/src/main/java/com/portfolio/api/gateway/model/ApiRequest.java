package com.portfolio.api.gateway.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an API request passing through the gateway.
 */
public record ApiRequest(
    String service,
    String path,
    String method,
    Map<String, String> headers,
    Map<String, String> queryParams,
    Map<String, Object> body,
    String clientIp,
    Instant timestamp,
    String traceId
) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String service;
        private String path;
        private String method;
        private Map<String, String> headers;
        private Map<String, String> queryParams;
        private Map<String, Object> body;
        private String clientIp;
        private Instant timestamp;
        private String traceId;
        
        public Builder service(String service) {
            this.service = service;
            return this;
        }
        
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        
        public Builder method(String method) {
            this.method = method;
            return this;
        }
        
        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }
        
        public Builder queryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }
        
        public Builder body(Map<String, Object> body) {
            this.body = body;
            return this;
        }
        
        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }
        
        public ApiRequest build() {
            return new ApiRequest(
                service, path, method, headers, queryParams, 
                body, clientIp, timestamp, traceId
            );
        }
    }
}
