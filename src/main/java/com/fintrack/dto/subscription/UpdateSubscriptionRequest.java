package com.fintrack.dto.subscription;

import com.fintrack.domain.subscription.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateSubscriptionRequest(
    @NotBlank String name,
    @NotNull @Positive BigDecimal expectedAmount,
    @NotNull BillingCycle billingCycle,
    Long categoryId,
    Long creditCardId
) {}
