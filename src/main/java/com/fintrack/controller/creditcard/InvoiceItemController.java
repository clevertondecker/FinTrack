package com.fintrack.controller.creditcard;

import com.fintrack.domain.user.UserRepository;
import com.fintrack.dto.creditcard.CreateInvoiceItemRequest;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceItemJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDate;

/**
 * REST controller for managing invoice items.
 * Provides endpoints for invoice item operations.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceItemController {

    private final InvoiceItemJpaRepository invoiceItemRepository;
    private final InvoiceJpaRepository invoiceRepository;
    private final UserRepository userRepository;
    private final CategoryJpaRepository categoryRepository;

    public InvoiceItemController(InvoiceItemJpaRepository invoiceItemRepository,
                                InvoiceJpaRepository invoiceRepository,
                                UserRepository userRepository,
                                CategoryJpaRepository categoryRepository) {
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
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
            Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Find the invoice
            Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndCreditCardOwner(invoiceId, user);
            if (invoiceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invoice not found"));
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

            // Add item to invoice first (this will recalculate the total)
            invoice.addItem(invoiceItem);

            // Save the invoice (this will cascade to save the item)
            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Return a simple success response
            return ResponseEntity.ok(Map.of(
                "message", "Invoice item created successfully",
                "id", invoiceItem.getId() != null ? invoiceItem.getId() : 0L,
                "invoiceId", invoiceId,
                "description", invoiceItem.getDescription(),
                "amount", invoiceItem.getAmount(),
                "category", invoiceItem.getCategory() != null ? invoiceItem.getCategory().getName() : null,
                "invoiceTotalAmount", savedInvoice.getTotalAmount()
            ));
        } catch (Exception e) {
            System.err.println("Error creating invoice item: " + e.getMessage());
            e.printStackTrace();
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

        // Find the user
        Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the invoice
        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndCreditCardOwner(invoiceId, user);
        if (invoiceOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invoice not found"));
        }
        Invoice invoice = invoiceOpt.get();

        // Get items for the invoice
        List<InvoiceItem> items = invoiceItemRepository.findByInvoice(invoice);

        // Convert to DTO format
        List<Map<String, Object>> itemDtos = new ArrayList<>();
        for (InvoiceItem item : items) {
            Map<String, Object> itemDto = new HashMap<>();
            itemDto.put("id", item.getId());
            itemDto.put("description", item.getDescription());
            itemDto.put("amount", item.getAmount());
            itemDto.put("category", item.getCategory() != null ? item.getCategory().getName() : null);
            itemDto.put("purchaseDate", item.getPurchaseDate().toString());
            itemDtos.add(itemDto);
        }

        return ResponseEntity.ok(Map.of(
            "message", "Invoice items retrieved successfully",
            "invoiceId", invoiceId,
            "items", itemDtos,
            "count", itemDtos.size(),
            "totalAmount", invoice.getTotalAmount()
        ));
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

        // Find the user
        Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the invoice item
        Optional<InvoiceItem> itemOpt = invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(itemId, user);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InvoiceItem item = itemOpt.get();

        // Verify the item belongs to the specified invoice
        if (!item.getInvoice().getId().equals(invoiceId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Item does not belong to the specified invoice"));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Invoice item retrieved successfully",
            "item", Map.of(
                "id", item.getId(),
                "invoiceId", item.getInvoice().getId(),
                "description", item.getDescription(),
                "amount", item.getAmount(),
                "category", item.getCategory() != null ? item.getCategory().getName() : null,
                "createdAt", item.getCreatedAt().toLocalDate().toString()
            )
        ));
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

        // Find the user
        Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the invoice item
        Optional<InvoiceItem> itemOpt = invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(itemId, user);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InvoiceItem item = itemOpt.get();

        // Verify the item belongs to the specified invoice
        if (!item.getInvoice().getId().equals(invoiceId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Item does not belong to the specified invoice"));
        }

        // Remove item from invoice and save
        Invoice invoice = item.getInvoice();
        invoice.removeItem(item);
        invoiceRepository.save(invoice);

        return ResponseEntity.ok(Map.of(
            "message", "Invoice item deleted successfully",
            "deletedItemId", itemId,
            "invoiceId", invoiceId
        ));
    }
}