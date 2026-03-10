package com.fintrack.domain.subscription;

/** How a subscription was originally created. */
public enum SubscriptionSource {
    /** Detected automatically from recurring invoice items. */
    AUTO_DETECTED,
    /** Created manually by the user. */
    MANUAL
}
