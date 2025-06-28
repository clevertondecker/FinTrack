package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.*;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.Email;
import com.fintrack.dto.creditcard.CreateInvoiceRequest;
import com.fintrack.dto.creditcard.CreateInvoiceItemRequest;
import com.fintrack.dto.creditcard.InvoiceResponse;
import com.fintrack.dto.creditcard.InvoiceItemResponse;
import com.fintrack.dto.creditcard.InvoicePaymentRequest;
import com.fintrack.dto.creditcard.InvoicePaymentResponse;
import com.fintrack.infrastructure.persistence.creditcard.*;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final CreditCardJpaRepository creditCardRepository;
    private final CategoryJpaRepository categoryRepository;
    private final UserRepository userRepository;

    public InvoiceService(@Qualifier("invoiceJpaRepository") final InvoiceRepository theInvoiceRepository,
                         @Qualifier("invoiceItemJpaRepository") final InvoiceItemRepository theInvoiceItemRepository,
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
     * @param username the username (email) from UserDetails. Can be null or empty.
     * @return an Optional containing the user if found, empty otherwise. Never null.
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
     * @param request the invoice creation request. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the created invoice. Never null.
     * @throws IllegalArgumentException if credit card not found or doesn't belong to user.
     */
    public Invoice createInvoice(CreateInvoiceRequest request, User user) {
        // Find the credit card
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(request.creditCardId(), user);
        if (creditCardOpt.isEmpty()) {
            throw new IllegalArgumentException("Credit card not found");
        }
        CreditCard creditCard = creditCardOpt.get();

        // Create the invoice with month derived from due date
        YearMonth invoiceMonth = YearMonth.from(request.dueDate());
        
        Invoice invoice = Invoice.of(
            creditCard,
            invoiceMonth,
            request.dueDate()
        );

        return invoiceRepository.save(invoice);
    }

    /**
     * Gets all invoices for a user.
     *
     * @param user the authenticated user. Cannot be null.
     * @return list of invoices. Never null, may be empty.
     */
    public List<Invoice> getUserInvoices(User user) {
        List<Invoice> invoices = invoiceRepository.findByCreditCardOwner(user);
        // Update status dynamically for each invoice to reflect current state
        invoices.forEach(Invoice::updateStatus);
        return invoices;
    }

    /**
     * Gets invoices for a specific credit card.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return list of invoices for the credit card. Never null, may be empty.
     * @throws IllegalArgumentException if credit card not found or doesn't belong to user.
     */
    public List<Invoice> getInvoicesByCreditCard(Long creditCardId, User user) {
        // Find the credit card
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(creditCardId, user);
        if (creditCardOpt.isEmpty()) {
            throw new IllegalArgumentException("Credit card not found");
        }
        CreditCard creditCard = creditCardOpt.get();

        List<Invoice> invoices = invoiceRepository.findByCreditCard(creditCard);
        // Update status dynamically for each invoice to reflect current state
        invoices.forEach(Invoice::updateStatus);
        return invoices;
    }

    /**
     * Gets a specific invoice by ID.
     *
     * @param invoiceId the invoice ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the invoice. Never null.
     * @throws IllegalArgumentException if invoice not found or doesn't belong to user.
     */
    public Invoice getInvoice(Long invoiceId, User user) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndCreditCardOwner(invoiceId, user);
        if (invoiceOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice not found");
        }
        Invoice invoice = invoiceOpt.get();
        // Update status dynamically to reflect current state
        invoice.updateStatus();
        return invoice;
    }

    /**
     * Creates a new invoice item.
     *
     * @param invoiceId the invoice ID. Cannot be null.
     * @param request the invoice item creation request. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the updated invoice with the new item. Never null.
     * @throws IllegalArgumentException if invoice not found or doesn't belong to user.
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
     * @param invoiceId the invoice ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return list of invoice items. Never null, may be empty.
     * @throws IllegalArgumentException if invoice not found or doesn't belong to user.
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
     * @param invoiceId the invoice ID. Cannot be null.
     * @param itemId the invoice item ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the invoice item. Never null.
     * @throws IllegalArgumentException if item not found or doesn't belong to invoice/user.
     */
    public InvoiceItem getInvoiceItem(Long invoiceId, Long itemId, User user) {
        // Find the invoice item
        Optional<InvoiceItem> itemOpt =
          invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(itemId, user);

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
     * @param invoiceId the invoice ID. Cannot be null.
     * @param itemId the invoice item ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the updated invoice. Never null.
     * @throws IllegalArgumentException if item not found or doesn't belong to invoice/user.
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
     * Converts an Invoice to a InvoiceResponse DTO.
     */
    public InvoiceResponse toInvoiceResponse(Invoice invoice) {
        // Use dynamic calculation for real-time consistency
        InvoiceStatus currentStatus = invoice.calculateCurrentStatus();
        
        return new InvoiceResponse(
            invoice.getId(),
            invoice.getCreditCard().getId(),
            invoice.getCreditCard().getName(),
            invoice.getDueDate(),
            invoice.getTotalAmount(),
            invoice.getPaidAmount(),
            currentStatus.name(),
            invoice.getCreatedAt(),
            invoice.getUpdatedAt()
        );
    }

    /**
     * Converts a list of invoices to InvoiceResponse DTOs.
     */
    public List<InvoiceResponse> toInvoiceResponseList(List<Invoice> invoices) {
        List<InvoiceResponse> dtos = new ArrayList<>();
        for (Invoice invoice : invoices) {
            dtos.add(toInvoiceResponse(invoice));
        }
        return dtos;
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

    /**
     * Converts an InvoiceItem to InvoiceItemResponse DTO.
     */
    public InvoiceItemResponse toInvoiceItemResponse(InvoiceItem item) {
        return new InvoiceItemResponse(
            item.getId(),
            item.getInvoice().getId(),
            item.getDescription(),
            item.getAmount(),
            item.getCategory() != null ? item.getCategory().getName() : null,
            item.getPurchaseDate().toString(),
            item.getCreatedAt(),
            item.getInstallments(),
            item.getTotalInstallments()
        );
    }

    /**
     * Converts a list of InvoiceItem to InvoiceItemResponse DTOs.
     */
    public List<InvoiceItemResponse> toInvoiceItemResponseList(List<InvoiceItem> items) {
        List<InvoiceItemResponse> dtos = new ArrayList<>();
        for (InvoiceItem item : items) {
            dtos.add(toInvoiceItemResponse(item));
        }
        return dtos;
    }

    /**
     * Registers a payment for an invoice.
     *
     * @param invoiceId the invoice ID. Cannot be null.
     * @param request the payment request. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the payment response DTO. Never null.
     * @throws IllegalArgumentException if invoice not found or doesn't belong to user.
     */
    public InvoicePaymentResponse payInvoice(Long invoiceId, InvoicePaymentRequest request, User user) {
        Invoice invoice = getInvoice(invoiceId, user);
        invoice.recordPayment(request.amount());
        Invoice saved = invoiceRepository.save(invoice);
        return new InvoicePaymentResponse(
            saved.getId(),
            saved.getCreditCard().getId(),
            saved.getCreditCard().getName(),
            saved.getDueDate(),
            saved.getTotalAmount(),
            saved.getPaidAmount(),
            saved.getStatus().name(),
            saved.getUpdatedAt(),
            "Invoice payment registered successfully"
        );
    }
}