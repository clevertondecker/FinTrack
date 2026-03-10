package com.fintrack.domain.budget;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.persistence.converter.YearMonthConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "budget_month")
    @Convert(converter = YearMonthConverter.class)
    private YearMonth month;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Budget() { }

    private Budget(final User theOwner, final Category theCategory,
                   final BigDecimal theLimit, final YearMonth theMonth) {
        Validate.notNull(theOwner, "Owner must not be null.");
        Validate.notNull(theLimit, "Limit amount must not be null.");
        Validate.isTrue(theLimit.compareTo(BigDecimal.ZERO) > 0,
                "Limit amount must be positive.");

        this.owner = theOwner;
        this.category = theCategory;
        this.limitAmount = theLimit;
        this.month = theMonth;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Budget of(final User owner, final Category category,
                             final BigDecimal limitAmount, final YearMonth month) {
        return new Budget(owner, category, limitAmount, month);
    }

    public static Budget recurring(final User owner, final Category category,
                                    final BigDecimal limitAmount) {
        return new Budget(owner, category, limitAmount, null);
    }

    public void updateLimit(final BigDecimal newLimit) {
        Validate.notNull(newLimit, "Limit must not be null.");
        Validate.isTrue(newLimit.compareTo(BigDecimal.ZERO) > 0,
                "Limit must be positive.");
        this.limitAmount = newLimit;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRecurring() {
        return month == null;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public YearMonth getMonth() {
        return month;
    }

    public boolean isActive() {
        return active;
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
        if (!(o instanceof Budget budget)) {
            return false;
        }
        return Objects.equals(id, budget.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
