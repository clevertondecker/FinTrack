package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.ItemShareRepository;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.user.User;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ItemShareRepository using JPA.
 * Provides data access operations for item share entities.
 */
@Repository
public class ItemShareRepositoryImpl implements ItemShareRepository {

    private final ItemShareJpaRepository jpaRepository;

    public ItemShareRepositoryImpl(ItemShareJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ItemShare save(ItemShare itemShare) {
        return jpaRepository.save(itemShare);
    }

    @Override
    public List<ItemShare> findByInvoiceItem(InvoiceItem invoiceItem) {
        return jpaRepository.findByInvoiceItem(invoiceItem);
    }

    @Override
    public List<ItemShare> findByUser(User user) {
        return jpaRepository.findByUser(user);
    }

    @Override
    public List<ItemShare> findByUserAndMonth(User user, YearMonth month) {
        return jpaRepository.findByUserAndMonth(user, month.getYear(), month.getMonthValue());
    }

    @Override
    public Optional<ItemShare> findByUserAndInvoiceItem(User user, InvoiceItem invoiceItem) {
        return jpaRepository.findByUserAndInvoiceItem(user, invoiceItem);
    }

    @Override
    public List<ItemShare> findByUserAndResponsibleTrue(User user) {
        return jpaRepository.findByUserAndResponsibleTrue(user);
    }

    @Override
    public long countByInvoiceItem(InvoiceItem invoiceItem) {
        return jpaRepository.countByInvoiceItem(invoiceItem);
    }

    @Override
    public void deleteByInvoiceItem(InvoiceItem invoiceItem) {
        jpaRepository.deleteByInvoiceItem(invoiceItem);
    }

    @Override
    public List<ItemShare> findUnpaidSharesByUser(User user) {
        return jpaRepository.findUnpaidSharesByUser(user);
    }
} 