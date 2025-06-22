package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.CreditCardService;
import com.fintrack.dto.creditcard.CreateCreditCardRequest;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing credit cards.
 * Provides endpoints for credit card operations.
 */
@RestController
@RequestMapping("/api/credit-cards")
public class CreditCardController {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    /**
     * Creates a new credit card for the authenticated user.
     *
     * @param request the credit card creation request. Must be valid.
     * @param authentication the authenticated user details.
     * @return a response with the created credit card information.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCreditCard(
            @Valid @RequestBody CreateCreditCardRequest request,
            Authentication authentication) {

        try {
            Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Create the credit card using service
            CreditCard creditCard = creditCardService.createCreditCard(request, user);

            return ResponseEntity.ok(Map.of(
                "message", "Credit card created successfully",
                "id", creditCard.getId(),
                "name", creditCard.getName(),
                "lastFourDigits", creditCard.getLastFourDigits(),
                "limit", creditCard.getLimit(),
                "bankName", creditCard.getBank().getName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Gets all credit cards for the authenticated user.
     *
     * @param authentication the authenticated user details.
     * @return a response with the user's credit cards.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserCreditCards(
            Authentication authentication) {

        try {
            Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Get credit cards using service
            List<CreditCard> creditCards = creditCardService.getUserCreditCards(user);
            List<Map<String, Object>> creditCardDtos = creditCardService.toCreditCardDtos(creditCards);

            return ResponseEntity.ok(Map.of(
                "message", "Credit cards retrieved successfully",
                "creditCards", creditCardDtos,
                "count", creditCardDtos.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Gets a specific credit card by ID for the authenticated user.
     *
     * @param id the credit card ID.
     * @param authentication the authenticated user details.
     * @return a response with the credit card information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCreditCard(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Get credit card using service
            CreditCard creditCard = creditCardService.getCreditCard(id, user);
            Map<String, Object> creditCardDto = creditCardService.toCreditCardDto(creditCard);

            return ResponseEntity.ok(Map.of(
                "message", "Credit card retrieved successfully",
                "creditCard", creditCardDto
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Activates a credit card for the authenticated user.
     *
     * @param id the credit card ID. Cannot be null.
     * @param authentication the authenticated user details. Cannot be null.
     *
     * @return a response confirming the activation. Never null.
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateCreditCard(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Activate credit card using service
            CreditCard creditCard = creditCardService.activateCreditCard(id, user);

            return ResponseEntity.ok(Map.of(
                "message", "Credit card activated successfully",
                "id", creditCard.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Deactivates a credit card for the authenticated user.
     *
     * @param id the credit card ID.
     * @param authentication the authenticated user details.
     *
     * @return a response confirming the deactivation.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deactivateCreditCard(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Deactivate credit card using service
            CreditCard creditCard = creditCardService.deactivateCreditCard(id, user);

            return ResponseEntity.ok(Map.of(
                "message", "Credit card deactivated successfully",
                "id", creditCard.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Updates a credit card for the authenticated user.
     *
     * @param id the credit card ID.
     * @param request the credit card update request. Must be valid.
     * @param authentication the authenticated user details.
     *
     * @return a response with the updated credit card information.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCreditCard(
            @PathVariable Long id,
            @Valid @RequestBody CreateCreditCardRequest request,
            Authentication authentication) {

        try {
            Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Update credit card using service
            CreditCard creditCard = creditCardService.updateCreditCard(id, request, user);

            return ResponseEntity.ok(Map.of(
                "message", "Credit card updated successfully",
                "id", creditCard.getId(),
                "name", creditCard.getName(),
                "lastFourDigits", creditCard.getLastFourDigits(),
                "limit", creditCard.getLimit(),
                "bankName", creditCard.getBank().getName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}