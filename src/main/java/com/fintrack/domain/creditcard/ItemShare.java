package com.fintrack.domain.creditcard;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Business rules:
 * - A share is assigned to exactly one participant.
 * - The participant is either:
 *   - a system user (has a FinTrack login), or
 *   - a trusted contact (person in the owner's circle of trust; does not need to exist as a user).
 *
 * - The user_id field is nullable:
 *   - set when the participant is a system user;
 *   - null when the participant is a trusted contact.
 *     In this case, trusted_contact_id and the display name/email snapshots are set.
 *
 * - For a given item, the sum of all shares' percentages must equal 1.0 (100%).
 * - Only shares assigned to a system user can be marked as paid.
 *   Shares assigned to trusted contacts are for tracking who owes what and cannot be marked as paid.
 */

@Entity
@Table(name = "item_shares")
public class ItemShare {

    /** The share's unique identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The system user this share belongs to.
     * Null when the share is assigned to a trusted contact
     * (business rule: participant is either user or contact).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * The trusted contact this share belongs to.
     * Null when the share is assigned to a system user (business rule: participant is either user or contact).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trusted_contact_id")
    private TrustedContact trustedContact;

    /**
     * Snapshot of the contact's name when the share is for a trusted contact.
     * Used for display and history even if the contact is later edited or removed.
     */
    @Column(name = "contact_display_name", length = 255)
    private String contactDisplayName;

    /**
     * Snapshot of the contact's email when the share is for a trusted contact.
     * Used for display and history even if the contact is later edited or removed.
     */
    @Column(name = "contact_display_email", length = 255)
    private String contactDisplayEmail;

    /** The invoice item this share belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_item_id", nullable = false)
    private InvoiceItem invoiceItem;

    /** The fraction of the item amount for this participant (0.0 to 1.0). Sum of all shares for the item is 1.0. */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal percentage;

    /** The monetary amount this participant is responsible for (portion of the item total). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Whether this participant is designated as responsible for paying this share. */
    @Column(nullable = false)
    private boolean responsible = false;

    /**
     * Whether this share has been marked as paid.
     * Only meaningful for shares assigned to a system user; contact shares cannot be marked as paid.
     */
    @Column(nullable = false)
    private boolean paid = false;

    /** The payment method used. */
    @Column(length = 50)
    private String paymentMethod;

    /** The date and time when the share was paid. */
    @Column
    private LocalDateTime paidAt;

    /** The share's creation timestamp. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** The share's last update timestamp. */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Protected constructor for JPA only.
     */
    protected ItemShare() {}

    /**
     * Private constructor for user-based share.
     */
    private ItemShare(final User theUser, final InvoiceItem theInvoiceItem,
                     final BigDecimal thePercentage, final BigDecimal theAmount, final boolean theResponsible) {
        Validate.notNull(theUser, "User must not be null.");
        Validate.notNull(theInvoiceItem, "Invoice item must not be null.");
        validatePercentageAndAmount(thePercentage, theAmount);

        user = theUser;
        trustedContact = null;
        contactDisplayName = null;
        contactDisplayEmail = null;
        invoiceItem = theInvoiceItem;
        percentage = thePercentage;
        amount = theAmount;
        responsible = theResponsible;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Private constructor for trusted-contact-based share.
     */
    private ItemShare(final TrustedContact theContact, final InvoiceItem theInvoiceItem,
                     final BigDecimal thePercentage, final BigDecimal theAmount, final boolean theResponsible) {
        Validate.notNull(theContact, "TrustedContact must not be null.");
        Validate.notNull(theInvoiceItem, "Invoice item must not be null.");
        validatePercentageAndAmount(thePercentage, theAmount);

        user = null;
        trustedContact = theContact;
        contactDisplayName = theContact.getName();
        contactDisplayEmail = theContact.getEmail();
        invoiceItem = theInvoiceItem;
        percentage = thePercentage;
        amount = theAmount;
        responsible = theResponsible;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    private static void validatePercentageAndAmount(BigDecimal thePercentage, BigDecimal theAmount) {
        Validate.notNull(thePercentage, "Percentage must not be null.");
        Validate.isTrue(thePercentage.compareTo(BigDecimal.ZERO) >= 0, "Percentage must be non-negative.");
        Validate.isTrue(thePercentage.compareTo(BigDecimal.ONE) <= 0, "Percentage cannot exceed 1.0 (100%%).");
        Validate.notNull(theAmount, "Amount must not be null.");
        Validate.isTrue(theAmount.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive.");
    }

    /**
     * Creates a share assigned to a system user (participant has login in FinTrack).
     *
     * @param user the system user this share belongs to (must not be null).
     * @param invoiceItem the invoice item being split.
     * @param percentage fraction of the item (0.0 to 1.0).
     * @param amount the monetary amount for this share.
     * @param responsible whether this user is responsible for paying.
     * @return a new share with user set and trusted contact null.
     */
    public static ItemShare of(final User user, final InvoiceItem invoiceItem,
                              final BigDecimal percentage, final BigDecimal amount, final boolean responsible) {
        return new ItemShare(user, invoiceItem, percentage, amount, responsible);
    }

    /**
     * Creates a share assigned to a system user (not responsible for payment).
     */
    public static ItemShare of(final User user, final InvoiceItem invoiceItem,
                              final BigDecimal percentage, final BigDecimal amount) {
        return new ItemShare(user, invoiceItem, percentage, amount, false);
    }

    /**
     * Creates a share assigned to a trusted contact (participant does not need to be a system user).
     * Name and email are stored as snapshots for display and history. Such shares cannot be marked as paid.
     *
     * @param contact the trusted contact this share belongs to (must not be null).
     * @param invoiceItem the invoice item being split.
     * @param percentage fraction of the item (0.0 to 1.0).
     * @param amount the monetary amount for this share.
     * @param responsible whether this contact is designated as responsible for paying.
     * @return a new share with trusted contact set and user null.
     */
    public static ItemShare forContact(final TrustedContact contact, final InvoiceItem invoiceItem,
                                       final BigDecimal percentage, final BigDecimal amount,
                                       final boolean responsible) {
        return new ItemShare(contact, invoiceItem, percentage, amount, responsible);
    }

    /**
     * Updates the percentage and recalculates the amount based on the item's total.
     *
     * @param newPercentage the new percentage (0.0 to 1.0). Must be between 0 and 1.
     */
    public void updatePercentage(final BigDecimal newPercentage) {
        Validate.notNull(newPercentage, "New percentage must not be null.");
        Validate.isTrue(newPercentage.compareTo(BigDecimal.ZERO) >= 0, "Percentage must be non-negative.");
        Validate.isTrue(newPercentage.compareTo(BigDecimal.ONE) <= 0, "Percentage cannot exceed 1.0 (100%%).");

        percentage = newPercentage;
        amount = invoiceItem.getAmount().multiply(percentage);
        updatedAt = LocalDateTime.now();
    }

    /**
     * Sets whether this participant is responsible for paying this share.
     *
     * @param responsible true if responsible, false otherwise.
     */
    public void setResponsible(final boolean responsible) {
        this.responsible = responsible;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Marks this share as paid with payment details.
     * Only valid for shares assigned to a system user; contact shares cannot be marked as paid.
     *
     * @param paymentMethod the method used for payment (e.g., "PIX", "Transfer", "Cash").
     * @param paidAt the date and time when the payment was made.
     */
    public void markAsPaid(final String paymentMethod, final LocalDateTime paidAt) {
        Validate.notBlank(paymentMethod, "Payment method cannot be blank.");
        Validate.notNull(paidAt, "Payment date cannot be null.");
        
        this.paid = true;
        this.paymentMethod = paymentMethod;
        this.paidAt = paidAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks this share as unpaid (reverses payment).
     * Only meaningful for shares assigned to a system user.
     */
    public void markAsUnpaid() {
        this.paid = false;
        this.paymentMethod = null;
        this.paidAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if this share has been paid.
     *
     * @return true if the share has been paid, false otherwise.
     */
    public boolean isPaid() {
        return paid;
    }

    /**
     * Gets the payment method used for this share.
     *
     * @return the payment method, or null if not paid.
     */
    public String getPaymentMethod() {
        return paymentMethod;
    }

    /**
     * Gets the date and time when this share was paid.
     *
     * @return the payment date and time, or null if not paid.
     */
    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    /**
     * Sets the invoice item for this share. Used by JPA and InvoiceItem entity.
     *
     * @param invoiceItem the invoice item to set. Can be null.
     */
    void setInvoiceItem(final InvoiceItem invoiceItem) {
        this.invoiceItem = invoiceItem;
    }

    /**
     * Gets the item share's unique identifier.
     *
     * @return the item share's ID. May be null if not persisted.
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the system user this share belongs to.
     *
     * @return the user, or null if the share is assigned to a trusted contact (see {@link #getTrustedContact()}).
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the trusted contact this share belongs to.
     *
     * @return the contact, or null if the share is assigned to a system user (see {@link #getUser()}).
     */
    public TrustedContact getTrustedContact() {
        return trustedContact;
    }

    /**
     * Display name of the participant when the share is for a trusted contact (snapshot at creation time).
     *
     * @return the contact's name, or null when the share is for a system user.
     */
    public String getContactDisplayName() {
        return contactDisplayName;
    }

    /**
     * Display email of the participant when the share is for a trusted contact (snapshot at creation time).
     *
     * @return the contact's email, or null when the share is for a system user.
     */
    public String getContactDisplayEmail() {
        return contactDisplayEmail;
    }

    /**
     * Whether this share is assigned to a trusted contact (and not to a system user).
     *
     * @return true if {@link #getTrustedContact()} is non-null, false if {@link #getUser()} is non-null.
     */
    public boolean isContactShare() {
        return trustedContact != null;
    }

    /**
     * Gets the invoice item this share belongs to.
     *
     * @return the invoice item. Never null.
     */
    public InvoiceItem getInvoiceItem() {
        return invoiceItem;
    }

    /**
     * Gets the fraction of the item amount assigned to this participant.
     *
     * @return the percentage (0.0 to 1.0). Never null. Sum of all shares for the same item is 1.0.
     */
    public BigDecimal getPercentage() {
        return percentage;
    }

    /**
     * Gets the monetary amount this participant is responsible for.
     *
     * @return the amount. Never null, always positive.
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Whether this participant is designated as responsible for paying this share.
     *
     * @return true if responsible, false otherwise.
     */
    public boolean isResponsible() {
        return responsible;
    }

    /**
     * Gets the item share's creation timestamp.
     *
     * @return the creation timestamp. Never null.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the item share's last update timestamp.
     *
     * @return the last update timestamp. Never null.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemShare itemShare)) {
            return false;
        }
        return Objects.equals(id, itemShare.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ItemShare{"
            + "id=" + id
            + ", user=" + (user != null ? user.getId() : null)
            + ", trustedContact=" + (trustedContact != null ? trustedContact.getId() : null)
            + ", percentage=" + percentage
            + ", amount=" + amount
            + ", responsible=" + responsible
            + ", createdAt=" + createdAt
            + ", updatedAt=" + updatedAt
            + '}';
    }
} 