package com.fintrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
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
public class FinTranckApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinTranckApplication.class, args);
    }

    private FinTranckApplication() {
        // Utility class constructor
    }
}
