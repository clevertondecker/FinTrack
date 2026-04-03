package com.fintrack.application.dashboard;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.ExpenseReportService;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceCalculationService;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.InvoiceRepository;
import com.fintrack.domain.creditcard.InvoiceStatus;
import com.fintrack.domain.user.User;
import com.fintrack.dto.dashboard.CategoryRankingResponse;
import com.fintrack.dto.dashboard.CreditCardOverviewResponse;
import com.fintrack.dto.dashboard.DailyExpenseResponse;
import com.fintrack.dto.dashboard.DashboardOverviewResponse;
import com.fintrack.dto.user.UserResponse;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Category UNCATEGORIZED =
            Category.of("Sem categoria", "#CCCCCC");

    private final CreditCardJpaRepository creditCardRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseReportService expenseReportService;
    private final InvoiceCalculationService invoiceCalculationService;

    public DashboardServiceImpl(
            final CreditCardJpaRepository creditCardRepository,
            final InvoiceRepository invoiceRepository,
            final ExpenseReportService expenseReportService,
            final InvoiceCalculationService invoiceCalculationService) {
        this.creditCardRepository = creditCardRepository;
        this.invoiceRepository = invoiceRepository;
        this.expenseReportService = expenseReportService;
        this.invoiceCalculationService = invoiceCalculationService;
    }

    @Override
    public DashboardOverviewResponse getOverview(final User user, final YearMonth month) {
        List<CreditCard> activeCards =
                creditCardRepository.findByOwnerOrParentCardOwnerAndActiveTrue(user);
        List<Invoice> invoices = invoiceRepository.findByMonthAndCreditCardOwner(month, user);

        List<CreditCardOverviewResponse> cardOverviews =
                buildCardOverviews(activeCards, invoices, month, user);

        BigDecimal totalUserExpenses = expenseReportService.getTotalExpenses(user, month);
        BigDecimal totalGross = expenseReportService.getGrandTotalExpenses(user, month);

        Map<Category, CategoryAggregation> categoryMap = new HashMap<>();
        int totalTransactions = 0;

        for (Invoice invoice : invoices) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal userShare = invoiceCalculationService.calculateUserShareForItem(item, user);
                if (userShare.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                totalTransactions++;
                Category cat = item.getCategory() != null ? item.getCategory() : UNCATEGORIZED;
                categoryMap.computeIfAbsent(cat, k -> new CategoryAggregation())
                        .add(userShare);
            }
        }

        Map<LocalDate, BigDecimal> dailyMap = buildDailyExpenseMap(user, month);

        List<CategoryRankingResponse> ranking =
                buildCategoryRanking(categoryMap, totalUserExpenses);

        List<DailyExpenseResponse> dailyExpenses = buildDailyExpenses(dailyMap, month);

        return new DashboardOverviewResponse(
                toUserResponse(user),
                month,
                totalUserExpenses,
                totalGross,
                totalTransactions,
                cardOverviews,
                ranking,
                dailyExpenses
        );
    }

    private List<CreditCardOverviewResponse> buildCardOverviews(
            final List<CreditCard> cards,
            final List<Invoice> allInvoices,
            final YearMonth month,
            final User user) {

        YearMonth nextMonth = month.plusMonths(1);
        List<Invoice> nextMonthInvoices = invoiceRepository.findByMonthAndCreditCardOwner(nextMonth, user);

        List<CreditCardOverviewResponse> result = new ArrayList<>();
        for (CreditCard card : cards) {
            Invoice currentInvoice = allInvoices.stream()
                    .filter(inv -> inv.getCreditCard().getId().equals(card.getId()))
                    .findFirst()
                    .orElse(null);

            Invoice nextInvoice = nextMonthInvoices.stream()
                    .filter(inv -> inv.getCreditCard().getId().equals(card.getId()))
                    .findFirst()
                    .orElse(null);

            result.add(new CreditCardOverviewResponse(
                    card.getId(),
                    card.getName(),
                    card.getLastFourDigits(),
                    card.getBank().getName(),
                    card.getBank().getCode(),
                    currentInvoice != null ? currentInvoice.getDueDate() : null,
                    currentInvoice != null ? currentInvoice.getTotalAmount() : BigDecimal.ZERO,
                    nextInvoice != null ? nextInvoice.getTotalAmount() : BigDecimal.ZERO,
                    currentInvoice != null
                            ? currentInvoice.calculateCurrentStatus()
                            : InvoiceStatus.OPEN
            ));
        }

        return result;
    }

    private List<CategoryRankingResponse> buildCategoryRanking(
            final Map<Category, CategoryAggregation> categoryMap,
            final BigDecimal totalExpenses) {

        return categoryMap.entrySet().stream()
                .sorted(Comparator.comparing(
                        (Map.Entry<Category, CategoryAggregation> e) -> e.getValue().getTotal())
                        .reversed())
                .map(entry -> {
                    Category cat = entry.getKey();
                    CategoryAggregation agg = entry.getValue();
                    BigDecimal pct = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                            ? agg.getTotal().multiply(BigDecimal.valueOf(100))
                                    .divide(totalExpenses, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new CategoryRankingResponse(
                            cat.getId(),
                            cat.getName(),
                            cat.getColor(),
                            agg.getTotal(),
                            pct,
                            agg.getCount()
                    );
                })
                .toList();
    }

    private Map<LocalDate, BigDecimal> buildDailyExpenseMap(final User user, final YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<Invoice> relevantInvoices =
                invoiceRepository.findByMonthBetweenAndCreditCardOwner(month, month.plusMonths(1), user);

        Map<LocalDate, BigDecimal> dailyMap = new TreeMap<>();
        for (Invoice invoice : relevantInvoices) {
            for (InvoiceItem item : invoice.getItems()) {
                LocalDate pd = item.getPurchaseDate();
                if (pd == null || pd.isBefore(monthStart) || pd.isAfter(monthEnd)) {
                    continue;
                }
                BigDecimal userShare = invoiceCalculationService.calculateUserShareForItem(item, user);
                if (userShare.compareTo(BigDecimal.ZERO) > 0) {
                    dailyMap.merge(pd, userShare, BigDecimal::add);
                }
            }
        }
        return dailyMap;
    }

    private List<DailyExpenseResponse> buildDailyExpenses(
            final Map<LocalDate, BigDecimal> dailyMap,
            final YearMonth month) {

        List<DailyExpenseResponse> result = new ArrayList<>();
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDate limit = end.isBefore(today) ? end : today;

        for (LocalDate date = start; !date.isAfter(limit); date = date.plusDays(1)) {
            result.add(new DailyExpenseResponse(
                    date,
                    dailyMap.getOrDefault(date, BigDecimal.ZERO)
            ));
        }

        return result;
    }

    private UserResponse toUserResponse(final User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail().getEmail(),
                user.getRoles().stream().map(Enum::name).toArray(String[]::new),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private static class CategoryAggregation {
        private BigDecimal total = BigDecimal.ZERO;
        private int count = 0;

        void add(final BigDecimal amount) {
            total = total.add(amount);
            count++;
        }

        BigDecimal getTotal() {
            return total;
        }

        int getCount() {
            return count;
        }
    }
}
