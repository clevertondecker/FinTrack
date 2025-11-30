package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing a detailed expense entry in a category report.
 *
 * @param shareId the item share's unique identifier (null if unshared item)
 * @param itemId the invoice item's unique identifier
 * @param itemDescription the item's description
 * @param amount the amount for this expense entry
 * @param purchaseDate the purchase date of the item
 * @param invoiceId the invoice's unique identifier
 */
public record ExpenseDetailResponse(
    Long shareId,
    Long itemId,
    String itemDescription,
    BigDecimal amount,
    LocalDate purchaseDate,
    Long invoiceId
) {}

