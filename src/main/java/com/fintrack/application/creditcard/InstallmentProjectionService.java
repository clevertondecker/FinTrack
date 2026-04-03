package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.InvoiceRepository;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.ItemShareRepository;
import com.fintrack.domain.user.User;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Projects installment items from an invoice into future invoices.
 * Copies category and shares from the source item to each projected item.
 */
@Service
@Transactional
public class InstallmentProjectionService {

    private static final Logger logger =
            LoggerFactory.getLogger(InstallmentProjectionService.class);

    private final InvoiceRepository invoiceRepository;
    private final ItemShareRepository itemShareRepository;

    public InstallmentProjectionService(
            InvoiceRepository invoiceRepository,
            ItemShareRepository itemShareRepository) {
        this.invoiceRepository = invoiceRepository;
        this.itemShareRepository = itemShareRepository;
    }

    /**
     * Scans the given invoice for installment items and projects
     * remaining installments into future invoices.
     *
     * @param sourceInvoice the invoice to scan. Cannot be null.
     * @param owner the card owner (for security scoping). Cannot be null.
     * @return the number of projected items created.
     */
    public int projectInstallments(Invoice sourceInvoice, User owner) {
        Validate.notNull(sourceInvoice, "Source invoice must not be null.");
        Validate.notNull(owner, "Owner must not be null.");

        CreditCard creditCard = sourceInvoice.getCreditCard();
        Validate.isTrue(
                creditCard.getOwner().equals(owner),
                "Invoice does not belong to user.");

        int projectedCount = 0;

        List<InvoiceItem> items = sourceInvoice.getItems();
        for (InvoiceItem item : items) {
            if (item.isProjected()) {
                continue;
            }
            if (item.getTotalInstallments() <= 1) {
                continue;
            }
            if (item.getInstallments() >= item.getTotalInstallments()) {
                continue;
            }

            int remaining = item.getTotalInstallments()
                    - item.getInstallments();
            YearMonth baseMonth = sourceInvoice.getMonth();

            for (int offset = 1; offset <= remaining; offset++) {
                YearMonth targetMonth = baseMonth.plusMonths(offset);
                int targetInstallment = item.getInstallments() + offset;

                Invoice targetInvoice = findOrCreateInvoice(
                        creditCard, targetMonth,
                        computeDueDate(sourceInvoice, targetMonth));

                if (hasProjectionForSource(
                        targetInvoice, item.getId(), targetInstallment)) {
                    continue;
                }

                InvoiceItem projected = InvoiceItem.projected(
                        targetInvoice,
                        item.getDescription(),
                        item.getAmount(),
                        item.getCategory(),
                        item.getPurchaseDate(),
                        targetInstallment,
                        item.getTotalInstallments(),
                        item.getId());

                targetInvoice.addItem(projected);
                copyShares(item, projected);
                projectedCount++;
            }
        }

        if (projectedCount > 0) {
            logger.info(
                    "Projected {} installment items from invoice {} (card {})",
                    projectedCount, sourceInvoice.getId(),
                    creditCard.getId());
        }

        return projectedCount;
    }

    /**
     * Removes all projected items from the given invoice.
     *
     * @param invoice the invoice to clean up. Cannot be null.
     * @return the number of projected items removed.
     */
    public int removeProjectedItems(Invoice invoice) {
        Validate.notNull(invoice, "Invoice must not be null.");

        List<InvoiceItem> projectedItems = invoice.getItems().stream()
                .filter(InvoiceItem::isProjected)
                .toList();

        for (InvoiceItem item : projectedItems) {
            invoice.removeItem(item);
        }

        if (!projectedItems.isEmpty()) {
            invoiceRepository.save(invoice);
            logger.info("Removed {} projected items from invoice {}",
                    projectedItems.size(), invoice.getId());
        }

        return projectedItems.size();
    }

    private Invoice findOrCreateInvoice(
            CreditCard creditCard, YearMonth month, LocalDate dueDate) {
        return invoiceRepository
                .findByCreditCardAndMonth(creditCard, month)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Invoice created = Invoice.of(creditCard, month, dueDate);
                    Invoice saved = invoiceRepository.save(created);
                    logger.info(
                            "Created invoice for card {} month {}",
                            creditCard.getId(), month);
                    return saved;
                });
    }

    private boolean hasProjectionForSource(
            Invoice invoice, Long sourceItemId, int installment) {
        return invoice.getItems().stream()
                .anyMatch(i -> i.isProjected()
                        && Objects.equals(i.getSourceItemId(), sourceItemId)
                        && i.getInstallments() == installment);
    }

    private LocalDate computeDueDate(
            Invoice source, YearMonth targetMonth) {
        int dayOfMonth = Math.min(
                source.getDueDate().getDayOfMonth(),
                targetMonth.lengthOfMonth());
        return targetMonth.atDay(dayOfMonth);
    }

    private void copyShares(InvoiceItem source, InvoiceItem target) {
        for (ItemShare share : source.getShares()) {
            ItemShare copy;
            if (share.getUser() != null) {
                copy = ItemShare.of(
                        share.getUser(), target,
                        share.getPercentage(), share.getAmount(),
                        share.isResponsible());
            } else {
                copy = ItemShare.forContact(
                        share.getTrustedContact(), target,
                        share.getPercentage(), share.getAmount(),
                        share.isResponsible());
            }
            target.addShare(copy);
            itemShareRepository.save(copy);
        }
    }
}
