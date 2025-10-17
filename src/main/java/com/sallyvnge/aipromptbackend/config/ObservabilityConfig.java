package com.sallyvnge.aipromptbackend.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures a NOOP ObservationRegistry when observations are disabled,
 * which removes the Security Observation filter decoration and related logs.
 */
@Configuration
@ConditionalOnProperty(name = "management.observations.enabled", havingValue = "false", matchIfMissing = false)
public class ObservabilityConfig {

    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }
}

