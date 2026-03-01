package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing a single top expense item.
 *
 * @param rank the rank position (1-based)
 * @param itemId the invoice item's unique identifier
 * @param description the item's description
 * @param amount the expense amount for this item
 * @param purchaseDate the purchase date of the item
 * @param invoiceId the invoice's unique identifier
 * @param category the item's category (null if uncategorized)
 * @param percentageOfTotal the percentage this item represents of the total monthly expenses
 */
public record TopExpenseItemResponse(
    Integer rank,
    Long itemId,
    String description,
    BigDecimal amount,
    LocalDate purchaseDate,
    Long invoiceId,
    CategoryResponse category,
    BigDecimal percentageOfTotal
) {}
