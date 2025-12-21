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
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
     * Marks a share as paid.
     *
     * @param shareId the share ID.
     * @param request the payment details.
     * @param userDetails the authenticated user details.
     * @return a response confirming the payment.
     */
    @PostMapping("/shares/{shareId}/mark-as-paid")
    @Transactional
    public ResponseEntity<ItemShareResponse> markShareAsPaid(
            @PathVariable Long shareId,
            @Valid @RequestBody MarkShareAsPaidRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        ItemShare updatedShare = expenseSharingService.markShareAsPaid(
            shareId, request.paymentMethod(), request.paidAt(), user);

        ItemShareResponse response = toItemShareResponse(updatedShare);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks a share as unpaid.
     *
     * @param shareId the share ID.
     * @param userDetails the authenticated user details.
     * @return a response confirming the change.
     */
    @PostMapping("/shares/{shareId}/mark-as-unpaid")
    @Transactional
    public ResponseEntity<ItemShareResponse> markShareAsUnpaid(
            @PathVariable Long shareId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        ItemShare updatedShare = expenseSharingService.markShareAsUnpaid(shareId, user);

        ItemShareResponse response = toItemShareResponse(updatedShare);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk marks multiple shares as paid.
     */
    public record BulkMarkSharesAsPaidRequest(
        List<Long> shareIds,
        String paymentMethod,
        LocalDateTime paidAt
    ) {}

    @PostMapping("/shares/mark-as-paid-bulk")
    @Transactional
    public ResponseEntity<java.util.Map<String, Object>> markSharesAsPaidBulk(
            @Valid @RequestBody BulkMarkSharesAsPaidRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        List<ItemShare> updated = expenseSharingService.markSharesAsPaidBulk(
            request.shareIds(), request.paymentMethod(), request.paidAt(), user);

        List<ItemShareResponse> responses = updated.stream()
            .map(this::toItemShareResponse)
            .toList();

        return ResponseEntity.ok(java.util.Map.of(
            "message", "Shares marked as paid successfully",
            "updatedCount", responses.size(),
            "updatedShares", responses
        ));
    }

    /**
     * Converts an ItemShare to an ItemShareResponse DTO.
     *
     * @param share the item share to convert.
     * @return the ItemShareResponse DTO.
     */
    private ItemShareResponse toItemShareResponse(ItemShare share) {

      return new ItemShareResponse(
          share.getId(),
          share.getUser().getId(),
          share.getUser().getName(),
          share.getUser().getEmail().getEmail(),
          share.getPercentage(),
          share.getAmount(),
          share.isResponsible(),
          share.isPaid(),
          share.getPaymentMethod(),
          share.getPaidAt(),
          share.getCreatedAt()
      );
    }

    private MyShareResponse toMyShareResponse(ItemShare share) {
        InvoiceItem item = share.getInvoiceItem();
        Invoice invoice = item.getInvoice();
        CreditCard creditCard = invoice.getCreditCard();
        
        int remainingInstallments =
            Math.max(0, item.getTotalInstallments() - item.getInstallments());
        BigDecimal totalItemAmount =
            item.getAmount().multiply(BigDecimal.valueOf(item.getTotalInstallments()));
        BigDecimal remainingItemAmount =
            item.getAmount().multiply(BigDecimal.valueOf(remainingInstallments));
        
        return new MyShareResponse(
            share.getId(),
            invoice.getId(),
            item.getId(),
            item.getDescription(),
            item.getAmount(),
            share.getAmount(),
            share.getPercentage(),
            share.isResponsible(),
            share.isPaid(),
            share.getPaymentMethod(),
            share.getPaidAt(),
            creditCard.getName(),
            creditCard.getOwner().getName(),
            invoice.getDueDate(),
            invoice.getStatus().name(),
            share.getCreatedAt(),
            item.getInstallments(),
            item.getTotalInstallments(),
            remainingInstallments,
            totalItemAmount,
            remainingItemAmount
        );
    }

    /**
     * Recalculates all shares to fix rounding issues.
     * This endpoint recalculates all existing shares, ensuring the sum of shares
     * equals the item amount exactly by adjusting the last share.
     *
     * @param userDetails the authenticated user details.
     * @return a response with the number of items recalculated.
     */
    @PostMapping("/shares/recalculate-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> recalculateAllShares(
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        int recalculated = expenseSharingService.recalculateAllShares();

        Map<String, Object> response = Map.of(
            "message", "Shares recalculated successfully",
            "itemsRecalculated", recalculated
        );

        return ResponseEntity.ok(response);
    }
}