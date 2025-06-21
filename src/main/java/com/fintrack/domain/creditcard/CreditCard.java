package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Entity representing a credit card.
 * Contains credit card information and links to its owner.
 */
@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 4)
    private String lastFourDigits;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Protected constructor for JPA only.
     */
    protected CreditCard() {}

    /**
     * Private constructor for CreditCard. Use the static factory method to create
     * instances.
     *
     * @param theName the credit card's name. Must not be null or blank.
     *
     * @param theLastFourDigits the last four digits of the card. Must be exactly 4 digits.
     *
     * @param theLimit the credit card's limit. Must be positive.
     *
     * @param theOwner the credit card's owner. Must not be null.
     *
     * @param theBank the bank that issued the card. Must not be null.
     */
    private CreditCard(final String theName, final String theLastFourDigits,
                      final BigDecimal theLimit, final User theOwner, final Bank theBank) {
        Validate.notBlank(theName, "Credit card name must not be null or blank.");
        Validate.notBlank(theLastFourDigits, "Last four digits must not be null or blank.");
        Validate.isTrue(theLastFourDigits.length() == 4, "Last four digits must be exactly 4 characters.");
        Validate.isTrue(theLastFourDigits.matches("\\d{4}"), "Last four digits must contain only digits.");
        Validate.notNull(theLimit, "Credit card limit must not be null.");
        Validate.isTrue(theLimit.compareTo(BigDecimal.ZERO) > 0, "Credit card limit must be positive.");
        Validate.notNull(theOwner, "Credit card owner must not be null.");
        Validate.notNull(theBank, "Bank must not be null.");

        name = theName;
        lastFourDigits = theLastFourDigits;
        creditLimit = theLimit;
        owner = theOwner;
        bank = theBank;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Static factory method to create a new CreditCard instance.
     *
     * @param name the credit card's name. Cannot be null or blank.
     *
     * @param lastFourDigits the last four digits of the card. Must be exactly 4 digits.
     *
     * @param limit the credit card's limit. Must be positive.
     *
     * @param owner the credit card's owner. Cannot be null.
     *
     * @param bank the bank that issued the card. Cannot be null.
     *
     * @return a validated CreditCard entity. Never null.
     */
    public static CreditCard of(final String name, final String lastFourDigits,
                               final BigDecimal limit, final User owner, final Bank bank) {
        return new CreditCard(name, lastFourDigits, limit, owner, bank);
    }

    /**
     * Deactivates the credit card.
     */
    public void deactivate() {
        active = false;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Activates the credit card.
     */
    public void activate() {
        active = true;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the credit card's limit.
     *
     * @param newLimit the new limit. Must be positive.
     */
    public void updateLimit(final BigDecimal newLimit) {
        Validate.notNull(newLimit, "New limit must not be null.");
        Validate.isTrue(newLimit.compareTo(BigDecimal.ZERO) > 0, "New limit must be positive.");

        creditLimit = newLimit;
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLastFourDigits() { return lastFourDigits; }
    public BigDecimal getLimit() { return creditLimit; }
    public User getOwner() { return owner; }
    public Bank getBank() { return bank; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreditCard creditCard)) return false;
        return Objects.equals(id, creditCard.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CreditCard{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lastFourDigits='" + lastFourDigits + '\'' +
                ", limit=" + creditLimit +
                ", owner=" + owner +
                ", bank=" + bank +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", active=" + active +
                '}';
    }
}