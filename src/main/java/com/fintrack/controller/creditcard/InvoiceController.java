package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.dto.creditcard.CreateInvoiceRequest;
import com.fintrack.dto.creditcard.InvoiceCreateResponse;
import com.fintrack.dto.creditcard.InvoiceListResponse;
import com.fintrack.dto.creditcard.InvoiceDetailResponse;
import com.fintrack.dto.creditcard.InvoiceByCreditCardResponse;
import com.fintrack.dto.creditcard.InvoiceResponse;
import com.fintrack.dto.creditcard.InvoicePaymentRequest;
import com.fintrack.dto.creditcard.InvoicePaymentResponse;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing invoices.
 * Provides endpoints for invoice operations.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Creates a new invoice for a credit card.
     *
     * @param request the invoice creation request. Must be valid.
     * @param userDetails the authenticated user details.
     * @return a response with the created invoice information.
     */
    @PostMapping
    public ResponseEntity<InvoiceCreateResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Find the user
        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Create the invoice using service
        Invoice invoice = invoiceService.createInvoice(request, user);

        InvoiceCreateResponse response = new InvoiceCreateResponse(
            "Invoice created successfully",
            invoice.getId(),
            invoice.getCreditCard().getId(),
            invoice.getDueDate(),
            invoice.getStatus().name()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets all invoices for the authenticated user.
     *
     * @param userDetails the authenticated user details.
     * @return a response with the user's invoices.
     */
    @GetMapping
    public ResponseEntity<InvoiceListResponse> getUserInvoices(
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        List<Invoice> invoices = invoiceService.getUserInvoices(user);
        List<InvoiceResponse> invoiceResponses = invoiceService.toInvoiceResponseList(invoices);

        InvoiceListResponse response = new InvoiceListResponse(
            "Invoices retrieved successfully",
            invoiceResponses,
            invoiceResponses.size()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets invoices for a specific credit card.
     *
     * @param creditCardId the credit card ID.
     * @param userDetails the authenticated user details.
     * @return a response with the credit card's invoices.
     */
    @GetMapping("/credit-card/{creditCardId}")
    public ResponseEntity<InvoiceByCreditCardResponse> getInvoicesByCreditCard(
            @PathVariable Long creditCardId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        List<Invoice> invoices = invoiceService.getInvoicesByCreditCard(creditCardId, user);
        List<InvoiceResponse> invoiceResponses = invoiceService.toInvoiceResponseList(invoices);

        String creditCardName = invoices.isEmpty() ? "" : invoices.get(0).getCreditCard().getName();

        InvoiceByCreditCardResponse response = new InvoiceByCreditCardResponse(
            "Invoices retrieved successfully",
            creditCardId,
            creditCardName,
            invoiceResponses,
            invoiceResponses.size()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets a specific invoice by ID.
     *
     * @param id the invoice ID.
     * @param userDetails the authenticated user details.
     * @return a response with the invoice information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailResponse> getInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        Invoice invoice = invoiceService.getInvoice(id, user);
        InvoiceResponse invoiceResponse = invoiceService.toInvoiceResponse(invoice);

        InvoiceDetailResponse response = new InvoiceDetailResponse(
            "Invoice retrieved successfully",
            invoiceResponse
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<InvoicePaymentResponse> payInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoicePaymentRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Optional<com.fintrack.domain.user.User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        com.fintrack.domain.user.User user = userOpt.get();
        InvoicePaymentResponse response = invoiceService.payInvoice(id, request, user);
        return ResponseEntity.ok(response);
    }
}