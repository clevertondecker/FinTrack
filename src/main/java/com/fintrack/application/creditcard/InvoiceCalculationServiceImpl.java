package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.InvoiceCalculationService;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of InvoiceCalculationService.
 * Provides business logic for calculating invoice amounts and user shares.
 */
@Service
@Transactional
public class InvoiceCalculationServiceImpl implements InvoiceCalculationService {

    private final ExpenseSharingServiceImpl expenseSharingService;

    public InvoiceCalculationServiceImpl(ExpenseSharingServiceImpl expenseSharingService) {
        this.expenseSharingService = expenseSharingService;
    }

    @Override
    public BigDecimal calculateUserShare(Invoice invoice, User user) {
        BigDecimal userShare = BigDecimal.ZERO;
        
        // Calculate user's share for each item in the invoice
        for (InvoiceItem item : invoice.getItems()) {
            userShare = userShare.add(calculateUserShareForItem(item, user));
        }
        
        return userShare;
    }

    @Override
    public BigDecimal calculateTotalForUser(User user, YearMonth month) {
        List<ItemShare> shares = expenseSharingService.getSharesForUser(user, month);
        
        return shares.stream()
                .map(ItemShare::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<User, BigDecimal> calculateSharesForItem(InvoiceItem item) {
        Map<User, BigDecimal> shares = new HashMap<>();
        
        for (ItemShare share : item.getShares()) {
            shares.put(share.getUser(), share.getAmount());
        }
        
        return shares;
    }

    @Override
    public BigDecimal calculateTotalSharedAmount(Invoice invoice) {
        return invoice.getItems().stream()
                .map(InvoiceItem::getSharedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateUnsharedAmount(Invoice invoice) {
        return invoice.getItems().stream()
                .map(InvoiceItem::getUnsharedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateSharedPercentage(Invoice invoice) {
        BigDecimal totalAmount = invoice.getTotalAmount();
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sharedAmount = calculateTotalSharedAmount(invoice);
        return sharedAmount.divide(totalAmount, 4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Calculates the amount a specific user is responsible for in a specific invoice item.
     *
     * @param item the invoice item to calculate for. Must not be null.
     * @param user the user to calculate for. Must not be null.
     * @return the amount the user is responsible for. Never null.
     */
    private BigDecimal calculateUserShareForItem(InvoiceItem item, User user) {
        // Check if the user has a share for this item
        for (ItemShare share : item.getShares()) {
            if (share.getUser().equals(user)) {
                return share.getAmount();
            }
        }
        
        // If user has no share, they are responsible for the unshared amount
        // (assuming they are the card owner)
        if (item.getShares().isEmpty()) {
            return item.getAmount();
        }
        
        // If item is shared but user has no share, they are responsible for the unshared amount
        return item.getUnsharedAmount();
    }
} 