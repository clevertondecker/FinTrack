package com.fintrack.domain.creditcard;

import java.math.BigDecimal;

/**
 * Represents a participant's aggregated share in an invoice.
 * Used internally for calculation results before conversion to DTOs.
 *
 * <p>Participants are grouped by email to unify shares from the same person
 * across different representations (system User vs TrustedContact).</p>
 *
 * @param name the participant's display name. Never null.
 * @param email the participant's email (used as grouping key). Never null.
 * @param totalAmount the total amount the participant owes in the invoice. Never null.
 */
public record ParticipantShare(
    String name,
    String email,
    BigDecimal totalAmount
) {}
