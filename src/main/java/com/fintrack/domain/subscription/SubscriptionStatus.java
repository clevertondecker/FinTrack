package com.fintrack.domain.subscription;

/** Current lifecycle status of a subscription. */
public enum SubscriptionStatus {
    /** Subscription is currently active. */
    ACTIVE,
    /** Subscription is temporarily paused by the user. */
    PAUSED,
    /** Subscription has been cancelled. */
    CANCELLED
}
