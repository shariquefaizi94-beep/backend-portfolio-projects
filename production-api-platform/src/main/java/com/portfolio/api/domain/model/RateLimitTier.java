package com.portfolio.api.domain.model;

/**
 * Rate limit tier enumeration defining API usage quotas.
 * Each tier specifies requests per minute and daily limits.
 */
public enum RateLimitTier {
    FREE(60, 1000),           // 60 req/min, 1000 req/day
    BASIC(120, 5000),         // 120 req/min, 5000 req/day
    PREMIUM(300, 20000),      // 300 req/min, 20000 req/day
    ENTERPRISE(1000, 100000), // 1000 req/min, 100000 req/day
    UNLIMITED(-1, -1);        // No limits (internal services)
    
    private final int requestsPerMinute;
    private final int requestsPerDay;
    
    RateLimitTier(int requestsPerMinute, int requestsPerDay) {
        this.requestsPerMinute = requestsPerMinute;
        this.requestsPerDay = requestsPerDay;
    }
    
    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }
    
    public int getRequestsPerDay() {
        return requestsPerDay;
    }
    
    public boolean hasLimits() {
        return requestsPerMinute > 0 && requestsPerDay > 0;
    }
}
