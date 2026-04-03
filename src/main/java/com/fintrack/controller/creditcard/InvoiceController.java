package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.InstallmentProjectionService;
import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.controller.BaseController;
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
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing invoices.
 * Provides endpoints for invoice operations.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController extends BaseController {

    /** The invoice service. */
    private final InvoiceService invoiceService;

    /** The installment projection service. */
    private final InstallmentProjectionService installmentProjectionService;

    public InvoiceController(
            InvoiceService invoiceService,
            InstallmentProjectionService installmentProjectionService) {
        this.invoiceService = invoiceService;
        this.installmentProjectionService = installmentProjectionService;
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

        User user = resolveUser(userDetails);

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

        User user = resolveUser(userDetails);

        List<Invoice> invoices = invoiceService.getUserInvoices(user);
        List<InvoiceResponse> invoiceResponses = invoices.stream()
            .map(invoice -> invoiceService.toInvoiceResponse(invoice, user))
            .toList();

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

        User user = resolveUser(userDetails);

        List<Invoice> invoices = invoiceService.getInvoicesByCreditCard(creditCardId, user);
        List<InvoiceResponse> invoiceResponses = invoices.stream()
            .map(invoice -> invoiceService.toInvoiceResponse(invoice, user))
            .toList();

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

        User user = resolveUser(userDetails);

        Invoice invoice = invoiceService.getInvoice(id, user);
        InvoiceResponse invoiceResponse = invoiceService.toInvoiceResponse(invoice, user);

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
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        InvoicePaymentResponse response = invoiceService.payInvoice(id, request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a summary of shared items for a given invoice,
     * so the frontend can show a confirmation dialog before deleting.
     */
    @GetMapping("/{id}/delete-info")
    public ResponseEntity<java.util.Map<String, Object>> getDeleteInfo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        Invoice invoice = invoiceService.getInvoice(id, user);

        int totalItems = invoice.getItems().size();
        long sharedItems = invoice.getItems().stream()
                .filter(item -> !item.getShares().isEmpty())
                .count();
        long totalShares = invoice.getItems().stream()
                .mapToLong(item -> item.getShares().size())
                .sum();
        long paidShares = invoice.getItems().stream()
                .flatMap(item -> item.getShares().stream())
                .filter(com.fintrack.domain.creditcard.ItemShare::isPaid)
                .count();

        return ResponseEntity.ok(java.util.Map.of(
                "invoiceId", id,
                "totalItems", totalItems,
                "sharedItems", sharedItems,
                "totalShares", totalShares,
                "paidShares", paidShares
        ));
    }

    /**
     * Deletes an invoice by ID. Accessible to the card owner.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        invoiceService.getInvoice(id, user);
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Projects remaining installment items from this invoice into future
     * invoices, copying categories and shares.
     *
     * @param id the invoice ID.
     * @param userDetails the authenticated user details.
     * @return the number of projected items created.
     */
    @PostMapping("/{id}/project-installments")
    public ResponseEntity<Map<String, Object>> projectInstallments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        Invoice invoice = invoiceService.getInvoice(id, user);
        int count = installmentProjectionService
                .projectInstallments(invoice, user);
        return ResponseEntity.ok(Map.of(
                "message", "Projected " + count + " installment items",
                "projectedCount", count));
    }
}