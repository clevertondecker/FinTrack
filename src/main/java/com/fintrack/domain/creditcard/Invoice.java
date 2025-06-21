package com.fintrack.domain.creditcard;

import com.fintrack.infrastructure.persistence.converter.YearMonthConverter;
import jakarta.persistence.*;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id", nullable = false)
    private CreditCard creditCard;

    @Column(nullable = false)
    @Convert(converter = YearMonthConverter.class)
    private YearMonth month;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.OPEN;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL,
      orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Protected constructor for JPA only.
     */
    protected Invoice() {}

    /**
     * Private constructor for Invoice. Use the static factory method to create
     * instances.
     *
     * @param theCreditCard the credit card this invoice belongs to. Must not be null.
     * @param theMonth the month/year of the invoice. Must not be null.
     * @param theDueDate the due date of the invoice. Must not be null.
     */
    private Invoice(final CreditCard theCreditCard, final YearMonth theMonth, final LocalDate theDueDate) {
        Validate.notNull(theCreditCard, "Credit card must not be null.");
        Validate.notNull(theMonth, "Month must not be null.");
        Validate.notNull(theDueDate, "Due date must not be null.");

        creditCard = theCreditCard;
        month = theMonth;
        dueDate = theDueDate;
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
     * @param amount the amount being paid. Must be positive.
     */
    public void recordPayment(final BigDecimal amount) {
        Validate.notNull(amount, "Payment amount must not be null.");
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "Payment amount must be positive.");
        Validate.isTrue(amount.compareTo(totalAmount) <= 0, "Payment amount cannot exceed total amount.");

        paidAmount = paidAmount.add(amount);
        updateStatus();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the status based on current payment and due date.
     */
    public void updateStatus() {
        if (paidAmount.compareTo(totalAmount) >= 0) {
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
     * @return the remaining amount.
     */
    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(paidAmount);
    }

    /**
     * Gets the total amount of this invoice.
     *
     * @return the total amount.
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Long getId() { return id; }
    public CreditCard getCreditCard() { return creditCard; }
    public YearMonth getMonth() { return month; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public InvoiceStatus getStatus() { return status; }
    public List<InvoiceItem> getItems() { return new ArrayList<>(items); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice invoice)) return false;
        return Objects.equals(id, invoice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", creditCard=" + creditCard +
                ", month=" + month +
                ", dueDate=" + dueDate +
                ", totalAmount=" + totalAmount +
                ", paidAmount=" + paidAmount +
                ", status=" + status +
                ", items=" + items +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}