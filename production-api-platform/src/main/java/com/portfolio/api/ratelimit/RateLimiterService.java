package com.portfolio.api.ratelimit;

import com.portfolio.api.domain.model.RateLimitTier;
import com.portfolio.api.service.UserService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for rate limiting API requests using token bucket algorithm.
 * Integrates with user tiers for differentiated rate limits.
 */
@Service
public class RateLimiterService {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    
    private final RateLimitConfig rateLimitConfig;
    private final UserService userService;
    private final Counter acceptedRequests;
    private final Counter rejectedRequests;
    
    public RateLimiterService(RateLimitConfig rateLimitConfig, 
                               UserService userService,
                               MeterRegistry meterRegistry) {
        this.rateLimitConfig = rateLimitConfig;
        this.userService = userService;
        this.acceptedRequests = meterRegistry.counter("api.ratelimit.accepted");
        this.rejectedRequests = meterRegistry.counter("api.ratelimit.rejected");
    }
    
    /**
     * Checks if request is allowed based on user's rate limit tier.
     */
    public RateLimitResult checkRateLimit(String username) {
        RateLimitTier tier = userService.getRateLimitTier(username);
        return checkRateLimitForTier(username, tier);
    }
    
    /**
     * Checks if request is allowed for a specific tier.
     */
    public RateLimitResult checkRateLimitForTier(String identifier, RateLimitTier tier) {
        if (!tier.hasLimits()) {
            acceptedRequests.increment();
            return new RateLimitResult(true, -1, -1, tier);
        }
        
        String bucketKey = identifier + ":" + tier.name();
        Bucket bucket = rateLimitConfig.resolveBucket(bucketKey, tier.getRequestsPerMinute());
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            acceptedRequests.increment();
            log.debug("Rate limit check passed for {}: {} tokens remaining", 
                     identifier, probe.getRemainingTokens());
            return new RateLimitResult(
                true, 
                probe.getRemainingTokens(), 
                0,
                tier
            );
        } else {
            rejectedRequests.increment();
            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded for {}: retry after {} seconds", 
                    identifier, waitTimeSeconds);
            return new RateLimitResult(
                false, 
                0, 
                waitTimeSeconds,
                tier
            );
        }
    }
    
    /**
     * Checks rate limit using client IP (for anonymous requests).
     */
    public RateLimitResult checkRateLimitByIp(String clientIp) {
        return checkRateLimitForTier("ip:" + clientIp, RateLimitTier.FREE);
    }
    
    /**
     * Gets available tokens without consuming.
     */
    public long getAvailableTokens(String identifier, RateLimitTier tier) {
        if (!tier.hasLimits()) {
            return Long.MAX_VALUE;
        }
        
        String bucketKey = identifier + ":" + tier.name();
        Bucket bucket = rateLimitConfig.resolveBucket(bucketKey, tier.getRequestsPerMinute());
        return bucket.getAvailableTokens();
    }
    
    /**
     * Result of a rate limit check.
     */
    public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds,
        RateLimitTier tier
    ) {
        public boolean isLimited() {
            return !allowed;
        }
    }
}
