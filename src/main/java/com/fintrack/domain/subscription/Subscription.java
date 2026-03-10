package com.fintrack.domain.subscription;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "merchant_key", nullable = false, length = 255)
    private String merchantKey;

    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionSource source;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "last_detected_date")
    private LocalDate lastDetectedDate;

    @Column(name = "last_detected_amount", precision = 15, scale = 2)
    private BigDecimal lastDetectedAmount;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Subscription() { }

    private Subscription(final User theOwner, final String theName, final String theMerchantKey,
                         final BigDecimal theExpectedAmount, final BillingCycle theBillingCycle,
                         final SubscriptionSource theSource) {
        Validate.notNull(theOwner, "Owner must not be null");
        Validate.notBlank(theName, "Name must not be blank");
        Validate.notBlank(theMerchantKey, "Merchant key must not be blank");
        Validate.notNull(theExpectedAmount, "Expected amount must not be null");
        Validate.isTrue(theExpectedAmount.compareTo(BigDecimal.ZERO) > 0, "Expected amount must be positive");
        Validate.notNull(theBillingCycle, "Billing cycle must not be null");
        Validate.notNull(theSource, "Source must not be null");

        this.owner = theOwner;
        this.name = theName;
        this.merchantKey = theMerchantKey;
        this.expectedAmount = theExpectedAmount;
        this.billingCycle = theBillingCycle;
        this.source = theSource;
        this.status = SubscriptionStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Subscription manual(final User owner, final String name, final String merchantKey,
                                      final BigDecimal expectedAmount, final BillingCycle billingCycle) {
        return new Subscription(owner, name, merchantKey, expectedAmount, billingCycle, SubscriptionSource.MANUAL);
    }

    public static Subscription autoDetected(final User owner, final String name, final String merchantKey,
                                            final BigDecimal expectedAmount, final LocalDate firstSeen) {
        Subscription sub = new Subscription(owner, name, merchantKey, expectedAmount,
                BillingCycle.MONTHLY, SubscriptionSource.AUTO_DETECTED);
        sub.startDate = firstSeen;
        sub.lastDetectedDate = firstSeen;
        sub.lastDetectedAmount = expectedAmount;
        return sub;
    }

    public void updateDetails(final String newName, final BigDecimal newAmount, final BillingCycle newCycle) {
        Validate.notBlank(newName, "Name must not be blank");
        Validate.notNull(newAmount, "Amount must not be null");
        Validate.isTrue(newAmount.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive");
        this.name = newName;
        this.expectedAmount = newAmount;
        this.billingCycle = newCycle;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignCategory(final Category newCategory) {
        this.category = newCategory;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignCreditCard(final CreditCard card) {
        this.creditCard = card;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordDetection(final LocalDate detectedDate, final BigDecimal detectedAmount) {
        this.lastDetectedDate = detectedDate;
        this.lastDetectedAmount = detectedAmount;
        if (this.startDate == null) {
            this.startDate = detectedDate;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void pause() {
        this.status = SubscriptionStatus.PAUSED;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasPriceChanged() {
        if (lastDetectedAmount == null) {
            return false;
        }
        return expectedAmount.compareTo(lastDetectedAmount) != 0;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getMerchantKey() {
        return merchantKey;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public Category getCategory() {
        return category;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public SubscriptionSource getSource() {
        return source;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getLastDetectedDate() {
        return lastDetectedDate;
    }

    public BigDecimal getLastDetectedAmount() {
        return lastDetectedAmount;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Subscription that = (Subscription) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
