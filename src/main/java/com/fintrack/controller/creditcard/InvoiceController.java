package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.dto.creditcard.CreateInvoiceRequest;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<Map<String, Object>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Find the user
            Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Create the invoice using service
            Invoice invoice = invoiceService.createInvoice(request, user);

            return ResponseEntity.ok(Map.of(
                "message", "Invoice created successfully",
                "id", invoice.getId(),
                "creditCardId", invoice.getCreditCard().getId(),
                "dueDate", invoice.getDueDate(),
                "totalAmount", invoice.getTotalAmount(),
                "status", invoice.getStatus().name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
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

        try {
            // Find the user
            Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Get invoices using service
            List<Invoice> invoices = invoiceService.getUserInvoices(user);
            List<Map<String, Object>> invoiceDtos = invoiceService.toInvoiceDtos(invoices);

            return ResponseEntity.ok(Map.of(
                "message", "Invoices retrieved successfully",
                "invoices", invoiceDtos,
                "count", invoiceDtos.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
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

        try {
            // Find the user
            Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Get invoices using service
            List<Invoice> invoices = invoiceService.getInvoicesByCreditCard(creditCardId, user);
            List<Map<String, Object>> invoiceDtos = invoiceService.toInvoiceDtos(invoices);

            return ResponseEntity.ok(Map.of(
                "message", "Invoices retrieved successfully",
                "creditCardId", creditCardId,
                "creditCardName", invoices.isEmpty() ? "" : invoices.get(0).getCreditCard().getName(),
                "invoices", invoiceDtos,
                "count", invoiceDtos.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
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

        try {
            // Find the user
            Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Get invoice using service
            Invoice invoice = invoiceService.getInvoice(id, user);
            Map<String, Object> invoiceDto = invoiceService.toInvoiceDto(invoice);

            return ResponseEntity.ok(Map.of(
                "message", "Invoice retrieved successfully",
                "invoice", invoiceDto
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}