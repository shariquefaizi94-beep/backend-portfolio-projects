package com.portfolio.api.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for token bucket rate limiting.
 * Creates and manages buckets based on rate limit tiers.
 */
@Configuration
public class RateLimitConfig {
    
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    
    /**
     * Gets or creates a rate limit bucket for the given key and tier.
     */
    public Bucket resolveBucket(String key, int requestsPerMinute) {
        return bucketCache.computeIfAbsent(key, k -> createBucket(requestsPerMinute));
    }
    
    /**
     * Creates a token bucket with specified capacity.
     * Uses gradual refill strategy.
     */
    private Bucket createBucket(int requestsPerMinute) {
        if (requestsPerMinute <= 0) {
            // Unlimited tier - very high capacity
            return Bucket.builder()
                .addLimit(Bandwidth.classic(1_000_000, Refill.greedy(1_000_000, Duration.ofMinutes(1))))
                .build();
        }
        
        // Standard tier with specified capacity
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Creates a bucket with multiple rate limits (burst + sustained).
     */
    public Bucket createMultiLimitBucket(int burstCapacity, int sustainedPerMinute) {
        // Short burst limit - allows quick succession of requests
        Bandwidth burstLimit = Bandwidth.classic(
            burstCapacity,
            Refill.intervally(burstCapacity, Duration.ofSeconds(10))
        );
        
        // Sustained limit - longer term rate control
        Bandwidth sustainedLimit = Bandwidth.classic(
            sustainedPerMinute,
            Refill.greedy(sustainedPerMinute, Duration.ofMinutes(1))
        );
        
        return Bucket.builder()
            .addLimit(burstLimit)
            .addLimit(sustainedLimit)
            .build();
    }
    
    /**
     * Removes a bucket from cache (for testing or tier changes).
     */
    public void evictBucket(String key) {
        bucketCache.remove(key);
    }
    
    /**
     * Clears all cached buckets.
     */
    public void clearCache() {
        bucketCache.clear();
    }
    
    /**
     * Returns current cache size.
     */
    public int cacheSize() {
        return bucketCache.size();
    }
}
