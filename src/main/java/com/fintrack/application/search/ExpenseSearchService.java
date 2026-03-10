package com.fintrack.application.search;

import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.dto.search.ExpenseSearchRequest;
import com.fintrack.dto.search.ExpenseSearchResponse;
import com.fintrack.dto.search.ExpenseSearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ExpenseSearchService {

    private final EntityManager entityManager;

    public ExpenseSearchService(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ExpenseSearchResponse search(final User user, final ExpenseSearchRequest request) {
        Validate.notNull(user, "User must not be null");
        Validate.notNull(request, "Search request must not be null");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        Tuple countAndSum = executeCountAndSum(cb, user, request);
        long totalCount = countAndSum.get(0, Long.class);

        int totalPages = request.size() > 0
                ? (int) Math.ceil((double) totalCount / request.size())
                : 0;

        List<ExpenseSearchResult> results = totalCount > 0
                ? executeSearch(cb, user, request)
                : List.of();

        BigDecimal totalAmount = countAndSum.get(1, BigDecimal.class);

        return new ExpenseSearchResponse(
                results,
                (int) totalCount,
                request.page(),
                totalPages,
                totalAmount != null ? totalAmount : BigDecimal.ZERO
        );
    }

    private List<ExpenseSearchResult> executeSearch(
            final CriteriaBuilder cb, final User user, final ExpenseSearchRequest request) {

        CriteriaQuery<InvoiceItem> cq = cb.createQuery(InvoiceItem.class);
        Root<InvoiceItem> item = cq.from(InvoiceItem.class);

        Join<?, ?> invoiceJoin = (Join<?, ?>) item.fetch("invoice");
        Join<?, ?> cardJoin = (Join<?, ?>) invoiceJoin.fetch("creditCard");
        item.fetch("category", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, item, invoiceJoin, cardJoin, user, request);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(item.get("purchaseDate")), cb.desc(item.get("id")));

        TypedQuery<InvoiceItem> query = entityManager.createQuery(cq);
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());

        return query.getResultList().stream()
                .map(ExpenseSearchService::toResult)
                .toList();
    }

    private Tuple executeCountAndSum(
            final CriteriaBuilder cb, final User user, final ExpenseSearchRequest request) {

        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<InvoiceItem> item = cq.from(InvoiceItem.class);

        cq.multiselect(
                cb.count(item),
                cb.coalesce(cb.sum(item.get("amount")), BigDecimal.ZERO)
        );

        Join<?, ?> invoiceJoin = item.join("invoice");
        Join<?, ?> cardJoin = invoiceJoin.join("creditCard");

        List<Predicate> predicates = buildPredicates(cb, item, invoiceJoin, cardJoin, user, request);
        cq.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getSingleResult();
    }

    private List<Predicate> buildPredicates(
            final CriteriaBuilder cb, final Root<InvoiceItem> item,
            final Join<?, ?> invoice, final Join<?, ?> creditCard,
            final User user, final ExpenseSearchRequest request) {

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(creditCard.get("owner"), user));

        if (request.query() != null && !request.query().isBlank()) {
            String searchTerm = "%" + request.query().toLowerCase().trim() + "%";
            predicates.add(cb.like(cb.lower(item.get("description")), searchTerm));
        }

        if (request.categoryId() != null) {
            predicates.add(cb.equal(item.get("category").get("id"), request.categoryId()));
        }

        if (request.cardId() != null) {
            predicates.add(cb.equal(creditCard.get("id"), request.cardId()));
        }

        if (request.dateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(item.get("purchaseDate"), request.dateFrom()));
        }

        if (request.dateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(item.get("purchaseDate"), request.dateTo()));
        }

        if (request.amountMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(item.get("amount"), request.amountMin()));
        }

        if (request.amountMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(item.get("amount"), request.amountMax()));
        }

        return predicates;
    }

    private static ExpenseSearchResult toResult(final InvoiceItem item) {
        return new ExpenseSearchResult(
                item.getId(),
                item.getInvoice().getId(),
                item.getDescription(),
                item.getAmount(),
                item.getPurchaseDate(),
                CategoryResponse.from(item.getCategory()),
                item.getInvoice().getCreditCard().getName(),
                item.getInvoice().getCreditCard().getLastFourDigits(),
                item.getInvoice().getMonth(),
                item.getInstallments(),
                item.getTotalInstallments()
        );
    }
}
