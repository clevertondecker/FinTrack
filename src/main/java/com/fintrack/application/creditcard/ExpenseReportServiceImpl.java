package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.ExpenseReportService;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceCalculationService;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.InvoiceRepository;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.ExpenseDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ExpenseReportService.
 * Provides business logic for generating expense reports grouped by category.
 */
@Service
@Transactional(readOnly = true)
public class ExpenseReportServiceImpl implements ExpenseReportService {

    /** The invoice repository. */
    private final InvoiceRepository invoiceRepository;
    /** The invoice calculation service. */
    private final InvoiceCalculationService invoiceCalculationService;

    /**
     * Constructs a new ExpenseReportServiceImpl.
     *
     * @param invoiceRepository the invoice repository. Must not be null.
     * @param invoiceCalculationService the invoice calculation service. Must not be null.
     */
    public ExpenseReportServiceImpl(
            final InvoiceRepository invoiceRepository,
            final InvoiceCalculationService invoiceCalculationService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceCalculationService = invoiceCalculationService;
    }

    @Override
    public BigDecimal getTotalByCategory(final User user, final YearMonth month, final Category category) {
        Map<Category, BigDecimal> expensesByCategory = getExpensesByCategory(user, month);
        return expensesByCategory.getOrDefault(category, BigDecimal.ZERO);
    }

    /** Category for items without a category. */
    private static final Category UNCATEGORIZED_CATEGORY = Category.of("Sem categoria", "#CCCCCC");

    @Override
    public Map<Category, BigDecimal> getExpensesByCategory(final User user, final YearMonth month) {
        Map<Category, BigDecimal> expensesMap = new HashMap<>();

        // Get all invoices for the month (filtered by invoice month, not due date)
        List<Invoice> invoices = invoiceRepository.findByMonth(month);
        
        // For each invoice, calculate user's share per item
        // Use the same logic as InvoiceCalculationService.calculateUserShare to ensure consistency
        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                // Calculate user's share for this item using the same method as InvoiceCalculationService
                BigDecimal userShareForItem = calculateUserShareForItem(item, user);
                
                if (userShareForItem.compareTo(BigDecimal.ZERO) > 0) {
                    Category category = item.getCategory() != null ? item.getCategory() : UNCATEGORIZED_CATEGORY;
                    expensesMap.merge(category, userShareForItem, BigDecimal::add);
                }
            }
        }

        return expensesMap;
    }

    /**
     * Calculates the amount a specific user is responsible for in a specific invoice item.
     * Uses the exact same logic as InvoiceCalculationService.calculateUserShareForItem to ensure consistency.
     *
     * @param item the invoice item to calculate for. Must not be null.
     * @param user the user to calculate for. Must not be null.
     * @return the amount the user is responsible for. Never null.
     */
    private BigDecimal calculateUserShareForItem(final InvoiceItem item, final User user) {
        // Check if the user has a share for this item
        for (ItemShare share : item.getShares()) {
            if (share.getUser().equals(user)) {
                return share.getAmount();
            }
        }
        
        // If item has no shares, check if user is the card owner
        if (item.getShares().isEmpty()) {
            User cardOwner = item.getInvoice().getCreditCard().getOwner();
            if (cardOwner.equals(user)) {
                return item.getAmount();
            }
            // User is not the card owner and item has no shares, so they owe nothing
            return BigDecimal.ZERO;
        }
        
        // Item is shared but user has no share
        // Only the card owner should receive the unshared amount
        User cardOwner = item.getInvoice().getCreditCard().getOwner();
        if (cardOwner.equals(user)) {
            return item.getUnsharedAmount();
        }
        
        // User is not the card owner and has no share, so they owe nothing
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalExpenses(final User user, final YearMonth month) {
        // Use the same calculation method as InvoiceCalculationService for consistency
        // This ensures the total matches what's shown in the invoice list
        List<Invoice> invoices = invoiceRepository.findByMonth(month);
        BigDecimal total = BigDecimal.ZERO;
        
        for (Invoice invoice : invoices) {
            BigDecimal userShare = invoiceCalculationService.calculateUserShare(invoice, user);
            total = total.add(userShare);
        }
        
        return total;
    }

    /**
     * Gets total expenses by category for all users on cards owned by the given user.
     * Only includes expenses from invoices on credit cards where the user is the owner.
     *
     * @param user the card owner. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @return a map of category to total expense amount. Never null.
     */
    public Map<Category, BigDecimal> getTotalExpensesByCategory(final User user, final YearMonth month) {
        Map<Category, BigDecimal> expensesMap = new HashMap<>();

        List<Invoice> invoices = invoiceRepository.findByMonth(month);
        
        for (Invoice invoice : invoices) {
            // Only include invoices from cards owned by this user
            if (!invoice.getCreditCard().getOwner().equals(user)) {
                continue;
            }
            
            for (InvoiceItem item : invoice.getItems()) {
                Category category = item.getCategory() != null ? item.getCategory() : UNCATEGORIZED_CATEGORY;
                expensesMap.merge(category, item.getAmount(), BigDecimal::add);
            }
        }

        return expensesMap;
    }

    /**
     * Gets grand total expenses for all items on cards owned by the given user.
     *
     * @param user the card owner. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @return the total expense amount. Never null.
     */
    public BigDecimal getGrandTotalExpenses(final User user, final YearMonth month) {
        List<Invoice> invoices = invoiceRepository.findByMonth(month);
        BigDecimal total = BigDecimal.ZERO;
        
        for (Invoice invoice : invoices) {
            // Only include invoices from cards owned by this user
            if (!invoice.getCreditCard().getOwner().equals(user)) {
                continue;
            }
            total = total.add(invoice.getTotalAmount());
        }
        
        return total;
    }

    /**
     * Gets detailed total expense information for all users on a card owned by the given user.
     *
     * @param user the card owner. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @param category the category to filter by. Can be null for uncategorized items.
     * @return a list of detailed expense entries. Never null.
     */
    public List<ExpenseDetailResponse> getTotalExpenseDetails(
            final User user, final YearMonth month, final Category category) {
        List<ExpenseDetailResponse> details = new ArrayList<>();
        Category targetCategory = category != null ? category : UNCATEGORIZED_CATEGORY;

        List<Invoice> invoices = invoiceRepository.findByMonth(month);
        
        for (Invoice invoice : invoices) {
            // Only include invoices from cards owned by this user
            if (!invoice.getCreditCard().getOwner().equals(user)) {
                continue;
            }
            
            for (InvoiceItem item : invoice.getItems()) {
                Category itemCategory = item.getCategory() != null ? item.getCategory() : UNCATEGORIZED_CATEGORY;
                
                if (matchesCategory(itemCategory, targetCategory)) {
                    details.add(new ExpenseDetailResponse(
                        null, // No specific share for total view
                        item.getId(),
                        item.getDescription(),
                        item.getAmount(),
                        item.getPurchaseDate(),
                        item.getInvoice().getId()
                    ));
                }
            }
        }

        return details;
    }

    /**
     * Gets detailed expense information for a specific user, month, and category.
     * Returns both shared amounts (ItemShare) and unshared amounts (for card owner).
     * Uses the same logic as getExpensesByCategory to ensure consistency.
     *
     * @param user the user to get expenses for. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @param category the category to filter by. Can be null for uncategorized items.
     * @return a list of detailed expense entries. Never null.
     */
    public List<ExpenseDetailResponse> getExpenseDetails(
            final User user, final YearMonth month, final Category category) {
        List<ExpenseDetailResponse> details = new ArrayList<>();
        Category targetCategory = category != null ? category : UNCATEGORIZED_CATEGORY;

        // Get all invoices for the month
        List<Invoice> invoices = invoiceRepository.findByMonth(month);
        
        // For each invoice, calculate user's share per item
        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                // Calculate user's share for this item
                BigDecimal userShareForItem = calculateUserShareForItem(item, user);
                
                if (userShareForItem.compareTo(BigDecimal.ZERO) > 0) {
                    Category itemCategory = item.getCategory() != null ? item.getCategory() : UNCATEGORIZED_CATEGORY;
                    
                    // Check if this item matches the target category
                    if (matchesCategory(itemCategory, targetCategory)) {
                        // Find the share ID if this is a shared item
                        Long shareId = findShareIdForUser(item, user);
                        
                        details.add(new ExpenseDetailResponse(
                            shareId, // null if unshared amount
                            item.getId(),
                            item.getDescription(),
                            userShareForItem,
                            item.getPurchaseDate(),
                            item.getInvoice().getId()
                        ));
                    }
                }
            }
        }

        return details;
    }

    /**
     * Finds the share ID for a user in an invoice item, if it exists.
     *
     * @param item the invoice item to search in. Must not be null.
     * @param user the user to find the share for. Must not be null.
     * @return the share ID if found, null otherwise.
     */
    private Long findShareIdForUser(final InvoiceItem item, final User user) {
        for (ItemShare share : item.getShares()) {
            if (share.getUser().equals(user)) {
                return share.getId();
            }
        }
        return null;
    }

    /**
     * Checks if two categories match, handling the uncategorized case.
     *
     * @param category1 the first category. Can be null.
     * @param category2 the second category. Can be null.
     * @return true if the categories match, false otherwise.
     */
    private boolean matchesCategory(final Category category1, final Category category2) {
        if (category1 == null && category2 == null) {
            return true;
        }
        if (category1 == null || category2 == null) {
            // Check if both are uncategorized by name
            if (category1 != null && "Sem categoria".equals(category1.getName())) {
                return true;
            }
            if (category2 != null && "Sem categoria".equals(category2.getName())) {
                return true;
            }
            return false;
        }
        // If both have IDs, compare by ID
        if (category1.getId() != null && category2.getId() != null) {
            return category1.getId().equals(category2.getId());
        }
        // Otherwise compare by name
        return category1.getName().equals(category2.getName());
    }
}

