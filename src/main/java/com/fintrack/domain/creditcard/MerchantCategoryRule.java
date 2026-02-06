package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.commons.lang3.Validate;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a rule that maps a normalized merchant name to a category.
 * Rules are user-specific, allowing each user to have their own categorization preferences.
 *
 * <p>The rule tracks confidence metrics to determine when to auto-apply categories:
 * <ul>
 *   <li>timesConfirmed: incremented when user manually assigns same category</li>
 *   <li>timesOverridden: incremented when user changes to different category</li>
 *   <li>autoApply: enabled when confidence threshold is reached</li>
 * </ul>
 */
@Entity
@Table(
    name = "merchant_category_rules",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_merchant_rule_user_merchant",
        columnNames = {"user_id", "merchant_key"}
    ),
    indexes = {
        @Index(name = "idx_merchant_rule_user", columnList = "user_id"),
        @Index(name = "idx_merchant_rule_merchant_key", columnList = "merchant_key")
    }
)
public class MerchantCategoryRule {

    /** Threshold of confirmations required to enable auto-apply. */
    private static final int AUTO_APPLY_THRESHOLD = 1;

    /** The rule's unique identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who owns this rule. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The normalized merchant key used for matching. */
    @Column(name = "merchant_key", nullable = false, length = 255)
    private String merchantKey;

    /** An example of the original description for reference. */
    @Column(name = "original_description", length = 500)
    private String originalDescription;

    /** The category to apply when this rule matches. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Number of times this rule was applied. */
    @Column(name = "times_applied", nullable = false)
    private int timesApplied = 0;

    /** Number of times the user confirmed this categorization. */
    @Column(name = "times_confirmed", nullable = false)
    private int timesConfirmed = 0;

    /** Number of times the user changed the category. */
    @Column(name = "times_overridden", nullable = false)
    private int timesOverridden = 0;

    /** Whether to automatically apply this rule on import. */
    @Column(name = "auto_apply", nullable = false)
    private boolean autoApply = false;

    /** When this rule was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** When this rule was last updated. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Protected constructor for JPA only.
     */
    protected MerchantCategoryRule() {
    }

    /**
     * Private constructor for MerchantCategoryRule.
     *
     * @param theUser the user who owns this rule. Must not be null.
     * @param theMerchantKey the normalized merchant key. Must not be blank.
     * @param theOriginalDescription an example of the original description. Can be null.
     * @param theCategory the category to apply. Must not be null.
     */
    private MerchantCategoryRule(
            final User theUser,
            final String theMerchantKey,
            final String theOriginalDescription,
            final Category theCategory) {
        Validate.notNull(theUser, "User must not be null.");
        Validate.notBlank(theMerchantKey, "Merchant key must not be blank.");
        Validate.notNull(theCategory, "Category must not be null.");

        this.user = theUser;
        this.merchantKey = theMerchantKey;
        this.originalDescription = theOriginalDescription;
        this.category = theCategory;
        this.timesConfirmed = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Check if threshold is met (for threshold = 1, this enables auto-apply)
        checkAutoApplyThreshold();
    }

    /**
     * Static factory method to create a new MerchantCategoryRule.
     *
     * @param user the user who owns this rule. Cannot be null.
     * @param merchantKey the normalized merchant key. Cannot be blank.
     * @param originalDescription an example of the original description. Can be null.
     * @param category the category to apply. Cannot be null.
     * @return a new MerchantCategoryRule instance. Never null.
     */
    public static MerchantCategoryRule of(
            final User user,
            final String merchantKey,
            final String originalDescription,
            final Category category) {
        return new MerchantCategoryRule(user, merchantKey, originalDescription, category);
    }

    /**
     * Records that this rule was used to categorize an item.
     * Increments the timesApplied counter.
     */
    public void recordApplication() {
        this.timesApplied++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Records that the user confirmed this categorization (assigned same category manually).
     * Increments timesConfirmed and may enable autoApply if threshold is reached.
     */
    public void recordConfirmation() {
        this.timesConfirmed++;
        this.updatedAt = LocalDateTime.now();
        checkAutoApplyThreshold();
    }

    /**
     * Records that the user overrode this categorization with a different category.
     * Updates the category and increments timesOverridden.
     *
     * @param newCategory the new category. Must not be null.
     */
    public void recordOverride(final Category newCategory) {
        Validate.notNull(newCategory, "New category must not be null.");

        this.category = newCategory;
        this.timesOverridden++;
        this.timesConfirmed = 1; // Reset confirmations for new category
        this.autoApply = false;  // Disable auto-apply until re-confirmed
        this.updatedAt = LocalDateTime.now();

        // Check if threshold is met (for threshold = 1, this will re-enable)
        checkAutoApplyThreshold();
    }

    /**
     * Checks if auto-apply should be enabled based on confirmation threshold.
     */
    private void checkAutoApplyThreshold() {
        if (this.timesConfirmed >= AUTO_APPLY_THRESHOLD && !this.autoApply) {
            this.autoApply = true;
        }
    }

    /**
     * Calculates the confidence score for this rule.
     * Based on the ratio of confirmations to total interactions.
     *
     * @return a confidence score between 0.0 and 1.0. Never null.
     */
    public double getConfidenceScore() {
        int total = timesConfirmed + timesOverridden;
        if (total == 0) {
            return 0.0;
        }
        return (double) timesConfirmed / total;
    }

    /**
     * Checks if this rule should be auto-applied.
     *
     * @return true if the rule should be auto-applied, false otherwise.
     */
    public boolean shouldAutoApply() {
        return autoApply && timesConfirmed > timesOverridden;
    }

    // Getters

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getMerchantKey() {
        return merchantKey;
    }

    public String getOriginalDescription() {
        return originalDescription;
    }

    public Category getCategory() {
        return category;
    }

    public int getTimesApplied() {
        return timesApplied;
    }

    public int getTimesConfirmed() {
        return timesConfirmed;
    }

    public int getTimesOverridden() {
        return timesOverridden;
    }

    public boolean isAutoApply() {
        return autoApply;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MerchantCategoryRule that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MerchantCategoryRule{"
            + "id=" + id
            + ", merchantKey='" + merchantKey + '\''
            + ", category=" + (category != null ? category.getName() : "null")
            + ", timesConfirmed=" + timesConfirmed
            + ", timesOverridden=" + timesOverridden
            + ", autoApply=" + autoApply
            + '}';
    }
}
