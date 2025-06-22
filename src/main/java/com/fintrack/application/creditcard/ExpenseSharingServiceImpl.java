package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.ExpenseSharingService;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.ItemShareRepository;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.dto.creditcard.CreateItemShareRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ExpenseSharingService.
 * Provides business logic for sharing invoice items among users.
 */
@Service
@Transactional
public class ExpenseSharingServiceImpl implements ExpenseSharingService {

    private final ItemShareRepository itemShareRepository;
    private final UserRepository userRepository;

    public ExpenseSharingServiceImpl(ItemShareRepository itemShareRepository, UserRepository userRepository) {
        this.itemShareRepository = itemShareRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void shareItem(InvoiceItem item, Map<User, BigDecimal> shares) {
        validateShares(shares);
        
        // Remove existing shares
        removeShares(item);
        
        // Create new shares
        for (Map.Entry<User, BigDecimal> entry : shares.entrySet()) {
            User user = entry.getKey();
            BigDecimal percentage = entry.getValue();
            BigDecimal amount = item.getAmount().multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            
            ItemShare share = ItemShare.of(user, item, percentage, amount, false);
            item.addShare(share);
            itemShareRepository.save(share);
        }
    }

    @Override
    public void updateShares(InvoiceItem item, Map<User, BigDecimal> newShares) {
        validateShares(newShares);
        
        // Remove existing shares
        removeShares(item);
        
        // Create new shares
        for (Map.Entry<User, BigDecimal> entry : newShares.entrySet()) {
            User user = entry.getKey();
            BigDecimal percentage = entry.getValue();
            BigDecimal amount = item.getAmount().multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            
            ItemShare share = ItemShare.of(user, item, percentage, amount, false);
            item.addShare(share);
            itemShareRepository.save(share);
        }
    }

    @Override
    public void removeShares(InvoiceItem item) {
        List<ItemShare> existingShares = itemShareRepository.findByInvoiceItem(item);
        for (ItemShare share : existingShares) {
            item.removeShare(share);
            itemShareRepository.delete(share);
        }
    }

    @Override
    public List<ItemShare> getSharesForUser(User user, YearMonth month) {
        return itemShareRepository.findByUserAndMonth(user, month);
    }

    @Override
    public List<ItemShare> getSharesForItem(InvoiceItem item) {
        // Use the repository method to ensure shares are loaded from the database
        return itemShareRepository.findByInvoiceItem(item);
    }

    @Override
    public boolean validateShares(Map<User, BigDecimal> shares) {
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Shares cannot be null or empty");
        }
        
        BigDecimal totalPercentage = BigDecimal.ZERO;
        for (BigDecimal percentage : shares.values()) {
            if (percentage == null) {
                throw new IllegalArgumentException("Share percentage cannot be null");
            }
            if (percentage.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Share percentage cannot be negative");
            }
            if (percentage.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Share percentage cannot exceed 1.0 (100%)");
            }
            totalPercentage = totalPercentage.add(percentage);
        }
        
        // Allow for small rounding differences (within 0.01)
        BigDecimal difference = totalPercentage.subtract(BigDecimal.ONE).abs();
        if (difference.compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("Sum of share percentages must equal 1.0 (100%). Current sum: " + totalPercentage);
        }
        
        return true;
    }

    @Override
    public Map<User, BigDecimal> calculateShareAmounts(InvoiceItem item, Map<User, BigDecimal> shares) {
        validateShares(shares);
        
        Map<User, BigDecimal> amounts = new HashMap<>();
        for (Map.Entry<User, BigDecimal> entry : shares.entrySet()) {
            User user = entry.getKey();
            BigDecimal percentage = entry.getValue();
            BigDecimal amount = item.getAmount().multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            amounts.put(user, amount);
        }
        
        return amounts;
    }

    /**
     * Creates shares for an item based on user IDs and percentages.
     *
     * @param item the invoice item to share. Must not be null.
     * @param userShares a list of user share information. Must not be null.
     * @return a list of created item shares. Never null.
     */
    public List<ItemShare> createSharesFromUserIds(InvoiceItem item, List<CreateItemShareRequest.UserShare> userShares) {
        if (userShares == null || userShares.isEmpty()) {
            throw new IllegalArgumentException("User shares cannot be null or empty");
        }
        
        // Convert to Map<User, BigDecimal> for validation
        Map<User, BigDecimal> shares = new HashMap<>();
        for (CreateItemShareRequest.UserShare userShare : userShares) {
            User user = userRepository.findById(userShare.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userShare.userId()));
            shares.put(user, userShare.percentage());
        }
        
        validateShares(shares);
        
        // Remove existing shares
        removeShares(item);
        
        // Create new shares
        List<ItemShare> createdShares = new ArrayList<>();
        for (CreateItemShareRequest.UserShare userShare : userShares) {
            User user = userRepository.findById(userShare.userId()).get();
            BigDecimal percentage = userShare.percentage();
            BigDecimal amount = item.getAmount().multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            
            ItemShare share = ItemShare.of(user, item, percentage, amount, userShare.responsible());
            item.addShare(share);
            ItemShare savedShare = itemShareRepository.save(share);
            createdShares.add(savedShare);
        }
        
        return createdShares;
    }
} 