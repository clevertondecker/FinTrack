package com.fintrack.domain.creditcard;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

    /** The invoice item's unique identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The invoice this item belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** The item's description. */
    @Column(nullable = false)
    private String description;

    /** The item's amount. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** The item's category. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** The purchase date. */
    @Column(nullable = false)
    private LocalDate purchaseDate;

    /** The current installment number. */
    @Column(nullable = false)
    private Integer installments = 1;

    /** The total number of installments. */
    @Column(nullable = false)
    private Integer totalInstallments = 1;

    /** The shares for this item. */
    @OneToMany(mappedBy = "invoiceItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemShare> shares = new ArrayList<>();

    /** The invoice item's creation timestamp. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** The normalized merchant key extracted from the description. */
    @Column(name = "merchant_key", length = 255)
    private String merchantKey;

    /** How the category was assigned to this item. */
    @Enumerated(EnumType.STRING)
    @Column(name = "categorization_source", length = 20)
    private CategorizationSource categorizationSource;

    /** The rule that was used to categorize this item (for auditing). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_rule_id")
    private MerchantCategoryRule appliedRule;

    /**
     * Protected constructor for JPA only.
     */
    protected InvoiceItem() {}

    /**
     * Private constructor for InvoiceItem. Use the static factory method to create instances.
     *
     * @param theInvoice the invoice this item belongs to. Must not be null.
     * @param theDescription the item's description. Must not be null or blank.
     * @param theAmount the item's amount. Must be zero or positive.
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
        Validate.notNull(thePurchaseDate, "Purchase date must not be null.");
        Validate.notNull(theInstallments, "Installments must not be null.");
        Validate.isTrue(theInstallments > 0, "Installments must be positive.");
        Validate.notNull(theTotalInstallments, "Total installments must not be null.");
        Validate.isTrue(theTotalInstallments > 0, "Total installments must be positive.");
        Validate.isTrue(theInstallments <= theTotalInstallments,
            "Current installment cannot exceed total installments.");

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
     * @param amount the item's amount. Must be zero or positive.
     * @param category the item's category. Can be null.
     * @param purchaseDate the date of purchase. Cannot be null.
     * @param installments the current installment number. Must be positive.
     * @param totalInstallments the total number of installments. Must be positive.
     * @return a validated InvoiceItem entity. Never null.
     */
    public static InvoiceItem of(
            final Invoice invoice, final String description, final BigDecimal amount,
            final Category category, final LocalDate purchaseDate,
            final Integer installments, final Integer totalInstallments) {
        return new InvoiceItem(
            invoice, description, amount, category, purchaseDate, installments, totalInstallments);
    }

    /**
     * Static factory method to create a new InvoiceItem instance for a single payment.
     *
     * @param invoice the invoice this item belongs to. Cannot be null.
     * @param description the item's description. Cannot be null or blank.
     * @param amount the item's amount. Must be zero or positive.
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
     * @return the total shared amount. Never null, may be zero.
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
     * @return the unshared amount. Never null, may be zero.
     */
    public BigDecimal getUnsharedAmount() {
        return amount.subtract(getSharedAmount());
    }

    /**
     * Sets the invoice for this item. Used by JPA and Invoice entity.
     *
     * @param invoice the invoice to set. Can be null.
     */
    void setInvoice(final Invoice invoice) {
        this.invoice = invoice;
    }

    /**
     * Updates the category of this invoice item.
     * This is typically used after import to categorize items that were imported without a category.
     *
     * @param newCategory the new category to assign. Can be null to remove category.
     */
    public void updateCategory(final Category newCategory) {
        this.category = newCategory;
        this.categorizationSource = CategorizationSource.MANUAL;
        this.appliedRule = null;
    }

    /**
     * Updates the category of this invoice item with full categorization metadata.
     *
     * @param newCategory the new category to assign. Can be null to remove category.
     * @param source the source of the categorization. Can be null.
     * @param rule the rule that was applied. Can be null.
     */
    public void updateCategory(
            final Category newCategory,
            final CategorizationSource source,
            final MerchantCategoryRule rule) {
        this.category = newCategory;
        this.categorizationSource = source;
        this.appliedRule = rule;
    }

    /**
     * Sets the normalized merchant key for this item.
     *
     * @param key the normalized merchant key. Can be null.
     */
    public void setMerchantKey(final String key) {
        this.merchantKey = key;
    }

    /**
     * Gets the invoice item's unique identifier.
     *
     * @return the invoice item's ID. May be null if not persisted.
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the invoice this item belongs to.
     *
     * @return the invoice. Never null.
     */
    public Invoice getInvoice() {
        return invoice;
    }

    /**
     * Gets the item's description.
     *
     * @return the description. Never null or blank.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the item's amount.
     *
     * @return the amount. Never null, always positive.
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Gets the item's category.
     *
     * @return the category. May be null.
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Gets the purchase date.
     *
     * @return the purchase date. Never null.
     */
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    /**
     * Gets the current installment number.
     *
     * @return the current installment. Never null, always positive.
     */
    public Integer getInstallments() {
        return installments;
    }

    /**
     * Gets the total number of installments.
     *
     * @return the total installments. Never null, always positive.
     */
    public Integer getTotalInstallments() {
        return totalInstallments;
    }

    /**
     * Gets all shares for this item.
     *
     * @return a defensive copy of the shares list. Never null, may be empty.
     */
    public List<ItemShare> getShares() { 
        // Return a defensive copy of the shares list
        // Note: This relies on the shares being properly loaded by the repository
        return new ArrayList<>(shares); 
    }

    /**
     * Gets the invoice item's creation timestamp.
     *
     * @return the creation timestamp. Never null.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the normalized merchant key.
     *
     * @return the merchant key. May be null.
     */
    public String getMerchantKey() {
        return merchantKey;
    }

    /**
     * Gets the categorization source.
     *
     * @return the categorization source. May be null if not categorized.
     */
    public CategorizationSource getCategorizationSource() {
        return categorizationSource;
    }

    /**
     * Gets the rule that was applied to categorize this item.
     *
     * @return the applied rule. May be null.
     */
    public MerchantCategoryRule getAppliedRule() {
        return appliedRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InvoiceItem invoiceItem)) {
            return false;
        }
        return Objects.equals(id, invoiceItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "InvoiceItem{"
            + "id=" + id
            + ", invoice=" + invoice
            + ", description='" + description + '\''
            + ", amount=" + amount
            + ", category=" + category
            + ", purchaseDate=" + purchaseDate
            + ", installments=" + installments
            + ", totalInstallments=" + totalInstallments
            + ", shares=" + shares
            + ", createdAt=" + createdAt
            + '}';
    }
} 