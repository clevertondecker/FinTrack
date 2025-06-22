package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.dto.creditcard.CreateInvoiceItemRequest;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
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
 * REST controller for managing invoice items.
 * Provides endpoints for invoice item operations.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceItemController {

    private final InvoiceService invoiceService;

    public InvoiceItemController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Creates a new invoice item.
     *
     * @param invoiceId the invoice ID from the path.
     * @param request the invoice item creation request. Must be valid.
     * @param userDetails the authenticated user details.
     * @return a response with the created invoice item information.
     */
    @PostMapping("/{invoiceId}/items")
    public ResponseEntity<Map<String, Object>> createInvoiceItem(
            @PathVariable Long invoiceId,
            @Valid @RequestBody CreateInvoiceItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Find the user
            Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Create the invoice item using service
            Invoice savedInvoice = invoiceService.createInvoiceItem(invoiceId, request, user);

            // Find the created item to return its details
            List<InvoiceItem> items = invoiceService.getInvoiceItems(invoiceId, user);
            InvoiceItem createdItem = items.stream()
                .filter(item -> item.getDescription().equals(request.description()) && 
                               item.getAmount().equals(request.amount()))
                .findFirst()
                .orElse(null);

            return ResponseEntity.ok(Map.of(
                "message", "Invoice item created successfully",
                "id", createdItem != null ? createdItem.getId() : 0L,
                "invoiceId", invoiceId,
                "description", request.description(),
                "amount", request.amount(),
                "category", createdItem != null && createdItem.getCategory() != null ? 
                           createdItem.getCategory().getName() : null,
                "invoiceTotalAmount", savedInvoice.getTotalAmount()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Gets all items for a specific invoice.
     *
     * @param invoiceId the invoice ID.
     * @param userDetails the authenticated user details.
     * @return a response with the invoice items.
     */
    @GetMapping("/{invoiceId}/items")
    public ResponseEntity<Map<String, Object>> getInvoiceItems(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Find the user
            Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Get items using service
            List<InvoiceItem> items = invoiceService.getInvoiceItems(invoiceId, user);
            List<Map<String, Object>> itemDtos = invoiceService.toInvoiceItemDtos(items);

            // Get invoice for total amount
            Invoice invoice = invoiceService.getInvoice(invoiceId, user);

            return ResponseEntity.ok(Map.of(
                "message", "Invoice items retrieved successfully",
                "invoiceId", invoiceId,
                "items", itemDtos,
                "count", itemDtos.size(),
                "totalAmount", invoice.getTotalAmount()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Gets a specific invoice item by ID.
     *
     * @param invoiceId the invoice ID.
     * @param itemId the invoice item ID.
     * @param userDetails the authenticated user details.
     * @return a response with the invoice item information.
     */
    @GetMapping("/{invoiceId}/items/{itemId}")
    public ResponseEntity<Map<String, Object>> getInvoiceItem(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Find the user
            Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Get item using service
            InvoiceItem item = invoiceService.getInvoiceItem(invoiceId, itemId, user);
            Map<String, Object> itemDto = invoiceService.toInvoiceItemDto(item);

            return ResponseEntity.ok(Map.of(
                "message", "Invoice item retrieved successfully",
                "item", itemDto
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Deletes an invoice item.
     *
     * @param invoiceId the invoice ID.
     * @param itemId the invoice item ID.
     * @param userDetails the authenticated user details.
     * @return a response confirming the deletion.
     */
    @DeleteMapping("/{invoiceId}/items/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteInvoiceItem(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Find the user
            Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Delete item using service
            Invoice updatedInvoice = invoiceService.deleteInvoiceItem(invoiceId, itemId, user);

            return ResponseEntity.ok(Map.of(
                "message", "Invoice item deleted successfully",
                "invoiceId", invoiceId,
                "itemId", itemId,
                "invoiceTotalAmount", updatedInvoice.getTotalAmount()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}