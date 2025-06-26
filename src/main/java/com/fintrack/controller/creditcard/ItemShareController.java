package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.ExpenseSharingServiceImpl;
import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing item shares.
 * Provides endpoints for item share operations.
 */
@RestController
@RequestMapping("/api/invoices")
public class ItemShareController {

    private final ExpenseSharingServiceImpl expenseSharingService;
    private final InvoiceService invoiceService;

    public ItemShareController(ExpenseSharingServiceImpl expenseSharingService, InvoiceService invoiceService) {
        this.expenseSharingService = expenseSharingService;
        this.invoiceService = invoiceService;
    }

    /**
     * Creates shares for an invoice item.
     *
     * @param invoiceId the invoice ID from the path.
     * @param itemId the invoice item ID from the path.
     * @param request the item share creation request. Must be valid.
     * @param userDetails the authenticated user details.
     * @return a response with the created item shares information.
     */
    @PostMapping("/{invoiceId}/items/{itemId}/shares")
    public ResponseEntity<ItemShareCreateResponse> createItemShares(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            @Valid @RequestBody CreateItemShareRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Get the invoice item
        InvoiceItem item = invoiceService.getInvoiceItem(invoiceId, itemId, user);

        // Create shares
        List<ItemShare> shares =
          expenseSharingService.createSharesFromUserIds(item, request.userShares());

        // Convert to response DTOs
        List<ItemShareResponse> shareResponses = shares.stream()
            .map(this::toItemShareResponse)
            .toList();

        ItemShareCreateResponse response = new ItemShareCreateResponse(
            "Item shares created successfully",
            invoiceId,
            itemId,
            item.getDescription(),
            item.getAmount(),
            shareResponses,
            shareResponses.size(),
            item.getSharedAmount(),
            item.getUnsharedAmount()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets all shares for a specific invoice item.
     *
     * @param invoiceId the invoice ID.
     * @param itemId the invoice item ID.
     * @param userDetails the authenticated user details.
     * @return a response with the item shares.
     */
    @GetMapping("/{invoiceId}/items/{itemId}/shares")
    @Transactional
    public ResponseEntity<ItemShareListResponse> getItemShares(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Get the invoice item
        InvoiceItem item = invoiceService.getInvoiceItem(invoiceId, itemId, user);

        // Get shares for the item
        List<ItemShare> shares = expenseSharingService.getSharesForItem(item);

        // Convert to response DTOs
        List<ItemShareResponse> shareResponses = shares.stream()
            .map(this::toItemShareResponse)
            .toList();

        ItemShareListResponse response = new ItemShareListResponse(
            "Item shares retrieved successfully",
            invoiceId,
            itemId,
            item.getDescription(),
            item.getAmount(),
            shareResponses,
            shareResponses.size(),
            item.getSharedAmount(),
            item.getUnsharedAmount()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Removes all shares from an invoice item.
     *
     * @param invoiceId the invoice ID.
     * @param itemId the invoice item ID.
     * @param userDetails the authenticated user details.
     * @return a response confirming the removal.
     */
    @DeleteMapping("/{invoiceId}/items/{itemId}/shares")
    public ResponseEntity<Map<String, Object>> removeItemShares(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Get the invoice item
        InvoiceItem item = invoiceService.getInvoiceItem(invoiceId, itemId, user);

        // Remove shares
        expenseSharingService.removeShares(item);

        return ResponseEntity.ok(Map.of(
            "message", "Item shares removed successfully",
            "invoiceId", invoiceId,
            "itemId", itemId,
            "itemDescription", item.getDescription()
        ));
    }

    /**
     * Gets all shares for the current user.
     *
     * @param userDetails the authenticated user details.
     * @return a response with the user's shares.
     */
    @GetMapping("/shares/my-shares")
    public ResponseEntity<MySharesResponse> getMyShares(
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Get all shares for the user
        List<ItemShare> userShares = expenseSharingService.getSharesForUser(user);
        
        // Convert to response DTOs
        List<MyShareResponse> shareResponses = userShares.stream()
            .map(this::toMyShareResponse)
            .toList();

        MySharesResponse response = new MySharesResponse(
            "User shares retrieved successfully",
            shareResponses,
            shareResponses.size()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Converts an ItemShare to an ItemShareResponse DTO.
     *
     * @param share the item share to convert.
     * @return the ItemShareResponse DTO.
     */
    private ItemShareResponse toItemShareResponse(ItemShare share) {
        ItemShareResponse response = new ItemShareResponse(
            share.getId(),
            share.getUser().getId(),
            share.getUser().getName(),
            share.getUser().getEmail().getEmail(),
            share.getPercentage(),
            share.getAmount(),
            Boolean.valueOf(share.isResponsible()),
            share.getCreatedAt()
        );
        
        return response;
    }

    private MyShareResponse toMyShareResponse(ItemShare share) {
        InvoiceItem item = share.getInvoiceItem();
        Invoice invoice = item.getInvoice();
        CreditCard creditCard = invoice.getCreditCard();
        
        return new MyShareResponse(
            share.getId(),
            invoice.getId(),
            item.getId(),
            item.getDescription(),
            item.getAmount(),
            share.getAmount(),
            share.getPercentage(),
            share.isResponsible(),
            creditCard.getName(),
            creditCard.getOwner().getName(),
            invoice.getDueDate(),
            invoice.getStatus().name(),
            share.getCreatedAt()
        );
    }
}