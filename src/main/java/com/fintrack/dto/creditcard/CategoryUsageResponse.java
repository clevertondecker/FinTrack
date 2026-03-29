package com.fintrack.dto.creditcard;

public record CategoryUsageResponse(
    Long categoryId,
    String name,
    long itemCount,
    long ruleCount,
    long budgetCount,
    long subscriptionCount
) {
    public long totalUsage() {
        return itemCount + ruleCount + budgetCount + subscriptionCount;
    }
}
