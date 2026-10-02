package com.portfolio.cloudnative;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cloud Native Microservices Platform - Main Application
 * 
 * A production-ready cloud-native platform demonstrating:
 * - Kubernetes deployment patterns (Deployments, Services, ConfigMaps, Secrets)
 * - Service Mesh configuration (Istio virtual services, destination rules)
 * - Distributed tracing with Micrometer and Zipkin
 * - Circuit breaker patterns with Resilience4j
 * - Service-to-service communication with Feign clients
 * - Health checks and readiness probes
 * - Horizontal Pod Autoscaling configuration
 * 
 * @author Portfolio Project
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class CloudNativeMicroservicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudNativeMicroservicesApplication.class, args);
    }
}
