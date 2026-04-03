package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal userShareForItem = invoiceCalculationService.calculateUserShareForItem(item, user);
                if (userShareForItem.compareTo(BigDecimal.ZERO) > 0) {
                    expensesMap.merge(resolveCategory(item), userShareForItem, BigDecimal::add);
                }
            }
        }

        return expensesMap;
    }

    @Override
    public BigDecimal getTotalExpenses(final User user, final YearMonth month) {
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);
        BigDecimal total = BigDecimal.ZERO;
        
        for (Invoice invoice : invoices) {
            BigDecimal userShare = invoiceCalculationService.calculateUserShare(invoice, user);
            total = total.add(userShare);
        }
        
        return total;
    }

    @Override
    public Map<Category, BigDecimal> getTotalExpensesByCategory(final User user, final YearMonth month) {
        Map<Category, BigDecimal> expensesMap = new HashMap<>();
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                expensesMap.merge(resolveCategory(item), item.getAmount(), BigDecimal::add);
            }
        }

        return expensesMap;
    }

    @Override
    public BigDecimal getGrandTotalExpenses(final User user, final YearMonth month) {
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);
        BigDecimal total = BigDecimal.ZERO;

        for (Invoice invoice : invoices) {
            total = total.add(invoice.getTotalAmount());
        }

        return total;
    }

    @Override
    public List<ExpenseDetailResponse> getTotalExpenseDetails(
            final User user, final YearMonth month, final Category category) {
        List<ExpenseDetailResponse> details = new ArrayList<>();
        Category targetCategory = category != null ? category : UNCATEGORIZED_CATEGORY;

        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                if (matchesCategory(resolveCategory(item), targetCategory)) {
                    details.add(new ExpenseDetailResponse(
                        null, item.getId(), item.getDescription(),
                        item.getAmount(), item.getPurchaseDate(), item.getInvoice().getId()
                    ));
                }
            }
        }

        return details;
    }

    @Override
    public List<ExpenseDetailResponse> getExpenseDetails(
            final User user, final YearMonth month, final Category category) {
        List<ExpenseDetailResponse> details = new ArrayList<>();
        Category targetCategory = category != null ? category : UNCATEGORIZED_CATEGORY;
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal userShareForItem = invoiceCalculationService.calculateUserShareForItem(item, user);

                if (userShareForItem.compareTo(BigDecimal.ZERO) > 0
                        && matchesCategory(resolveCategory(item), targetCategory)) {
                    Long shareId = findShareIdForUser(item, user);
                    details.add(new ExpenseDetailResponse(
                        shareId, item.getId(), item.getDescription(),
                        userShareForItem, item.getPurchaseDate(), item.getInvoice().getId()
                    ));
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
            if (user.equals(share.getUser())) {
                return share.getId();
            }
        }
        return null;
    }

    @Override
    public Map<YearMonth, Map<Category, BigDecimal>> getExpensesByMonthAndCategory(
            final User user, final YearMonth from, final YearMonth to) {
        Map<YearMonth, Map<Category, BigDecimal>> result = initializeMonthRange(from, to);
        List<Invoice> invoices = invoiceRepository.findByMonthBetweenAndCreditCardOwner(from, to, user);

        for (Invoice invoice : invoices) {
            Map<Category, BigDecimal> monthMap = result.get(invoice.getMonth());
            if (monthMap == null) {
                continue;
            }
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal userShare = invoiceCalculationService.calculateUserShareForItem(item, user);
                if (userShare.compareTo(BigDecimal.ZERO) > 0) {
                    Category category = resolveCategory(item);
                    monthMap.merge(category, userShare, BigDecimal::add);
                }
            }
        }

        return result;
    }

    @Override
    public Map<YearMonth, Map<Category, BigDecimal>> getTotalExpensesByMonthAndCategory(
            final User user, final YearMonth from, final YearMonth to) {
        Map<YearMonth, Map<Category, BigDecimal>> result = initializeMonthRange(from, to);
        List<Invoice> invoices = invoiceRepository.findByMonthBetweenAndCreditCardOwner(from, to, user);

        for (Invoice invoice : invoices) {
            Map<Category, BigDecimal> monthMap = result.get(invoice.getMonth());
            if (monthMap == null) {
                continue;
            }
            for (InvoiceItem item : invoice.getItems()) {
                monthMap.merge(resolveCategory(item), item.getAmount(), BigDecimal::add);
            }
        }

        return result;
    }

    @Override
    public List<TopExpenseEntry> getTopExpenses(final User user, final YearMonth month, final int limit) {
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);
        List<TopExpenseEntry> entries = new ArrayList<>();

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal userShare = invoiceCalculationService.calculateUserShareForItem(item, user);
                if (userShare.compareTo(BigDecimal.ZERO) > 0) {
                    entries.add(toTopExpenseEntry(item, invoice.getId(), userShare));
                }
            }
        }

        return sortAndLimit(entries, limit);
    }

    @Override
    public List<TopExpenseEntry> getTotalTopExpenses(
            final User user, final YearMonth month, final int limit) {
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);
        List<TopExpenseEntry> entries = new ArrayList<>();

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                entries.add(toTopExpenseEntry(item, invoice.getId(), item.getAmount()));
            }
        }

        return sortAndLimit(entries, limit);
    }

    /**
     * Creates a LinkedHashMap with empty maps for each month in the range.
     */
    private Map<YearMonth, Map<Category, BigDecimal>> initializeMonthRange(
            final YearMonth from, final YearMonth to) {
        Map<YearMonth, Map<Category, BigDecimal>> result = new LinkedHashMap<>();
        YearMonth current = from;
        while (!current.isAfter(to)) {
            result.put(current, new HashMap<>());
            current = current.plusMonths(1);
        }
        return result;
    }

    /**
     * Resolves the category for an item, using UNCATEGORIZED_CATEGORY if null.
     */
    private Category resolveCategory(final InvoiceItem item) {
        return item.getCategory() != null ? item.getCategory() : UNCATEGORIZED_CATEGORY;
    }

    /**
     * Creates a TopExpenseEntry from an invoice item and a calculated amount.
     */
    private TopExpenseEntry toTopExpenseEntry(
            final InvoiceItem item, final Long invoiceId, final BigDecimal amount) {
        return new TopExpenseEntry(
            item.getId(), item.getDescription(), amount,
            item.getPurchaseDate(), invoiceId, item.getCategory()
        );
    }

    /**
     * Sorts entries by amount descending and returns the top N.
     */
    private List<TopExpenseEntry> sortAndLimit(final List<TopExpenseEntry> entries, final int limit) {
        entries.sort(Comparator.comparing(TopExpenseEntry::amount).reversed());
        return entries.stream().limit(limit).toList();
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

    @Override
    public List<CardExpenseEntry> getExpensesByCard(final User user, final YearMonth month) {
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);
        Map<Long, CardAggregation> cardMap = new HashMap<>();

        for (Invoice invoice : invoices) {
            CreditCard card = invoice.getCreditCard();
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal userShare = invoiceCalculationService.calculateUserShareForItem(item, user);
                if (userShare.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                CardAggregation agg = cardMap.computeIfAbsent(
                        card.getId(), k -> new CardAggregation(card));
                agg.add(userShare, resolveCategory(item));
            }
        }

        return cardMap.values().stream()
                .sorted(Comparator.comparing(CardAggregation::getTotal).reversed())
                .map(agg -> new CardExpenseEntry(
                        agg.getCard().getId(),
                        agg.getCard().getName(),
                        agg.getCard().getLastFourDigits(),
                        agg.getCard().getBank().getName(),
                        agg.getTotal(),
                        agg.getCount(),
                        agg.getCategoryBreakdown()
                ))
                .toList();
    }

    @Override
    public List<RecurrenceExpenseEntry> getExpensesByRecurrence(
            final User user, final YearMonth month) {
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);
        BigDecimal installmentTotal = BigDecimal.ZERO;
        BigDecimal singleTotal = BigDecimal.ZERO;
        int installmentCount = 0;
        int singleCount = 0;

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal userShare = invoiceCalculationService.calculateUserShareForItem(item, user);
                if (userShare.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (item.getTotalInstallments() > 1) {
                    installmentTotal = installmentTotal.add(userShare);
                    installmentCount++;
                } else {
                    singleTotal = singleTotal.add(userShare);
                    singleCount++;
                }
            }
        }

        List<RecurrenceExpenseEntry> entries = new ArrayList<>();
        if (singleCount > 0) {
            entries.add(new RecurrenceExpenseEntry("SINGLE", singleTotal, singleCount));
        }
        if (installmentCount > 0) {
            entries.add(new RecurrenceExpenseEntry(
                    "INSTALLMENT", installmentTotal, installmentCount));
        }
        entries.sort(Comparator.comparing(RecurrenceExpenseEntry::amount).reversed());
        return entries;
    }

    private static final class CardAggregation {
        private final CreditCard card;
        private BigDecimal total = BigDecimal.ZERO;
        private int count;
        private final Map<Category, BigDecimal> categoryBreakdown = new HashMap<>();

        CardAggregation(final CreditCard card) {
            this.card = card;
        }

        void add(final BigDecimal amount, final Category category) {
            total = total.add(amount);
            count++;
            categoryBreakdown.merge(category, amount, BigDecimal::add);
        }

        CreditCard getCard() {
            return card;
        }

        BigDecimal getTotal() {
            return total;
        }

        int getCount() {
            return count;
        }

        Map<Category, BigDecimal> getCategoryBreakdown() {
            return categoryBreakdown;
        }
    }
}

