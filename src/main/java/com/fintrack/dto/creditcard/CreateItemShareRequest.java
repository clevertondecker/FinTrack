package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for creating or replacing item shares (split of an invoice item among participants).
 *
 * Business rules: the list must not be empty; the sum of all percentages must equal 1.0 (100%);
 * each entry represents one participant and must have exactly one of {@code userId} or {@code contactId}
 * (system user or trusted contact from the owner's circle of trust). Trusted contacts do not need to
 * exist as users.
 */
public record CreateItemShareRequest(
    @NotNull(message = "User shares are required.")
    List<UserShare> userShares
) {
    /**
     * One participant in the split: either a system user ({@code userId}) or a trusted contact ({@code contactId}).
     * Exactly one must be set; the other must be null. Percentages across all UserShares for the item must sum to 1.0.
     */
    public record UserShare(
        Long userId,
        Long contactId,
        @NotNull(message = "Share percentage is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Share percentage must be non-negative.")
        @DecimalMax(value = "1.0", inclusive = true, message = "Share percentage cannot exceed 1.0 (100%).")
        BigDecimal percentage,
        @NotNull(message = "Responsible flag is required.")
        Boolean responsible
    ) {}
} 