package com.fintrack.domain.creditcard;

/**
 * Enum representing the source of an invoice item's category assignment.
 * Used to track how a category was assigned to an item for auditing and analytics.
 */
public enum CategorizationSource {

    /**
     * Category was assigned manually by the user.
     */
    MANUAL,

    /**
     * Category was automatically applied by a merchant rule.
     */
    AUTO_RULE,

    /**
     * Category was suggested by a merchant rule but not yet confirmed.
     */
    SUGGESTED
}
