package com.fintrack.domain.creditcard;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Entity representing an item in a credit card invoice.
 * Contains item information and links to its invoice and shares.
 */
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false)
    private Integer installments = 1;

    @Column(nullable = false)
    private Integer totalInstallments = 1;

    @OneToMany(mappedBy = "invoiceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemShare> shares = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Protected constructor for JPA only.
     */
    protected InvoiceItem() {}

    /**
     * Private constructor for InvoiceItem. Use the static factory method to create
     * instances.
     *
     * @param theInvoice the invoice this item belongs to. Must not be null.
     * @param theDescription the item's description. Must not be null or blank.
     * @param theAmount the item's amount. Must be positive.
     * @param theCategory the item's category. Can be null.
     * @param thePurchaseDate the date of purchase. Must not be null.
     * @param theInstallments the current installment number. Must be positive.
     * @param theTotalInstallments the total number of installments. Must be positive.
     */
    private InvoiceItem(final Invoice theInvoice, final String theDescription, final BigDecimal theAmount,
                       final Category theCategory, final LocalDate thePurchaseDate,
                       final Integer theInstallments, final Integer theTotalInstallments) {
        Validate.notNull(theInvoice, "Invoice must not be null.");
        Validate.notBlank(theDescription, "Description must not be null or blank.");
        Validate.notNull(theAmount, "Amount must not be null.");
        Validate.isTrue(theAmount.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive.");
        Validate.notNull(thePurchaseDate, "Purchase date must not be null.");
        Validate.notNull(theInstallments, "Installments must not be null.");
        Validate.isTrue(theInstallments > 0, "Installments must be positive.");
        Validate.notNull(theTotalInstallments, "Total installments must not be null.");
        Validate.isTrue(theTotalInstallments > 0, "Total installments must be positive.");
        Validate.isTrue(theInstallments <= theTotalInstallments, "Current installment cannot exceed total installments.");

        invoice = theInvoice;
        description = theDescription;
        amount = theAmount;
        category = theCategory;
        purchaseDate = thePurchaseDate;
        installments = theInstallments;
        totalInstallments = theTotalInstallments;
        createdAt = LocalDateTime.now();
    }

    /**
     * Static factory method to create a new InvoiceItem instance.
     *
     * @param invoice the invoice this item belongs to. Cannot be null.
     * @param description the item's description. Cannot be null or blank.
     * @param amount the item's amount. Must be positive.
     * @param category the item's category. Can be null.
     * @param purchaseDate the date of purchase. Cannot be null.
     * @param installments the current installment number. Must be positive.
     * @param totalInstallments the total number of installments. Must be positive.
     * @return a validated InvoiceItem entity. Never null.
     */
    public static InvoiceItem of(final Invoice invoice, final String description, final BigDecimal amount,
                                final Category category, final LocalDate purchaseDate,
                                final Integer installments, final Integer totalInstallments) {
        return new InvoiceItem(invoice, description, amount, category, purchaseDate, installments, totalInstallments);
    }

    /**
     * Static factory method to create a new InvoiceItem instance for a single payment.
     *
     * @param invoice the invoice this item belongs to. Cannot be null.
     * @param description the item's description. Cannot be null or blank.
     * @param amount the item's amount. Must be positive.
     * @param category the item's category. Can be null.
     * @param purchaseDate the date of purchase. Cannot be null.
     * @return a validated InvoiceItem entity. Never null.
     */
    public static InvoiceItem of(final Invoice invoice, final String description, final BigDecimal amount,
                                final Category category, final LocalDate purchaseDate) {
        return new InvoiceItem(invoice, description, amount, category, purchaseDate, 1, 1);
    }

    /**
     * Adds a share to this item.
     *
     * @param share the share to add. Must not be null.
     */
    public void addShare(final ItemShare share) {
        Validate.notNull(share, "Share must not be null.");
        
        shares.add(share);
        share.setInvoiceItem(this);
    }

    /**
     * Removes a share from this item.
     *
     * @param share the share to remove. Must not be null.
     */
    public void removeShare(final ItemShare share) {
        Validate.notNull(share, "Share must not be null.");
        
        shares.remove(share);
        share.setInvoiceItem(null);
    }

    /**
     * Gets the total amount shared among users.
     *
     * @return the total shared amount.
     */
    public BigDecimal getSharedAmount() {
        return shares.stream()
                .map(ItemShare::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Checks if this item is fully shared.
     *
     * @return true if the item is fully shared, false otherwise.
     */
    public boolean isFullyShared() {
        return getSharedAmount().compareTo(amount) >= 0;
    }

    /**
     * Gets the amount that is not shared.
     *
     * @return the unshared amount.
     */
    public BigDecimal getUnsharedAmount() {
        return amount.subtract(getSharedAmount());
    }

    /**
     * Sets the invoice for this item. Used by JPA and Invoice entity.
     *
     * @param invoice the invoice to set.
     */
    void setInvoice(final Invoice invoice) {
        this.invoice = invoice;
    }

    /**
     * Sets the invoice item for a share. Used by JPA and ItemShare entity.
     *
     * @param invoiceItem the invoice item to set.
     */
    void setInvoiceItem(final InvoiceItem invoiceItem) {
        // This method is used by ItemShare to set the back reference
    }

    public Long getId() { return id; }
    public Invoice getInvoice() { return invoice; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public Category getCategory() { return category; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public Integer getInstallments() { return installments; }
    public Integer getTotalInstallments() { return totalInstallments; }
    public List<ItemShare> getShares() { return new ArrayList<>(shares); }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceItem invoiceItem)) return false;
        return Objects.equals(id, invoiceItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "id=" + id +
                ", invoice=" + invoice +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", category=" + category +
                ", purchaseDate=" + purchaseDate +
                ", installments=" + installments +
                ", totalInstallments=" + totalInstallments +
                ", shares=" + shares +
                ", createdAt=" + createdAt +
                '}';
    }
} 