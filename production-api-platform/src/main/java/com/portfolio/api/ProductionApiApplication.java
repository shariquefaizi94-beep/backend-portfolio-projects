package com.portfolio.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Production API Platform - Main Application
 * 
 * A production-grade API platform demonstrating:
 * - GraphQL API with Spring GraphQL
 * - Token Bucket Rate Limiting with Bucket4j
 * - API Gateway Pattern with routing and transformation
 * - Circuit Breaker with Resilience4j
 * - Request validation and authentication
 * - Caching with Caffeine
 * - Comprehensive metrics and monitoring
 * 
 * @author Portfolio Project
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class ProductionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductionApiApplication.class, args);
    }
}
