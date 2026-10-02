package com.portfolio.api.ratelimit;

import com.portfolio.api.domain.model.RateLimitTier;
import com.portfolio.api.service.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limiter Service Tests")
class RateLimiterServiceTest {

    @Mock
    private UserService userService;

    private RateLimitConfig rateLimitConfig;
    private MeterRegistry meterRegistry;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
        meterRegistry = new SimpleMeterRegistry();
        rateLimiterService = new RateLimiterService(rateLimitConfig, userService, meterRegistry);
    }

    @Test
    @DisplayName("Should allow request within rate limit")
    void shouldAllowRequestWithinRateLimit() {
        RateLimiterService.RateLimitResult result = 
            rateLimiterService.checkRateLimitForTier("test-user", RateLimitTier.BASIC);

        assertTrue(result.allowed());
        assertFalse(result.isLimited());
        assertTrue(result.remainingTokens() > 0);
        assertEquals(RateLimitTier.BASIC, result.tier());
    }

    @Test
    @DisplayName("Should allow unlimited tier without limits")
    void shouldAllowUnlimitedTierWithoutLimits() {
        RateLimiterService.RateLimitResult result = 
            rateLimiterService.checkRateLimitForTier("admin-user", RateLimitTier.UNLIMITED);

        assertTrue(result.allowed());
        assertEquals(-1, result.remainingTokens());
        assertEquals(RateLimitTier.UNLIMITED, result.tier());
    }

    @Test
    @DisplayName("Should check rate limit using user tier")
    void shouldCheckRateLimitUsingUserTier() {
        when(userService.getRateLimitTier("premium-user")).thenReturn(RateLimitTier.PREMIUM);

        RateLimiterService.RateLimitResult result = 
            rateLimiterService.checkRateLimit("premium-user");

        assertTrue(result.allowed());
        assertEquals(RateLimitTier.PREMIUM, result.tier());
    }

    @Test
    @DisplayName("Should use FREE tier for unknown users")
    void shouldUseFreeTierForUnknownUsers() {
        when(userService.getRateLimitTier("unknown-user")).thenReturn(RateLimitTier.FREE);

        RateLimiterService.RateLimitResult result = 
            rateLimiterService.checkRateLimit("unknown-user");

        assertTrue(result.allowed());
        assertEquals(RateLimitTier.FREE, result.tier());
    }

    @Test
    @DisplayName("Should check rate limit by IP")
    void shouldCheckRateLimitByIp() {
        RateLimiterService.RateLimitResult result = 
            rateLimiterService.checkRateLimitByIp("192.168.1.100");

        assertTrue(result.allowed());
        assertEquals(RateLimitTier.FREE, result.tier()); // IP-based uses FREE tier
    }

    @Test
    @DisplayName("Should get available tokens")
    void shouldGetAvailableTokens() {
        long tokens = rateLimiterService.getAvailableTokens("test-user", RateLimitTier.BASIC);

        assertEquals(120, tokens); // BASIC tier has 120 requests/minute
    }

    @Test
    @DisplayName("Should return max tokens for unlimited tier")
    void shouldReturnMaxTokensForUnlimitedTier() {
        long tokens = rateLimiterService.getAvailableTokens("admin", RateLimitTier.UNLIMITED);

        assertEquals(Long.MAX_VALUE, tokens);
    }

    @Test
    @DisplayName("Should decrement tokens on consumption")
    void shouldDecrementTokensOnConsumption() {
        String identifier = "decrement-test-user";
        
        // First request
        RateLimiterService.RateLimitResult first = 
            rateLimiterService.checkRateLimitForTier(identifier, RateLimitTier.FREE);
        
        // Second request
        RateLimiterService.RateLimitResult second = 
            rateLimiterService.checkRateLimitForTier(identifier, RateLimitTier.FREE);

        assertTrue(first.allowed());
        assertTrue(second.allowed());
        assertEquals(first.remainingTokens() - 1, second.remainingTokens());
    }

    @Test
    @DisplayName("Should eventually limit requests when exhausted")
    void shouldEventuallyLimitRequestsWhenExhausted() {
        String identifier = "exhaust-test-" + System.currentTimeMillis();
        
        // Exhaust all tokens for FREE tier (60 requests/minute)
        int allowed = 0;
        int blocked = 0;
        
        for (int i = 0; i < 70; i++) {
            RateLimiterService.RateLimitResult result = 
                rateLimiterService.checkRateLimitForTier(identifier, RateLimitTier.FREE);
            if (result.allowed()) {
                allowed++;
            } else {
                blocked++;
            }
        }

        assertEquals(60, allowed); // FREE tier allows 60 requests
        assertEquals(10, blocked); // Remaining 10 should be blocked
    }

    @Test
    @DisplayName("Should provide retry after seconds when limited")
    void shouldProvideRetryAfterSecondsWhenLimited() {
        String identifier = "retry-test-" + System.currentTimeMillis();
        
        // Exhaust all tokens
        for (int i = 0; i < 60; i++) {
            rateLimiterService.checkRateLimitForTier(identifier, RateLimitTier.FREE);
        }
        
        // Next request should be limited
        RateLimiterService.RateLimitResult result = 
            rateLimiterService.checkRateLimitForTier(identifier, RateLimitTier.FREE);

        assertFalse(result.allowed());
        assertTrue(result.isLimited());
        assertTrue(result.retryAfterSeconds() >= 0);
    }
}
