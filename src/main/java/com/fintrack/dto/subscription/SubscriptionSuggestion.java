package com.fintrack.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionSuggestion(
    String merchantKey,
    String displayName,
    BigDecimal averageAmount,
    int occurrences,
    LocalDate firstSeen,
    LocalDate lastSeen,
    String categoryName,
    String categoryColor,
    String cardName
) {}
