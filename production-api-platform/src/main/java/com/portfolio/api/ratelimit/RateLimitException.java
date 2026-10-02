package com.portfolio.api.ratelimit;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class RateLimitException extends RuntimeException {
    
    private final long retryAfterSeconds;
    private final String tier;
    
    public RateLimitException(String message, long retryAfterSeconds, String tier) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.tier = tier;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    public String getTier() {
        return tier;
    }
}
