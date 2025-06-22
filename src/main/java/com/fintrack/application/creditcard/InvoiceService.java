package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.*;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.Email;
import com.fintrack.dto.creditcard.CreateInvoiceRequest;
import com.fintrack.dto.creditcard.CreateInvoiceItemRequest;
import com.fintrack.infrastructure.persistence.creditcard.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service for managing invoice-related business logic.
 * Provides operations for invoices and invoice items.
 */
@Service
@Transactional
public class InvoiceService {

    private final InvoiceJpaRepository invoiceRepository;
    private final InvoiceItemJpaRepository invoiceItemRepository;
    private final CreditCardJpaRepository creditCardRepository;
    private final CategoryJpaRepository categoryRepository;
    private final UserRepository userRepository;

    public InvoiceService(final InvoiceJpaRepository theInvoiceRepository,
                         final InvoiceItemJpaRepository theInvoiceItemRepository,
                         final CreditCardJpaRepository theCreditCardRepository,
                         final CategoryJpaRepository theCategoryRepository,
                         final UserRepository theUserRepository) {
        invoiceRepository = theInvoiceRepository;
        invoiceItemRepository = theInvoiceItemRepository;
        creditCardRepository = theCreditCardRepository;
        categoryRepository = theCategoryRepository;
        userRepository = theUserRepository;
    }

    /**
     * Finds a user by email from UserDetails.
     *
     * @param username the username (email) from UserDetails
     * @return an Optional containing the user if found
     */
    public Optional<User> findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            Email email = Email.of(username);
            return userRepository.findByEmail(email);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new invoice for a credit card.
     *
     * @param request the invoice creation request
     * @param user the authenticated user
     * @return the created invoice
     * @throws IllegalArgumentException if credit card not found or doesn't belong to user
     */
    public Invoice createInvoice(CreateInvoiceRequest request, User user) {
        // Find the credit card
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(request.creditCardId(), user);
        if (creditCardOpt.isEmpty()) {
            throw new IllegalArgumentException("Credit card not found");
        }
        CreditCard creditCard = creditCardOpt.get();

        // Create the invoice
        Invoice invoice = Invoice.of(
            creditCard,
            YearMonth.from(request.dueDate()),
            request.dueDate()
        );

        return invoiceRepository.save(invoice);
    }

    /**
     * Gets all invoices for a user.
     *
     * @param user the authenticated user
     * @return list of invoices
     */
    public List<Invoice> getUserInvoices(User user) {
        return invoiceRepository.findByCreditCardOwner(user);
    }

    /**
     * Gets invoices for a specific credit card.
     *
     * @param creditCardId the credit card ID
     * @param user the authenticated user
     * @return list of invoices for the credit card
     * @throws IllegalArgumentException if credit card not found or doesn't belong to user
     */
    public List<Invoice> getInvoicesByCreditCard(Long creditCardId, User user) {
        // Find the credit card
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(creditCardId, user);
        if (creditCardOpt.isEmpty()) {
            throw new IllegalArgumentException("Credit card not found");
        }
        CreditCard creditCard = creditCardOpt.get();

        return invoiceRepository.findByCreditCard(creditCard);
    }

    /**
     * Gets a specific invoice by ID.
     *
     * @param invoiceId the invoice ID
     * @param user the authenticated user
     * @return the invoice
     * @throws IllegalArgumentException if invoice not found or doesn't belong to user
     */
    public Invoice getInvoice(Long invoiceId, User user) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndCreditCardOwner(invoiceId, user);
        if (invoiceOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice not found");
        }
        return invoiceOpt.get();
    }

    /**
     * Creates a new invoice item.
     *
     * @param invoiceId the invoice ID
     * @param request the invoice item creation request
     * @param user the authenticated user
     * @return the updated invoice with the new item
     * @throws IllegalArgumentException if invoice not found or doesn't belong to user
     */
    public Invoice createInvoiceItem(Long invoiceId, CreateInvoiceItemRequest request, User user) {
        // Find the invoice
        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndCreditCardOwner(invoiceId, user);
        if (invoiceOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice not found");
        }
        Invoice invoice = invoiceOpt.get();

        // Find the category if provided
        Category category = null;
        if (request.categoryId() != null) {
            Optional<Category> categoryOpt = categoryRepository.findById(request.categoryId());
            category = categoryOpt.orElse(null);
        }

        // Create the invoice item
        InvoiceItem invoiceItem = InvoiceItem.of(
            invoice,
            request.description(),
            request.amount(),
            category,
            request.purchaseDate()
        );

        // Add item to invoice (this will recalculate the total)
        invoice.addItem(invoiceItem);

        // Save the invoice (this will cascade to save the item)
        return invoiceRepository.save(invoice);
    }

    /**
     * Gets all items for a specific invoice.
     *
     * @param invoiceId the invoice ID
     * @param user the authenticated user
     * @return list of invoice items
     * @throws IllegalArgumentException if invoice not found or doesn't belong to user
     */
    public List<InvoiceItem> getInvoiceItems(Long invoiceId, User user) {
        // Find the invoice
        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndCreditCardOwner(invoiceId, user);
        if (invoiceOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice not found");
        }
        Invoice invoice = invoiceOpt.get();

        return invoiceItemRepository.findByInvoice(invoice);
    }

    /**
     * Gets a specific invoice item by ID.
     *
     * @param invoiceId the invoice ID
     * @param itemId the invoice item ID
     * @param user the authenticated user
     * @return the invoice item
     * @throws IllegalArgumentException if item not found or doesn't belong to invoice/user
     */
    public InvoiceItem getInvoiceItem(Long invoiceId, Long itemId, User user) {
        // Find the invoice item
        Optional<InvoiceItem> itemOpt = invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(itemId, user);
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice item not found");
        }
        InvoiceItem item = itemOpt.get();

        // Verify the item belongs to the specified invoice
        if (!item.getInvoice().getId().equals(invoiceId)) {
            throw new IllegalArgumentException("Item does not belong to the specified invoice");
        }

        return item;
    }

    /**
     * Deletes an invoice item.
     *
     * @param invoiceId the invoice ID
     * @param itemId the invoice item ID
     * @param user the authenticated user
     * @return the updated invoice
     * @throws IllegalArgumentException if item not found or doesn't belong to invoice/user
     */
    public Invoice deleteInvoiceItem(Long invoiceId, Long itemId, User user) {
        // Find the invoice item
        Optional<InvoiceItem> itemOpt = invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(itemId, user);
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice item not found");
        }
        InvoiceItem item = itemOpt.get();

        // Verify the item belongs to the specified invoice
        if (!item.getInvoice().getId().equals(invoiceId)) {
            throw new IllegalArgumentException("Item does not belong to the specified invoice");
        }

        // Remove item from invoice and save
        Invoice invoice = item.getInvoice();
        invoice.removeItem(item);
        invoiceItemRepository.delete(item);

        return invoiceRepository.save(invoice);
    }

    /**
     * Converts an Invoice to a Map DTO.
     *
     * @param invoice the invoice to convert
     * @return Map representation of the invoice
     */
    public Map<String, Object> toInvoiceDto(Invoice invoice) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", invoice.getId());
        dto.put("creditCardId", invoice.getCreditCard().getId());
        dto.put("creditCardName", invoice.getCreditCard().getName());
        dto.put("dueDate", invoice.getDueDate());
        dto.put("totalAmount", invoice.getTotalAmount());
        dto.put("paidAmount", invoice.getPaidAmount());
        dto.put("status", invoice.getStatus().name());
        dto.put("createdAt", invoice.getCreatedAt());
        dto.put("updatedAt", invoice.getUpdatedAt());
        return dto;
    }

    /**
     * Converts an InvoiceItem to a Map DTO.
     *
     * @param item the invoice item to convert
     * @return Map representation of the invoice item
     */
    public Map<String, Object> toInvoiceItemDto(InvoiceItem item) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", item.getId());
        dto.put("invoiceId", item.getInvoice().getId());
        dto.put("description", item.getDescription());
        dto.put("amount", item.getAmount());
        dto.put("category", item.getCategory() != null ? item.getCategory().getName() : null);
        dto.put("purchaseDate", item.getPurchaseDate().toString());
        dto.put("createdAt", item.getCreatedAt());
        return dto;
    }

    /**
     * Converts a list of invoices to DTOs.
     *
     * @param invoices the list of invoices to convert
     * @return list of invoice DTOs
     */
    public List<Map<String, Object>> toInvoiceDtos(List<Invoice> invoices) {
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (Invoice invoice : invoices) {
            dtos.add(toInvoiceDto(invoice));
        }
        return dtos;
    }

    /**
     * Converts a list of invoice items to DTOs.
     *
     * @param items the list of invoice items to convert
     * @return list of invoice item DTOs
     */
    public List<Map<String, Object>> toInvoiceItemDtos(List<InvoiceItem> items) {
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (InvoiceItem item : items) {
            dtos.add(toInvoiceItemDto(item));
        }
        return dtos;
    }
} 