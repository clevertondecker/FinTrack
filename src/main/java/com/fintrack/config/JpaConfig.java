package com.fintrack.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for entity scanning and repository detection.
 * Separated from the main application class so that {@code @WebMvcTest}
 * slices do not attempt to initialize the persistence layer.
 */
@Configuration
@EntityScan(basePackages = {
    "com.fintrack.domain.creditcard",
    "com.fintrack.domain.user",
    "com.fintrack.domain.contact",
    "com.fintrack.domain.invoice"
})
@EnableJpaRepositories(basePackages = {
    "com.fintrack.domain.creditcard",
    "com.fintrack.infrastructure.persistence.creditcard",
    "com.fintrack.infrastructure.persistence.user",
    "com.fintrack.infrastructure.persistence.contact",
    "com.fintrack.infrastructure.persistence.invoice"
})
public class JpaConfig {
}
