package com.fintrack.dto.subscription;

import com.fintrack.domain.subscription.BillingCycle;
import com.fintrack.domain.subscription.Subscription;
import com.fintrack.domain.subscription.SubscriptionSource;
import com.fintrack.domain.subscription.SubscriptionStatus;
import com.fintrack.dto.creditcard.CategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
    Long id,
    String name,
    String merchantKey,
    BigDecimal expectedAmount,
    CategoryResponse category,
    String creditCardName,
    Long creditCardId,
    BillingCycle billingCycle,
    SubscriptionStatus status,
    SubscriptionSource source,
    LocalDate startDate,
    LocalDate lastDetectedDate,
    BigDecimal lastDetectedAmount,
    boolean priceChanged,
    String monthStatus
) {
    public static SubscriptionResponse from(final Subscription sub, final String monthStatus) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getName(),
                sub.getMerchantKey(),
                sub.getExpectedAmount(),
                CategoryResponse.from(sub.getCategory()),
                sub.getCreditCard() != null ? sub.getCreditCard().getName() : null,
                sub.getCreditCard() != null ? sub.getCreditCard().getId() : null,
                sub.getBillingCycle(),
                sub.getStatus(),
                sub.getSource(),
                sub.getStartDate(),
                sub.getLastDetectedDate(),
                sub.getLastDetectedAmount(),
                sub.hasPriceChanged(),
                monthStatus
        );
    }
}
