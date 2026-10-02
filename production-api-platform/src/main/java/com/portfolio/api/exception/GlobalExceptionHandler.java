package com.portfolio.api.exception;

import com.portfolio.api.ratelimit.RateLimitException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST and GraphQL errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends DataFetcherExceptionResolverAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // ============================================
    // REST Exception Handlers
    // ============================================
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(errorResponse("Invalid argument", "BAD_REQUEST", e.getMessage()));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("Invalid state: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(errorResponse("Invalid state", "CONFLICT", e.getMessage()));
    }
    
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        Map<String, Object> body = errorResponse("Rate limit exceeded", "RATE_LIMIT_EXCEEDED", e.getMessage());
        body.put("retryAfter", e.getRetryAfterSeconds());
        body.put("tier", e.getTier());
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
            .body(body);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse("Internal server error", "INTERNAL_ERROR", 
                "An unexpected error occurred"));
    }
    
    // ============================================
    // GraphQL Exception Handler
    // ============================================
    
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType errorType = ErrorType.INTERNAL_ERROR;
        
        if (ex instanceof IllegalArgumentException) {
            errorType = ErrorType.BAD_REQUEST;
        } else if (ex instanceof IllegalStateException) {
            errorType = ErrorType.BAD_REQUEST;
        } else if (ex instanceof RateLimitException) {
            errorType = ErrorType.FORBIDDEN;
        }
        
        return GraphqlErrorBuilder.newError()
            .errorType(errorType)
            .message(ex.getMessage())
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation())
            .build();
    }
    
    // ============================================
    // Helper Methods
    // ============================================
    
    private Map<String, Object> errorResponse(String error, String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("code", code);
        response.put("message", message);
        response.put("timestamp", Instant.now().toString());
        return response;
    }
}
