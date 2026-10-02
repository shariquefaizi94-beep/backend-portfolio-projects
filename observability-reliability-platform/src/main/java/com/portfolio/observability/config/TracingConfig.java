package com.portfolio.observability.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Distributed tracing configuration.
 * Configures Micrometer tracing with support for span propagation and context.
 */
@Configuration
public class TracingConfig {

    /**
     * Enables @Observed annotation support for automatic span creation.
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
