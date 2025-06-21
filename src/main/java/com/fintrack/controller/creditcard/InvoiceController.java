package com.fintrack.controller.creditcard;

import com.fintrack.domain.user.UserRepository;
import com.fintrack.dto.creditcard.CreateInvoiceRequest;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceJpaRepository;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.time.YearMonth;

/**
 * REST controller for managing invoices.
 * Provides endpoints for invoice operations.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceJpaRepository invoiceRepository;
    private final CreditCardJpaRepository creditCardRepository;
    private final UserRepository userRepository;

    public InvoiceController(InvoiceJpaRepository invoiceRepository,
                            CreditCardJpaRepository creditCardRepository,
                            UserRepository userRepository) {
        this.invoiceRepository = invoiceRepository;
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new invoice for a credit card.
     *
     * @param request the invoice creation request. Must be valid.
     * @param userDetails the authenticated user details.
     * @return a response with the created invoice information.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Find the user
        Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the credit card
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(request.creditCardId(), user);
        if (creditCardOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Credit card not found"));
        }
        CreditCard creditCard = creditCardOpt.get();

        // Create the invoice
        Invoice invoice = Invoice.of(
            creditCard,
            YearMonth.from(request.dueDate()),
            request.dueDate()
        );

        // Save the invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        return ResponseEntity.ok(Map.of(
            "message", "Invoice created successfully",
            "id", savedInvoice.getId(),
            "creditCardId", savedInvoice.getCreditCard().getId(),
            "dueDate", savedInvoice.getDueDate(),
            "totalAmount", savedInvoice.getTotalAmount(),
            "status", savedInvoice.getStatus().name()
        ));
    }

    /**
     * Gets all invoices for the authenticated user.
     *
     * @param userDetails the authenticated user details.
     * @return a response with the user's invoices.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserInvoices(
            @AuthenticationPrincipal UserDetails userDetails) {

        // Find the user
        Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Get all invoices for the user's credit cards
        List<Invoice> invoices = invoiceRepository.findByCreditCardOwner(user);

        // Convert to DTO format
        List<Map<String, Object>> invoiceDtos = new ArrayList<>();
        for (Invoice invoice : invoices) {
            invoiceDtos.add(Map.of(
                "id", invoice.getId(),
                "creditCardId", invoice.getCreditCard().getId(),
                "creditCardName", invoice.getCreditCard().getName(),
                "dueDate", invoice.getDueDate(),
                "totalAmount", invoice.getTotalAmount(),
                "paidAmount", invoice.getPaidAmount(),
                "status", invoice.getStatus().name(),
                "createdAt", invoice.getCreatedAt()
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Invoices retrieved successfully",
            "invoices", invoiceDtos,
            "count", invoiceDtos.size()
        ));
    }

    /**
     * Gets invoices for a specific credit card.
     *
     * @param creditCardId the credit card ID.
     * @param userDetails the authenticated user details.
     * @return a response with the credit card's invoices.
     */
    @GetMapping("/credit-card/{creditCardId}")
    public ResponseEntity<Map<String, Object>> getInvoicesByCreditCard(
            @PathVariable Long creditCardId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Find the user
        Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the credit card
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(creditCardId, user);
        if (creditCardOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Credit card not found"));
        }
        CreditCard creditCard = creditCardOpt.get();

        // Get invoices for the credit card
        List<Invoice> invoices = invoiceRepository.findByCreditCard(creditCard);

        // Convert to DTO format
        List<Map<String, Object>> invoiceDtos = new ArrayList<>();
        for (Invoice invoice : invoices) {
            invoiceDtos.add(Map.of(
                "id", invoice.getId(),
                "dueDate", invoice.getDueDate(),
                "totalAmount", invoice.getTotalAmount(),
                "status", invoice.getStatus().name(),
                "createdAt", invoice.getCreatedAt()
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Invoices retrieved successfully",
            "creditCardId", creditCardId,
            "creditCardName", creditCard.getName(),
            "invoices", invoiceDtos,
            "count", invoiceDtos.size()
        ));
    }

    /**
     * Gets a specific invoice by ID.
     *
     * @param id the invoice ID.
     * @param userDetails the authenticated user details.
     * @return a response with the invoice information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Find the user
        Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the invoice
        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndCreditCardOwner(id, user);
        if (invoiceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Invoice invoice = invoiceOpt.get();

        return ResponseEntity.ok(Map.of(
            "message", "Invoice retrieved successfully",
            "invoice", Map.of(
                "id", invoice.getId(),
                "creditCardId", invoice.getCreditCard().getId(),
                "creditCardName", invoice.getCreditCard().getName(),
                "dueDate", invoice.getDueDate(),
                "totalAmount", invoice.getTotalAmount(),
                "status", invoice.getStatus().name(),
                "createdAt", invoice.getCreatedAt(),
                "updatedAt", invoice.getUpdatedAt()
            )
        ));
    }
}