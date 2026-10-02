package com.portfolio.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filter for API key authentication.
 * Validates API keys from X-API-Key header.
 */
@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    
    // Simulated valid API keys (in production, these would be stored securely)
    private static final Set<String> VALID_API_KEYS = Set.of(
        "demo-api-key-12345",
        "premium-api-key-67890",
        "enterprise-api-key-abcde"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey != null && isValidApiKey(apiKey)) {
            log.debug("Valid API key for request: {}", path);
            filterChain.doFilter(request, response);
        } else if (apiKey == null) {
            // No API key provided - allow request but log
            log.debug("No API key for request: {} - proceeding as anonymous", path);
            filterChain.doFilter(request, response);
        } else {
            // Invalid API key
            log.warn("Invalid API key attempted: {}", maskApiKey(apiKey));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid API key\",\"code\":\"INVALID_API_KEY\"}");
        }
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.equals("/") ||
               path.equals("/health") ||
               path.startsWith("/actuator/health") ||
               path.equals("/graphiql") ||
               path.startsWith("/graphql") ||
               path.equals("/favicon.ico");
    }
    
    private boolean isValidApiKey(String apiKey) {
        return VALID_API_KEYS.contains(apiKey);
    }
    
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
