package com.fintrack.application.subscription;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.subscription.Subscription;
import com.fintrack.domain.subscription.SubscriptionRepository;
import com.fintrack.domain.subscription.SubscriptionStatus;
import com.fintrack.domain.user.User;
import com.fintrack.dto.subscription.CreateSubscriptionRequest;
import com.fintrack.dto.subscription.SubscriptionResponse;
import com.fintrack.dto.subscription.SubscriptionSuggestion;
import com.fintrack.dto.subscription.UpdateSubscriptionRequest;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionService {

    private static final int MIN_OCCURRENCES_FOR_SUGGESTION = 2;
    private static final int MONTHS_TO_ANALYZE = 6;

    private final SubscriptionRepository subscriptionRepository;
    private final CategoryJpaRepository categoryRepository;
    private final CreditCardJpaRepository creditCardRepository;
    private final EntityManager entityManager;

    public SubscriptionService(final SubscriptionRepository subscriptionRepository,
                               final CategoryJpaRepository categoryRepository,
                               final CreditCardJpaRepository creditCardRepository,
                               final EntityManager entityManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.categoryRepository = categoryRepository;
        this.creditCardRepository = creditCardRepository;
        this.entityManager = entityManager;
    }

    public SubscriptionResponse create(final User user, final CreateSubscriptionRequest request) {
        if (subscriptionRepository.existsByOwnerAndMerchantKeyAndActiveTrue(user, request.merchantKey())) {
            throw new IllegalArgumentException("Subscription with this merchant already exists");
        }

        Subscription sub = Subscription.manual(
                user, request.name(), request.merchantKey(),
                request.expectedAmount(), request.billingCycle()
        );

        if (request.categoryId() != null) {
            Category cat = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            sub.assignCategory(cat);
        }

        if (request.creditCardId() != null) {
            CreditCard card = creditCardRepository.findByIdAndOwner(request.creditCardId(), user)
                    .orElseThrow(() -> new EntityNotFoundException("Credit card not found"));
            sub.assignCreditCard(card);
        }

        return SubscriptionResponse.from(subscriptionRepository.save(sub), "ACTIVE");
    }

    public SubscriptionResponse update(final User user, final Long id, final UpdateSubscriptionRequest request) {
        Subscription sub = subscriptionRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));

        sub.updateDetails(request.name(), request.expectedAmount(), request.billingCycle());

        if (request.categoryId() != null) {
            Category cat = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            sub.assignCategory(cat);
        } else {
            sub.assignCategory(null);
        }

        if (request.creditCardId() != null) {
            CreditCard card = creditCardRepository.findByIdAndOwner(request.creditCardId(), user)
                    .orElseThrow(() -> new EntityNotFoundException("Credit card not found"));
            sub.assignCreditCard(card);
        } else {
            sub.assignCreditCard(null);
        }

        return SubscriptionResponse.from(subscriptionRepository.save(sub), "ACTIVE");
    }

    public void cancel(final User user, final Long id) {
        Subscription sub = subscriptionRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        sub.cancel();
        subscriptionRepository.save(sub);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptions(final User user, final YearMonth month) {
        List<Subscription> subscriptions = subscriptionRepository.findByOwnerAndActiveTrue(user);

        Map<String, List<InvoiceItem>> itemsByMerchant = getItemsByMerchantForMonth(user, month);

        return subscriptions.stream()
                .map(sub -> {
                    String monthStatus = resolveMonthStatus(sub, itemsByMerchant);
                    return SubscriptionResponse.from(sub, monthStatus);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<SubscriptionSuggestion> getSuggestions(final User user) {
        YearMonth now = YearMonth.now();
        YearMonth from = now.minusMonths(MONTHS_TO_ANALYZE);

        Set<String> existingKeys = subscriptionRepository.findByOwnerAndActiveTrue(user).stream()
                .map(Subscription::getMerchantKey)
                .collect(Collectors.toSet());

        List<Tuple> candidates = entityManager.createQuery(
                "SELECT ii.merchantKey AS mk, "
                + "COUNT(DISTINCT i.month) AS monthCount, "
                + "AVG(ii.amount) AS avgAmount, "
                + "MIN(ii.purchaseDate) AS firstSeen, "
                + "MAX(ii.purchaseDate) AS lastSeen "
                + "FROM InvoiceItem ii "
                + "JOIN ii.invoice i "
                + "JOIN i.creditCard cc "
                + "WHERE cc.owner = :owner "
                + "AND i.month >= :fromMonth "
                + "AND ii.merchantKey IS NOT NULL "
                + "AND ii.merchantKey <> '' "
                + "AND ii.totalInstallments = 1 "
                + "GROUP BY ii.merchantKey "
                + "HAVING COUNT(DISTINCT i.month) >= :minOccurrences "
                + "ORDER BY COUNT(DISTINCT i.month) DESC, AVG(ii.amount) DESC",
                Tuple.class)
                .setParameter("owner", user)
                .setParameter("fromMonth", from)
                .setParameter("minOccurrences", (long) MIN_OCCURRENCES_FOR_SUGGESTION)
                .getResultList();

        List<SubscriptionSuggestion> suggestions = new ArrayList<>();
        for (Tuple row : candidates) {
            String merchantKey = row.get("mk", String.class);
            if (existingKeys.contains(merchantKey)) {
                continue;
            }

            Long monthCount = row.get("monthCount", Long.class);
            Double avgAmount = row.get("avgAmount", Double.class);
            LocalDate firstSeen = row.get("firstSeen", LocalDate.class);
            LocalDate lastSeen = row.get("lastSeen", LocalDate.class);

            MerchantDetails details = getMerchantDetails(user, merchantKey);

            suggestions.add(new SubscriptionSuggestion(
                    merchantKey,
                    details.displayName(),
                    BigDecimal.valueOf(avgAmount).setScale(2, RoundingMode.HALF_UP),
                    monthCount.intValue(),
                    firstSeen,
                    lastSeen,
                    details.categoryName(),
                    details.categoryColor(),
                    details.cardName()
            ));
        }

        return suggestions;
    }

    public SubscriptionResponse confirmSuggestion(final User user, final String merchantKey) {
        if (subscriptionRepository.existsByOwnerAndMerchantKeyAndActiveTrue(user, merchantKey)) {
            throw new IllegalArgumentException("Subscription already exists for this merchant");
        }

        MerchantDetails details = getMerchantDetails(user, merchantKey);

        YearMonth now = YearMonth.now();
        YearMonth from = now.minusMonths(MONTHS_TO_ANALYZE);

        @SuppressWarnings("unchecked")
        List<Tuple> stats = entityManager.createQuery(
                "SELECT AVG(ii.amount) AS avgAmount, "
                + "MIN(ii.purchaseDate) AS firstSeen, "
                + "MAX(ii.purchaseDate) AS lastSeen "
                + "FROM InvoiceItem ii "
                + "JOIN ii.invoice i "
                + "JOIN i.creditCard cc "
                + "WHERE cc.owner = :owner "
                + "AND i.month >= :fromMonth "
                + "AND ii.merchantKey = :merchantKey",
                Tuple.class)
                .setParameter("owner", user)
                .setParameter("fromMonth", from)
                .setParameter("merchantKey", merchantKey)
                .getResultList();

        Tuple stat = stats.get(0);
        Double avgAmount = stat.get("avgAmount", Double.class);
        LocalDate firstSeen = stat.get("firstSeen", LocalDate.class);

        BigDecimal amount = avgAmount != null
                ? BigDecimal.valueOf(avgAmount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        Subscription sub = Subscription.autoDetected(
                user, details.displayName(), merchantKey,
                amount, firstSeen != null ? firstSeen : LocalDate.now()
        );

        if (details.categoryId() != null) {
            categoryRepository.findById(details.categoryId()).ifPresent(sub::assignCategory);
        }

        if (details.cardId() != null) {
            creditCardRepository.findByIdAndOwner(details.cardId(), user).ifPresent(sub::assignCreditCard);
        }

        return SubscriptionResponse.from(subscriptionRepository.save(sub), "ACTIVE");
    }

    private String resolveMonthStatus(final Subscription sub, final Map<String, List<InvoiceItem>> itemsByMerchant) {
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (sub.getStatus() == SubscriptionStatus.PAUSED) {
            return "PAUSED";
        }

        List<InvoiceItem> matches = itemsByMerchant.get(sub.getMerchantKey());
        if (matches == null || matches.isEmpty()) {
            return "MISSED";
        }

        BigDecimal detectedAmount = matches.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (detectedAmount.compareTo(sub.getExpectedAmount()) != 0) {
            return "PRICE_CHANGED";
        }

        return "DETECTED";
    }

    private Map<String, List<InvoiceItem>> getItemsByMerchantForMonth(final User user, final YearMonth month) {
        @SuppressWarnings("unchecked")
        List<InvoiceItem> items = entityManager.createQuery(
                "SELECT ii FROM InvoiceItem ii "
                + "JOIN ii.invoice i "
                + "JOIN i.creditCard cc "
                + "WHERE cc.owner = :owner "
                + "AND i.month = :month "
                + "AND ii.merchantKey IS NOT NULL "
                + "AND ii.merchantKey <> ''")
                .setParameter("owner", user)
                .setParameter("month", month)
                .getResultList();

        return items.stream()
                .collect(Collectors.groupingBy(InvoiceItem::getMerchantKey));
    }

    @SuppressWarnings("unchecked")
    private MerchantDetails getMerchantDetails(final User user, final String merchantKey) {
        List<Object[]> rows = entityManager.createQuery(
                "SELECT ii.description, c.name, c.color, c.id, cc.name, cc.id "
                + "FROM InvoiceItem ii "
                + "JOIN ii.invoice i "
                + "JOIN i.creditCard cc "
                + "LEFT JOIN ii.category c "
                + "WHERE cc.owner = :owner "
                + "AND ii.merchantKey = :merchantKey "
                + "ORDER BY ii.purchaseDate DESC")
                .setParameter("owner", user)
                .setParameter("merchantKey", merchantKey)
                .setMaxResults(1)
                .getResultList();

        if (rows.isEmpty()) {
            return new MerchantDetails(merchantKey, null, null, null, null, null);
        }

        Object[] row = rows.get(0);
        return new MerchantDetails(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (Long) row[3],
                (String) row[4],
                (Long) row[5]
        );
    }

    private record MerchantDetails(
        String displayName,
        String categoryName,
        String categoryColor,
        Long categoryId,
        String cardName,
        Long cardId
    ) {}
}
