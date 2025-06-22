package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for creating item shares among users.
 * Contains validation annotations for request validation.
 */
public record CreateItemShareRequest(
    
    @NotNull(message = "User shares are required.")
    List<UserShare> userShares
) {
    
    /**
     * DTO for individual user share information.
     */
    public record UserShare(
        
        @NotNull(message = "User ID is required.")
        @Positive(message = "User ID must be positive.")
        Long userId,
        
        @NotNull(message = "Share percentage is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Share percentage must be non-negative.")
        @DecimalMax(value = "1.0", inclusive = true, message = "Share percentage cannot exceed 1.0 (100%).")
        BigDecimal percentage,
        
        @NotNull(message = "Responsible flag is required.")
        Boolean responsible
    ) {}
} 