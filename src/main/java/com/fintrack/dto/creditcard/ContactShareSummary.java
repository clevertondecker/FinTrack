package com.fintrack.dto.creditcard;

import java.math.BigDecimal;

/**
 * Summary of a participant's total share in an invoice.
 * Represents how much a specific person (trusted contact or other user) owes.
 *
 * @param contactName the participant's display name. Never null.
 * @param contactEmail the participant's email address. Never null.
 * @param totalAmount the total amount the participant owes in this invoice. Never null.
 */
public record ContactShareSummary(
    String contactName,
    String contactEmail,
    BigDecimal totalAmount
) {}
