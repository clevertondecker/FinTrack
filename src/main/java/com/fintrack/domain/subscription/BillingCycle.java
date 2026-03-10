package com.fintrack.domain.subscription;

/** Defines how often a subscription is billed. */
public enum BillingCycle {
    /** Billed every month. */
    MONTHLY,
    /** Billed every 3 months. */
    QUARTERLY,
    /** Billed every 6 months. */
    SEMI_ANNUAL,
    /** Billed once a year. */
    ANNUAL
}
