package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Domain service for calculating invoice amounts and user shares.
 * Provides business logic for invoice calculations and expense sharing.
 */
public interface InvoiceCalculationService {

    /**
     * Calculates the total amount a user is responsible for in a specific invoice.
     *
     * @param invoice the invoice to calculate for. Must not be null.
     * @param user the user to calculate for. Must not be null.
     * @return the total amount the user is responsible for. Never null.
     */
    BigDecimal calculateUserShare(Invoice invoice, User user);

    /**
     * Calculates the total amount a user is responsible for across all invoices in a month.
     *
     * @param user the user to calculate for. Must not be null.
     * @param month the month to calculate for. Must not be null.
     * @return the total amount the user is responsible for in the month. Never null.
     */
    BigDecimal calculateTotalForUser(User user, YearMonth month);

    /**
     * Calculates the shares for each user for a specific invoice item.
     *
     * @param item the invoice item to calculate shares for. Must not be null.
     * @return a map of users to their share amounts. Never null, may be empty.
     */
    Map<User, BigDecimal> calculateSharesForItem(InvoiceItem item);

    /**
     * Calculates the total amount shared among users for a specific invoice.
     *
     * @param invoice the invoice to calculate for. Must not be null.
     * @return the total amount shared among users. Never null.
     */
    BigDecimal calculateTotalSharedAmount(Invoice invoice);

    /**
     * Calculates the amount that is not shared (belongs to the card owner) for a specific invoice.
     *
     * @param invoice the invoice to calculate for. Must not be null.
     * @return the amount not shared. Never null.
     */
    BigDecimal calculateUnsharedAmount(Invoice invoice);

    /**
     * Calculates the percentage of an invoice that is shared among users.
     *
     * @param invoice the invoice to calculate for. Must not be null.
     * @return the percentage shared (0.0 to 1.0). Never null.
     */
    BigDecimal calculateSharedPercentage(Invoice invoice);

    /**
     * Calculates the total amount each other participant owes in a specific invoice.
     * Includes both trusted contacts (owned by the given user) and other system users.
     * Excludes the card owner (whose share is already captured by calculateUserShare).
     * Participants are grouped by email to unify the same person across User and
     * TrustedContact records.
     *
     * @param invoice the invoice to calculate for. Must not be null.
     * @param owner the card owner to exclude. Must not be null.
     * @return shares per participant, grouped by email. Never null, may be empty.
     */
    List<ParticipantShare> calculateOtherParticipantShares(Invoice invoice, User owner);
} 