package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Entity representing a share of an invoice item among users.
 * Contains information about how an item is shared between users.
 */
@Entity
@Table(name = "item_shares")
public class ItemShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_item_id", nullable = false)
    private InvoiceItem invoiceItem;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal percentage;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean responsible = false;

    /**
     * Protected constructor for JPA only.
     */
    protected ItemShare() {}

    /**
     * Private constructor for ItemShare. Use the static factory method to create
     * instances.
     *
     * @param theUser the user this share belongs to. Must not be null.
     * @param theInvoiceItem the invoice item this share belongs to. Must not be null.
     * @param thePercentage the percentage of the item (0.0 to 1.0). Must be between 0 and 1.
     * @param theAmount the amount this user is responsible for. Must be positive.
     * @param theResponsible whether this user is responsible for paying. Default false.
     */
    private ItemShare(final User theUser, final InvoiceItem theInvoiceItem,
                     final BigDecimal thePercentage, final BigDecimal theAmount, final boolean theResponsible) {
        Validate.notNull(theUser, "User must not be null.");
        Validate.notNull(theInvoiceItem, "Invoice item must not be null.");
        Validate.notNull(thePercentage, "Percentage must not be null.");
        Validate.isTrue(thePercentage.compareTo(BigDecimal.ZERO) >= 0, "Percentage must be non-negative.");
        Validate.isTrue(thePercentage.compareTo(BigDecimal.ONE) <= 0, "Percentage cannot exceed 1.0 (100%).");
        Validate.notNull(theAmount, "Amount must not be null.");
        Validate.isTrue(theAmount.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive.");

        user = theUser;
        invoiceItem = theInvoiceItem;
        percentage = thePercentage;
        amount = theAmount;
        responsible = theResponsible;
    }

    /**
     * Static factory method to create a new ItemShare instance.
     *
     * @param user the user this share belongs to. Cannot be null.
     * @param invoiceItem the invoice item this share belongs to. Cannot be null.
     * @param percentage the percentage of the item (0.0 to 1.0). Must be between 0 and 1.
     * @param amount the amount this user is responsible for. Must be positive.
     * @param responsible whether this user is responsible for paying.
     * @return a validated ItemShare entity. Never null.
     */
    public static ItemShare of(final User user, final InvoiceItem invoiceItem,
                              final BigDecimal percentage, final BigDecimal amount, final boolean responsible) {
        return new ItemShare(user, invoiceItem, percentage, amount, responsible);
    }

    /**
     * Static factory method to create a new ItemShare instance where the user is not responsible.
     *
     * @param user the user this share belongs to. Cannot be null.
     * @param invoiceItem the invoice item this share belongs to. Cannot be null.
     * @param percentage the percentage of the item (0.0 to 1.0). Must be between 0 and 1.
     * @param amount the amount this user is responsible for. Must be positive.
     * @return a validated ItemShare entity. Never null.
     */
    public static ItemShare of(final User user, final InvoiceItem invoiceItem,
                              final BigDecimal percentage, final BigDecimal amount) {
        return new ItemShare(user, invoiceItem, percentage, amount, false);
    }

    /**
     * Updates the percentage and recalculates the amount based on the item's total.
     *
     * @param newPercentage the new percentage (0.0 to 1.0). Must be between 0 and 1.
     */
    public void updatePercentage(final BigDecimal newPercentage) {
        Validate.notNull(newPercentage, "New percentage must not be null.");
        Validate.isTrue(newPercentage.compareTo(BigDecimal.ZERO) >= 0, "Percentage must be non-negative.");
        Validate.isTrue(newPercentage.compareTo(BigDecimal.ONE) <= 0, "Percentage cannot exceed 1.0 (100%).");

        percentage = newPercentage;
        amount = invoiceItem.getAmount().multiply(percentage);
    }

    /**
     * Sets whether this user is responsible for paying this share.
     *
     * @param responsible true if the user is responsible, false otherwise.
     */
    public void setResponsible(final boolean responsible) {
        this.responsible = responsible;
    }

    /**
     * Sets the invoice item for this share. Used by JPA and InvoiceItem entity.
     *
     * @param invoiceItem the invoice item to set.
     */
    void setInvoiceItem(final InvoiceItem invoiceItem) {
        this.invoiceItem = invoiceItem;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public InvoiceItem getInvoiceItem() { return invoiceItem; }
    public BigDecimal getPercentage() { return percentage; }
    public BigDecimal getAmount() { return amount; }
    public boolean isResponsible() { return responsible; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemShare itemShare)) return false;
        return Objects.equals(id, itemShare.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ItemShare{" +
                "id=" + id +
                ", user=" + user +
                ", invoiceItem=" + invoiceItem +
                ", percentage=" + percentage +
                ", amount=" + amount +
                ", responsible=" + responsible +
                '}';
    }
} 