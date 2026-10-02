package com.portfolio.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP interceptor that applies rate limiting to incoming requests.
 * Adds rate limit headers to responses.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_TIER = "X-RateLimit-Tier";
    
    private final RateLimiterService rateLimiterService;
    
    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                              Object handler) throws Exception {
        
        // Skip rate limiting for health checks and actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.equals("/health")) {
            return true;
        }
        
        // Get identifier (username from header or IP)
        String identifier = extractIdentifier(request);
        
        RateLimiterService.RateLimitResult result;
        if (identifier.startsWith("ip:")) {
            result = rateLimiterService.checkRateLimitByIp(identifier.substring(3));
        } else {
            result = rateLimiterService.checkRateLimit(identifier);
        }
        
        // Add rate limit headers
        addRateLimitHeaders(response, result);
        
        if (result.isLimited()) {
            log.warn("Rate limit exceeded for {} on path {}", identifier, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"retryAfter\":" + 
                result.retryAfterSeconds() + "}");
            response.setContentType("application/json");
            return false;
        }
        
        return true;
    }
    
    private String extractIdentifier(HttpServletRequest request) {
        // Check for API key or username in header
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "apikey:" + apiKey;
        }
        
        String username = request.getHeader("X-Username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        
        // Fall back to client IP
        String clientIp = getClientIp(request);
        return "ip:" + clientIp;
    }
    
    private String getClientIp(HttpServletRequest request) {
        // Check for forwarded headers (if behind proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP in the chain
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private void addRateLimitHeaders(HttpServletResponse response, 
                                      RateLimiterService.RateLimitResult result) {
        if (result.tier().hasLimits()) {
            response.setHeader(HEADER_LIMIT, String.valueOf(result.tier().getRequestsPerMinute()));
            response.setHeader(HEADER_REMAINING, String.valueOf(result.remainingTokens()));
            response.setHeader(HEADER_TIER, result.tier().name());
            
            if (result.isLimited()) {
                response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
            }
        }
    }
}
