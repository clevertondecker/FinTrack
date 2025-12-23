package com.fintrack.domain.creditcard;

import com.fintrack.infrastructure.persistence.converter.YearMonthConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Entity representing a credit card invoice.
 * Contains invoice information and links to its credit card and items.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    /** The invoice's unique identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The credit card this invoice belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id", nullable = false)
    private CreditCard creditCard;

    /** The month/year of the invoice. */
    @Column(name = "invoice_month", nullable = false)
    @Convert(converter = YearMonthConverter.class)
    private YearMonth month;

    /** The due date of the invoice. */
    @Column(nullable = false)
    private LocalDate dueDate;

    /** The total amount of the invoice. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** The amount already paid. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** The current status of the invoice. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.OPEN;

    /** The items in this invoice. */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL,
      orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceItem> items = new ArrayList<>();

    /** The invoice's creation timestamp. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** The invoice's last update timestamp. */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Protected constructor for JPA only.
     */
    protected Invoice() {}

    /**
     * Private constructor for Invoice. Use the static factory method to create instances.
     *
     * @param theCreditCard the credit card this invoice belongs to. Must not be null.
     * @param theMonth the month/year of the invoice. Must not be null.
     * @param theDueDate the due date of the invoice. Must not be null.
     */
    private Invoice(CreditCard theCreditCard, YearMonth theMonth, LocalDate theDueDate) {
        Validate.notNull(theCreditCard, "Credit card must not be null.");
        Validate.notNull(theMonth, "Month must not be null.");
        Validate.notNull(theDueDate, "Due date must not be null.");

        creditCard = theCreditCard;
        month = theMonth;
        dueDate = theDueDate;
        status = InvoiceStatus.OPEN;
        totalAmount = BigDecimal.ZERO;
        paidAmount = BigDecimal.ZERO;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Static factory method to create a new Invoice instance.
     *
     * @param creditCard the credit card this invoice belongs to. Cannot be null.
     * @param month the month/year of the invoice. Cannot be null.
     * @param dueDate the due date of the invoice. Cannot be null.
     * @return a validated Invoice entity. Never null.
     */
    public static Invoice of(final CreditCard creditCard, final YearMonth month, final LocalDate dueDate) {
        return new Invoice(creditCard, month, dueDate);
    }

    /**
     * Adds an item to this invoice.
     *
     * @param item the item to add. Must not be null.
     */
    public void addItem(final InvoiceItem item) {
        Validate.notNull(item, "Item must not be null.");

        items.add(item);
        item.setInvoice(this);
        recalculateTotal();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Removes an item from this invoice.
     *
     * @param item the item to remove. Must not be null.
     */
    public void removeItem(final InvoiceItem item) {
        Validate.notNull(item, "Item must not be null.");

        items.remove(item);
        item.setInvoice(null);
        recalculateTotal();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Records a payment for this invoice.
     *
     * @param amount the amount being paid. Must be positive and not exceed the total amount.
     */
    public void recordPayment(final BigDecimal amount) {
        Validate.notNull(amount, "Payment amount must not be null.");
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "Payment amount must be positive.");
        // Allow payments even when totalAmount is zero (for closed invoices)
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            Validate.isTrue(amount.compareTo(totalAmount) <= 0, "Payment amount cannot exceed total amount.");
        }

        paidAmount = paidAmount.add(amount);
        updateStatus();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the status based on current payment and due date.
     */
    public void updateStatus() {
        // If there's no amount to pay and past due date, consider it CLOSED
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0 && LocalDate.now().isAfter(dueDate)) {
            status = InvoiceStatus.CLOSED;
        } else if (totalAmount.compareTo(BigDecimal.ZERO) > 0 && paidAmount.compareTo(totalAmount) >= 0) {
            status = InvoiceStatus.PAID;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            status = InvoiceStatus.PARTIAL;
        } else if (LocalDate.now().isAfter(dueDate)) {
            status = InvoiceStatus.OVERDUE;
        } else {
            status = InvoiceStatus.OPEN;
        }
    }

    /**
     * Recalculates the total amount based on all items.
     */
    private void recalculateTotal() {
        totalAmount = items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        updateStatus();
    }

    /**
     * Gets the remaining amount to be paid.
     *
     * @return the remaining amount. Never null may be zero.
     */
    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(paidAmount);
    }

    /**
     * Gets the total amount of this invoice.
     *
     * @return the total amount. Never null may be zero.
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * Gets the invoice's unique identifier.
     *
     * @return the invoice's ID. May be null if not persisted.
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the credit card this invoice belongs to.
     *
     * @return the credit card. Never null.
     */
    public CreditCard getCreditCard() {
        return creditCard;
    }

    /**
     * Gets the month/year of this invoice.
     *
     * @return the month/year. Never null.
     */
    public YearMonth getMonth() {
        return month;
    }

    /**
     * Gets the due date of this invoice.
     *
     * @return the due date. Never null.
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Gets the amount already paid for this invoice.
     *
     * @return the paid amount. Never null may be zero.
     */
    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    /**
     * Gets the current status of this invoice.
     *
     * @return the invoice status. Never null.
     */
    public InvoiceStatus getStatus() {
        return status;
    }

    /**
     * Calculates the current status dynamically based on real-time data.
     * Use this method when you need the most up-to-date status.
     *
     * @return the calculated invoice status. Never null.
     */
    public InvoiceStatus calculateCurrentStatus() {
        // If there's no amount to pay and past due date, consider it CLOSED
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0
            && LocalDate.now().isAfter(dueDate)) {
            return InvoiceStatus.CLOSED;
        } else if (totalAmount.compareTo(BigDecimal.ZERO) > 0
            && paidAmount.compareTo(totalAmount) >= 0) {
            // Only consider PAID if there is a total amount and it is fully paid
            return InvoiceStatus.PAID;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return InvoiceStatus.PARTIAL;
        } else if (LocalDate.now().isAfter(dueDate)) {
            return InvoiceStatus.OVERDUE;
        } else {
            return InvoiceStatus.OPEN;
        }
    }

    /**
     * Gets all items in this invoice.
     *
     * @return a defensive copy of the item list. Never null, may be empty.
     */
    public List<InvoiceItem> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Gets the invoice's creation timestamp.
     *
     * @return the creation timestamp. Never null.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the invoice's last update timestamp.
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
        if (!(o instanceof Invoice invoice)) {
            return false;
        }
        return Objects.equals(id, invoice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Invoice{"
            + "id=" + id
            + ", creditCard=" + creditCard
            + ", month=" + month
            + ", dueDate=" + dueDate
            + ", totalAmount=" + totalAmount
            + ", paidAmount=" + paidAmount
            + ", status=" + status
            + ", items=" + items
            + ", createdAt=" + createdAt
            + ", updatedAt=" + updatedAt
            + '}';
    }
}