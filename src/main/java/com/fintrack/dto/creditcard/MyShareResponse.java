package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for user's shares across different invoices.
 */
public record MyShareResponse(
    Long shareId,
    Long invoiceId,
    Long itemId,
    String itemDescription,
    BigDecimal itemAmount,
    BigDecimal myAmount,
    BigDecimal myPercentage,
    Boolean isResponsible,
    Boolean isPaid,
    String paymentMethod,
    LocalDateTime paidAt,
    String creditCardName,
    String creditCardOwnerName,
    LocalDate invoiceDueDate,
    String invoiceStatus,
    LocalDateTime shareCreatedAt
) {} 