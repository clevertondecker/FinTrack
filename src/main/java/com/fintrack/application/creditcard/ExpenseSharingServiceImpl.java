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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ExpenseSharingService.
 * Provides business logic for sharing invoice items among users.
 */
@Service
@Transactional
public class ExpenseSharingServiceImpl implements ExpenseSharingService {

    private final ItemShareRepository itemShareRepository;
    private final UserRepository userRepository;

    /**
     * Constructs a new ExpenseSharingServiceImpl.
     *
     * @param itemShareRepository the item share repository. Must not be null.
     * @param userRepository the user repository. Must not be null.
     */
    public ExpenseSharingServiceImpl(final ItemShareRepository itemShareRepository,
                                     final UserRepository userRepository) {
        this.itemShareRepository = itemShareRepository;
        this.userRepository = userRepository;
    }

    @Override

    public void shareItem(final InvoiceItem item, final Map<User, BigDecimal> shares) {
        validateShares(shares);
        
        // Remove existing shares
        removeShares(item);
        
        // Create shares with rounding adjustment to ensure a sum equals item amount
        List<ItemShare> shareList = createSharesWithRoundingAdjustment(item, shares, false);
        for (ItemShare share : shareList) {
            item.addShare(share);
            itemShareRepository.save(share);
        }
    }

    @Override
    public void updateShares(final InvoiceItem item, final Map<User, BigDecimal> newShares) {
        validateShares(newShares);
        
        // Remove existing shares
        removeShares(item);
        
        // Create shares with rounding adjustment to ensure a sum equals item amount
        List<ItemShare> shareList = createSharesWithRoundingAdjustment(item, newShares, false);
        for (ItemShare share : shareList) {
            item.addShare(share);
            itemShareRepository.save(share);
        }
    }

    @Override
    public void removeShares(final InvoiceItem item) {
        List<ItemShare> existingShares = itemShareRepository.findByInvoiceItem(item);
        for (ItemShare share : existingShares) {
            item.removeShare(share);
            itemShareRepository.delete(share);
        }
    }

    @Override
    public List<ItemShare> getSharesForUser(final User user, final YearMonth month) {
        return itemShareRepository.findByUserAndMonth(user, month);
    }

    /**
     * Gets all shares for a specific user across all invoices.
     *
     * @param user the user to get shares for. Must not be null.
     * @return a list of item shares for the user. Never null, may be empty.
     */
    public List<ItemShare> getSharesForUser(final User user) {
        return itemShareRepository.findByUser(user);
    }

    @Override
    public List<ItemShare> getSharesForItem(InvoiceItem item) {
        // Use the repository method to ensure shares are loaded from the database
        return itemShareRepository.findByInvoiceItem(item);
    }

    @Override
    public void validateShares(final Map<User, BigDecimal> shares) {
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Shares cannot be null or empty");
        }

        BigDecimal totalPercentage = getTotalPercentage(shares);

        // Allow for small rounding differences (within 0.01)
        BigDecimal difference = totalPercentage.subtract(BigDecimal.ONE).abs();
        if (difference.compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("Sum of share percentages must equal 1.0 (100%). Current sum: " + totalPercentage);
        }

    }

    // Extracted method to calculate total percentage and validate individual percentages.
    private BigDecimal getTotalPercentage(Map<User, BigDecimal> shares) {
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
        return totalPercentage;
    }

    @Override
    public Map<User, BigDecimal> calculateShareAmounts(final InvoiceItem item,
                                                       final Map<User, BigDecimal> shares) {
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
    public List<ItemShare> createSharesFromUserIds(final InvoiceItem item,
                                                   final List<CreateItemShareRequest.UserShare> userShares) {
        if (userShares == null || userShares.isEmpty()) {
            throw new IllegalArgumentException("User shares cannot be null or empty");
        }
        
        // Convert to Map<User, BigDecimal> for validation and build responsibleMap at the same time
        Map<User, BigDecimal> shares = new HashMap<>();
        Map<User, Boolean> responsibleMap = new HashMap<>();
        for (CreateItemShareRequest.UserShare userShare : userShares) {
            User user = userRepository.findById(userShare.userId())
                .orElseThrow(() ->
                    new IllegalArgumentException("User not found with ID: " + userShare.userId()));
            shares.put(user, userShare.percentage());
            responsibleMap.put(user, userShare.responsible());
        }
        
        validateShares(shares);
        
        // Remove existing shares
        removeShares(item);
            
        // Create shares with rounding adjustment to ensure a sum equals item amount
        List<ItemShare> shareList = createSharesWithRoundingAdjustment(item, shares, responsibleMap);
        List<ItemShare> createdShares = new ArrayList<>();
        for (ItemShare share : shareList) {
            item.addShare(share);
            ItemShare savedShare = itemShareRepository.save(share);
            createdShares.add(savedShare);
        }
        
        return createdShares;
    }

    @Override
    public ItemShare markShareAsPaid(final Long shareId, final String paymentMethod,
                                     final LocalDateTime paidAt, final User user) {
        ItemShare share = itemShareRepository.findById(shareId)
            .orElseThrow(() -> new IllegalArgumentException("Share not found with ID: " + shareId));
        
        if (!share.getUser().equals(user)) {
            throw new IllegalArgumentException("Share does not belong to the specified user");
        }
        
        share.markAsPaid(paymentMethod, paidAt);
        return itemShareRepository.save(share);
    }

    @Override
    public ItemShare markShareAsUnpaid(final Long shareId, final User user) {
        ItemShare share = itemShareRepository.findById(shareId)
            .orElseThrow(() -> new IllegalArgumentException("Share not found with ID: " + shareId));
        
        if (!share.getUser().equals(user)) {
            throw new IllegalArgumentException("Share does not belong to the specified user");
        }
        
        share.markAsUnpaid();
        return itemShareRepository.save(share);
    }

    @Override
    public List<ItemShare> markSharesAsPaidBulk(
            final List<Long> shareIds, final String paymentMethod,
            final LocalDateTime paidAt, final User user) {
        List<ItemShare> updated = new ArrayList<>();
        if (shareIds == null || shareIds.isEmpty()) {
            return updated;
        }
        for (ItemShare share : itemShareRepository.findAllById(shareIds)) {
            if (!share.getUser().equals(user)) {
                // Skip shares that don't belong to the user
                continue;
            }
            if (share.isPaid()) {
                // Skip shares already paid to avoid overriding existing payment info
                continue;
            }
            share.markAsPaid(paymentMethod, paidAt);
            updated.add(itemShareRepository.save(share));
        }
        return updated;
    }

    /**
     * Creates shares with rounding adjustment to ensure the sum of all shares
     * equals exactly the item amount, compensating for rounding differences.
     *
     * @param item the invoice item to share. Must not be null.
     * @param shares the map of users to their share percentages. Must not be null.
     * @param defaultResponsible the default responsible flag for shares. Used when responsibleMap is null.
     * @return a list of created item shares with adjusted amounts. Never null.
     */
    private List<ItemShare> createSharesWithRoundingAdjustment(
            final InvoiceItem item,
            final Map<User, BigDecimal> shares,
            final boolean defaultResponsible) {
        return createSharesWithRoundingAdjustment(item, shares, null, defaultResponsible);
    }

    /**
     * Creates shares with rounding adjustment to ensure the sum of all shares
     * equals exactly the item amount, compensating for rounding differences.
     *
     * @param item the invoice item to share. Must not be null.
     * @param shares the map of users to their share percentages. Must not be null.
     * @param responsibleMap the map of users to their responsible flags. Can be null.
     * @return a list of created item shares with adjusted amounts. Never null.
     */
    private List<ItemShare> createSharesWithRoundingAdjustment(
            final InvoiceItem item,
            final Map<User, BigDecimal> shares,
            final Map<User, Boolean> responsibleMap) {
        return createSharesWithRoundingAdjustment(item, shares, responsibleMap, false);
    }

    /**
     * Creates shares with rounding adjustment to ensure the sum of all shares
     * equals exactly the item amount, compensating for rounding differences.
     *
     * @param item the invoice item to share. Must not be null.
     * @param shares the map of users to their share percentages. Must not be null.
     * @param responsibleMap the map of users to their responsible flags. Can be null.
     * @param defaultResponsible the default responsible flag when responsibleMap doesn't contain a user.
     * @return a list of created item shares with adjusted amounts. Never null.
     */
    private List<ItemShare> createSharesWithRoundingAdjustment(
            final InvoiceItem item,
            final Map<User, BigDecimal> shares,
            final Map<User, Boolean> responsibleMap,
            final boolean defaultResponsible) {
        
        List<ItemShare> shareList = new ArrayList<>();
        List<Map.Entry<User, BigDecimal>> entries = new ArrayList<>(shares.entrySet());
        BigDecimal totalCalculated = BigDecimal.ZERO;
        
        // Calculate all shares except the last one
        for (int i = 0; i < entries.size() - 1; i++) {
            Map.Entry<User, BigDecimal> entry = entries.get(i);
            User user = entry.getKey();
            BigDecimal percentage = entry.getValue();
            BigDecimal amount = item.getAmount().multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            
            totalCalculated = totalCalculated.add(amount);
            
            boolean responsible = responsibleMap != null && responsibleMap.containsKey(user)
                ? responsibleMap.get(user)
                : defaultResponsible;
            ItemShare share = ItemShare.of(user, item, percentage, amount, responsible);
            shareList.add(share);
        }
        
        // Handle the last entry - adjust amount to ensure total equals item amount
        if (!entries.isEmpty()) {
            Map.Entry<User, BigDecimal> lastEntry = entries.get(entries.size() - 1);
            User lastUser = lastEntry.getKey();
            BigDecimal lastPercentage = lastEntry.getValue();
            
            // Calculate the last share amount to ensure total equals item amount
            BigDecimal lastAmount = item.getAmount().subtract(totalCalculated).setScale(2, RoundingMode.HALF_UP);
            
            // Ensure the last amount is not negative (should not happen, but safety check)
            if (lastAmount.compareTo(BigDecimal.ZERO) < 0) {
                // If negative, it means we over-calculated. Adjust by reducing from the last calculated share.
                BigDecimal adjustment = lastAmount.abs();
                if (!shareList.isEmpty()) {
                    ItemShare lastCreatedShare = shareList.get(shareList.size() - 1);
                    BigDecimal adjustedAmount = lastCreatedShare.getAmount().subtract(adjustment).setScale(2, RoundingMode.HALF_UP);
                    // Recreate the last share with adjusted amount
                    shareList.remove(shareList.size() - 1);
                    ItemShare adjustedShare = ItemShare.of(
                        lastCreatedShare.getUser(),
                        item,
                        lastCreatedShare.getPercentage(),
                        adjustedAmount,
                        lastCreatedShare.isResponsible()
                    );
                    shareList.add(adjustedShare);
                    lastAmount = BigDecimal.ZERO;
                }
            }
            
            boolean lastResponsible = responsibleMap != null && responsibleMap.containsKey(lastUser)
                ? responsibleMap.get(lastUser)
                : defaultResponsible;
            ItemShare lastShare = ItemShare.of(lastUser, item, lastPercentage, lastAmount, lastResponsible);
            shareList.add(lastShare);
        }
        
        return shareList;
    }

    /**
     * Recalculates all existing shares to fix rounding issues.
     * This method maintains the percentage distribution but adjusts amounts
     * to ensure the sum equals the item amount exactly.
     *
     * @return the number of items that were recalculated.
     */
    public int recalculateAllShares() {
        // Get all shares and group by invoice item
        List<ItemShare> allShares = itemShareRepository.findAll();
        Map<InvoiceItem, List<ItemShare>> sharesByItem = allShares.stream()
                .collect(Collectors.groupingBy(ItemShare::getInvoiceItem));

        int recalculated = 0;

        for (Map.Entry<InvoiceItem, List<ItemShare>> entry : sharesByItem.entrySet()) {
            InvoiceItem item = entry.getKey();
            List<ItemShare> shares = entry.getValue();

            if (shares.isEmpty()) {
                continue;
            }

            // Recalculate amounts with rounding adjustment
            BigDecimal totalCalculated = BigDecimal.ZERO;
            List<ItemShare> sharesList = new ArrayList<>(shares);
            
            // Calculate all shares except the last one
            for (int i = 0; i < sharesList.size() - 1; i++) {
                ItemShare share = sharesList.get(i);
                BigDecimal newAmount = item.getAmount().multiply(share.getPercentage())
                        .setScale(2, RoundingMode.HALF_UP);
                totalCalculated = totalCalculated.add(newAmount);
                
                // Update amount if different
                if (share.getAmount().compareTo(newAmount) != 0) {
                    // Create a new share with updated amount (preserving payment info)
                    ItemShare updatedShare = ItemShare.of(
                        share.getUser(),
                        item,
                        share.getPercentage(),
                        newAmount,
                        share.isResponsible()
                    );
                    // Preserve payment information
                    if (share.isPaid()) {
                        updatedShare.markAsPaid(share.getPaymentMethod(), share.getPaidAt());
                    }
                    // Update in database
                    item.removeShare(share);
                    itemShareRepository.delete(share);
                    item.addShare(updatedShare);
                    itemShareRepository.save(updatedShare);
                    sharesList.set(i, updatedShare);
                }
            }

            // Adjust the last share to ensure total equals item amount
            if (!sharesList.isEmpty()) {
                ItemShare lastShare = sharesList.get(sharesList.size() - 1);
                BigDecimal lastAmount = item.getAmount().subtract(totalCalculated)
                        .setScale(2, RoundingMode.HALF_UP);
                
                // Update last share if amount changed
                if (lastShare.getAmount().compareTo(lastAmount) != 0) {
                    ItemShare updatedLastShare = ItemShare.of(
                        lastShare.getUser(),
                        item,
                        lastShare.getPercentage(),
                        lastAmount,
                        lastShare.isResponsible()
                    );
                    // Preserve payment information
                    if (lastShare.isPaid()) {
                        updatedLastShare.markAsPaid(lastShare.getPaymentMethod(), lastShare.getPaidAt());
                    }
                    // Update in database
                    item.removeShare(lastShare);
                    itemShareRepository.delete(lastShare);
                    item.addShare(updatedLastShare);
                    itemShareRepository.save(updatedLastShare);
                }
            }

            recalculated++;
        }

        return recalculated;
    }
}