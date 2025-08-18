package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.dto.creditcard.CreateInvoiceItemRequest;
import com.fintrack.dto.creditcard.InvoiceItemCreateResponse;
import com.fintrack.dto.creditcard.InvoiceItemListResponse;
import com.fintrack.dto.creditcard.InvoiceItemDetailResponse;
import com.fintrack.dto.creditcard.InvoiceItemResponse;
import com.fintrack.dto.creditcard.UpdateInvoiceItemCategoryRequest;
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
    public ResponseEntity<InvoiceItemCreateResponse> createInvoiceItem(
            @PathVariable Long invoiceId,
            @Valid @RequestBody CreateInvoiceItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        Invoice savedInvoice = invoiceService.createInvoiceItem(invoiceId, request, user);
        List<InvoiceItem> items = invoiceService.getInvoiceItems(invoiceId, user);

        InvoiceItem createdItem = items.stream()
            .filter(item -> item.getDescription().equals(request.description()) &&
                           item.getAmount().equals(request.amount()))
            .findFirst()
            .orElse(null);

        InvoiceItemCreateResponse response = new InvoiceItemCreateResponse(
            "Invoice item created successfully",
            createdItem != null ? createdItem.getId() : 0L,
            invoiceId,
            request.description(),
            request.amount(),
            createdItem != null && createdItem.getCategory() != null ? createdItem.getCategory().getName() : null,
            savedInvoice.getTotalAmount()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets all items for a specific invoice.
     *
     * @param invoiceId the invoice ID.
     * @param userDetails the authenticated user details.
     * @return a response with the invoice items.
     */
    @GetMapping("/{invoiceId}/items")
    public ResponseEntity<InvoiceItemListResponse> getInvoiceItems(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        List<InvoiceItem> items = invoiceService.getInvoiceItems(invoiceId, user);
        List<InvoiceItemResponse> itemResponses = invoiceService.toInvoiceItemResponseList(items);
        Invoice invoice = invoiceService.getInvoice(invoiceId, user);

        InvoiceItemListResponse response = new InvoiceItemListResponse(
            "Invoice items retrieved successfully",
            invoiceId,
            itemResponses,
            itemResponses.size(),
            invoice.getTotalAmount()
        );

        return ResponseEntity.ok(response);
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
    public ResponseEntity<InvoiceItemDetailResponse> getInvoiceItem(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        InvoiceItem item = invoiceService.getInvoiceItem(invoiceId, itemId, user);
        InvoiceItemResponse itemResponse = invoiceService.toInvoiceItemResponse(item);

        InvoiceItemDetailResponse response = new InvoiceItemDetailResponse(
            "Invoice item retrieved successfully",
            itemResponse
        );

        return ResponseEntity.ok(response);
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

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        Invoice updatedInvoice = invoiceService.deleteInvoiceItem(invoiceId, itemId, user);

        return ResponseEntity.ok(Map.of(
            "message", "Invoice item deleted successfully",
            "invoiceId", invoiceId,
            "itemId", itemId,
            "invoiceTotalAmount", updatedInvoice.getTotalAmount()
        ));
    }

    /**
     * Updates the category of an invoice item.
     *
     * @param invoiceId the invoice ID.
     * @param itemId the invoice item ID.
     * @param request the update category request.
     * @param userDetails the authenticated user details.
     * @return a response with the updated invoice item information.
     */
    @PutMapping("/{invoiceId}/items/{itemId}/category")
    public ResponseEntity<InvoiceItemDetailResponse> updateInvoiceItemCategory(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateInvoiceItemCategoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        InvoiceItem updatedItem =
                invoiceService.updateInvoiceItemCategory(
                        invoiceId, itemId, request.categoryId(), user);
        InvoiceItemResponse itemResponse = invoiceService.toInvoiceItemResponse(updatedItem);

        InvoiceItemDetailResponse response = new InvoiceItemDetailResponse(
            "Invoice item category updated successfully",
            itemResponse
        );

        return ResponseEntity.ok(response);
    }
}