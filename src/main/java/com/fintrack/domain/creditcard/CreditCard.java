package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    /** The credit card's unique identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The credit card's name. */
    @Column(nullable = false)
    private String name;

    /** The last four digits of the credit card. */
    @Column(nullable = false, length = 4)
    private String lastFourDigits;

    /** The credit card's limit. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;

    /** The credit card's owner. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** The bank that issued the credit card. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    /** The credit card's creation timestamp. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** The credit card's last update timestamp. */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Whether the credit card is active. */
    @Column(nullable = false)
    private boolean active = true;

    /** The type of the credit card. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType = CardType.PHYSICAL;

    /** The parent card for additional cards. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_card_id")
    private CreditCard parentCard;

    /** The name on the card. */
    @Column(length = 100)
    private String cardholderName;

    /**
     * The user this card is assigned to (who uses it).
     * When null, the card is for the owner. Used for additional/virtual cards used by other people.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    /**
     * Protected constructor for JPA only.
     */
    protected CreditCard() {}

    /**
     * Private constructor for CreditCard. Use the static factory method to create instances.
     *
     * @param theName the credit card's name. Must not be null or blank.
     * @param theLastFourDigits the last four digits of the card. Must be exactly 4 digits.
     * @param theLimit the credit card's limit. Must be positive.
     * @param theOwner the credit card's owner. Must not be null.
     * @param theBank the bank that issued the card. Must not be null.
     * @param theCardType the type of the card. Must not be null.
     * @param theParentCard the parent card for additional cards. Can be null.
     * @param theCardholderName the name on the card. Can be null.
     * @param theAssignedUser the user this card is assigned to (who uses it). Can be null.
     */
    private CreditCard(final String theName, final String theLastFourDigits,
                      final BigDecimal theLimit, final User theOwner, final Bank theBank,
                      final CardType theCardType, final CreditCard theParentCard, final String theCardholderName,
                      final User theAssignedUser) {
        Validate.notBlank(theName, "Credit card name must not be null or blank.");
        Validate.notBlank(theLastFourDigits, "Last four digits must not be null or blank.");
        Validate.isTrue(theLastFourDigits.length() == 4, "Last four digits must be exactly 4 characters.");
        Validate.isTrue(theLastFourDigits.matches("\\d{4}"), "Last four digits must contain only digits.");
        Validate.notNull(theLimit, "Credit card limit must not be null.");
        Validate.isTrue(theLimit.compareTo(BigDecimal.ZERO) > 0, "Credit card limit must be positive.");
        Validate.notNull(theOwner, "Credit card owner must not be null.");
        Validate.notNull(theBank, "Bank must not be null.");
        Validate.notNull(theCardType, "Card type must not be null.");

        name = theName;
        lastFourDigits = theLastFourDigits;
        creditLimit = theLimit;
        owner = theOwner;
        bank = theBank;
        cardType = theCardType;
        parentCard = theParentCard;
        cardholderName = theCardholderName;
        assignedUser = theAssignedUser;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Static factory method to create a new CreditCard instance.
     *
     * @param name the credit card's name. Cannot be null or blank.
     * @param lastFourDigits the last four digits of the card. Must be exactly 4 digits.
     * @param limit the credit card's limit. Must be positive.
     * @param owner the credit card's owner. Cannot be null.
     * @param bank the bank that issued the card. Cannot be null.
     * @return a validated CreditCard entity. Never null.
     */
    public static CreditCard of(final String name, final String lastFourDigits,
                               final BigDecimal limit, final User owner, final Bank bank) {
        return new CreditCard(name, lastFourDigits, limit, owner, bank, CardType.PHYSICAL, null, null, null);
    }

    /**
     * Static factory method to create a new CreditCard instance with specific type.
     *
     * @param name the credit card's name. Cannot be null or blank.
     * @param lastFourDigits the last four digits of the card. Must be exactly 4 digits.
     * @param limit the credit card's limit. Must be positive.
     * @param owner the credit card's owner. Cannot be null.
     * @param bank the bank that issued the card. Cannot be null.
     * @param cardType the type of the card. Cannot be null.
     * @param parentCard the parent card for additional cards. Can be null.
     * @param cardholderName the name on the card. Can be null.
     * @param assignedUser the user this card is assigned to (who uses it). Can be null.
     * @return a validated CreditCard entity. Never null.
     */
    public static CreditCard of(final String name, final String lastFourDigits,
                               final BigDecimal limit, final User owner, final Bank bank,
                               final CardType cardType, final CreditCard parentCard, final String cardholderName,
                               final User assignedUser) {
        return new CreditCard(
            name, lastFourDigits, limit, owner, bank, cardType, parentCard, cardholderName, assignedUser);
    }

    /**
     * Static factory for credit card with type, parent and cardholder (no assigned user).
     *
     * @param name the credit card's name. Cannot be null or blank.
     * @param lastFourDigits the last four digits of the card. Must be exactly 4 digits.
     * @param limit the credit card's limit. Must be positive.
     * @param owner the credit card's owner. Cannot be null.
     * @param bank the bank that issued the card. Cannot be null.
     * @param cardType the type of the card. Cannot be null.
     * @param parentCard the parent card for additional cards. Can be null.
     * @param cardholderName the name on the card. Can be null.
     * @return a validated CreditCard entity. Never null.
     */
    public static CreditCard of(final String name, final String lastFourDigits,
                               final BigDecimal limit, final User owner, final Bank bank,
                               final CardType cardType, final CreditCard parentCard, final String cardholderName) {
        return of(name, lastFourDigits, limit, owner, bank, cardType, parentCard, cardholderName, null);
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

    /**
     * Updates the credit card's name.
     *
     * @param newName the new name. Cannot be null or blank.
     */
    public void updateName(final String newName) {
        Validate.notBlank(newName, "Credit card name must not be null or blank.");
        name = newName;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the credit card's bank.
     *
     * @param newBank the new bank. Cannot be null.
     */
    public void updateBank(final Bank newBank) {
        Validate.notNull(newBank, "Bank must not be null.");
        bank = newBank;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the credit card's type.
     *
     * @param newCardType the new card type. Cannot be null.
     */
    public void updateCardType(final CardType newCardType) {
        Validate.notNull(newCardType, "Card type must not be null.");
        cardType = newCardType;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the credit card's parent card.
     *
     * @param newParentCard the new parent card. Can be null.
     */
    public void updateParentCard(final CreditCard newParentCard) {
        parentCard = newParentCard;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the credit card's cardholder name.
     *
     * @param newCardholderName the new cardholder name. Can be null.
     */
    public void updateCardholderName(final String newCardholderName) {
        cardholderName = newCardholderName;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the user this card is assigned to.
     *
     * @param newAssignedUser the assigned user. Can be null.
     */
    public void updateAssignedUser(final User newAssignedUser) {
        assignedUser = newAssignedUser;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Gets the credit card's unique identifier.
     *
     * @return the credit card's ID. May be null if not persisted.
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the credit card's name.
     *
     * @return the credit card's name. Never null or blank.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the last four digits of the credit card.
     *
     * @return the last four digits. Never null or blank, exactly 4 characters.
     */
    public String getLastFourDigits() {
        return lastFourDigits;
    }

    /**
     * Gets the credit card's limit.
     *
     * @return the credit card's limit. Never null, always positive.
     */
    public BigDecimal getLimit() {
        return creditLimit;
    }

    /**
     * Gets the credit card's owner.
     *
     * @return the credit card's owner. Never null.
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Gets the bank that issued the credit card.
     *
     * @return the bank. Never null.
     */
    public Bank getBank() {
        return bank;
    }

    /**
     * Gets the credit card's creation timestamp.
     *
     * @return the creation timestamp. Never null.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the credit card's last update timestamp.
     *
     * @return the last update timestamp. Never null.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Checks if the credit card is active.
     *
     * @return true if the credit card is active, false otherwise.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Gets the credit card's type.
     *
     * @return the credit card's type. Never null.
     */
    public CardType getCardType() {
        return cardType;
    }

    /**
     * Gets the parent card for additional cards.
     *
     * @return the parent card. May be null for physical and virtual cards.
     */
    public CreditCard getParentCard() {
        return parentCard;
    }

    /**
     * Gets the name on the card.
     *
     * @return the cardholder name. May be null.
     */
    public String getCardholderName() {
        return cardholderName;
    }

    /**
     * Gets the user this card is assigned to (who uses it).
     *
     * @return the assigned user. May be null (card is for the owner).
     */
    public User getAssignedUser() {
        return assignedUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreditCard creditCard)) {
            return false;
        }
        return Objects.equals(id, creditCard.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CreditCard{"
            + "id=" + id
            + ", name='" + name + '\''
            + ", lastFourDigits='" + lastFourDigits + '\''
            + ", limit=" + creditLimit
            + ", owner=" + owner
            + ", bank=" + bank
            + ", cardType=" + cardType
            + ", parentCard=" + (parentCard != null ? parentCard.getId() : "null")
            + ", cardholderName='" + cardholderName + '\''
            + ", assignedUser=" + (assignedUser != null ? assignedUser.getId() : "null")
            + ", createdAt=" + createdAt
            + ", updatedAt=" + updatedAt
            + ", active=" + active
            + '}';
    }
}