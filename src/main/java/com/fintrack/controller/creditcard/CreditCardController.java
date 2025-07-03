package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.CreditCardService;
import com.fintrack.dto.creditcard.CreateCreditCardRequest;
import com.fintrack.dto.creditcard.CreditCardCreateResponse;
import com.fintrack.dto.creditcard.CreditCardListResponse;
import com.fintrack.dto.creditcard.CreditCardDetailResponse;
import com.fintrack.dto.creditcard.CreditCardActionResponse;
import com.fintrack.dto.creditcard.CreditCardResponse;
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
    public ResponseEntity<CreditCardCreateResponse> createCreditCard(
            @Valid @RequestBody CreateCreditCardRequest request,
            Authentication authentication) {

        Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Create the credit card using service
        CreditCard creditCard = creditCardService.createCreditCard(request, user);

        CreditCardCreateResponse response = new CreditCardCreateResponse(
            "Credit card created successfully",
            creditCard.getId(),
            creditCard.getName(),
            creditCard.getLastFourDigits(),
            creditCard.getLimit(),
            creditCard.getBank().getName(),
            creditCard.getCardType(),
            creditCard.getCardholderName()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets all credit cards for the authenticated user.
     *
     * @param authentication the authenticated user details.
     * @return a response with the user's credit cards.
     */
    @GetMapping
    public ResponseEntity<CreditCardListResponse> getUserCreditCards(
            Authentication authentication) {

        Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Get credit cards using service
        List<CreditCard> creditCards = creditCardService.getUserCreditCards(user);
        
        // Convert to CreditCardResponse list
        List<CreditCardResponse> creditCardResponses = creditCardService.toCreditCardResponseList(creditCards);

        CreditCardListResponse response = new CreditCardListResponse(
            "Credit cards retrieved successfully",
            creditCardResponses,
            creditCardResponses.size()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets a specific credit card by ID for the authenticated user.
     *
     * @param id the credit card ID.
     * @param authentication the authenticated user details.
     * @return a response with the credit card information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CreditCardDetailResponse> getCreditCard(
            @PathVariable Long id,
            Authentication authentication) {

        Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Get credit card using service
        CreditCard creditCard = creditCardService.getCreditCard(id, user);
        CreditCardResponse creditCardResponse = creditCardService.toCreditCardResponse(creditCard);

        CreditCardDetailResponse response = new CreditCardDetailResponse(
            "Credit card retrieved successfully",
            creditCardResponse
        );

        return ResponseEntity.ok(response);
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
    public ResponseEntity<CreditCardActionResponse> activateCreditCard(
            @PathVariable Long id,
            Authentication authentication) {

        Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Activate credit card using service
        CreditCard creditCard = creditCardService.activateCreditCard(id, user);

        CreditCardActionResponse response = new CreditCardActionResponse(
            "Credit card activated successfully",
            creditCard.getId()
        );

        return ResponseEntity.ok(response);
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
    public ResponseEntity<CreditCardActionResponse> deactivateCreditCard(
            @PathVariable Long id,
            Authentication authentication) {

        Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Deactivate credit card using service
        CreditCard creditCard = creditCardService.deactivateCreditCard(id, user);

        CreditCardActionResponse response = new CreditCardActionResponse(
            "Credit card deactivated successfully",
            creditCard.getId()
        );

        return ResponseEntity.ok(response);
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
    public ResponseEntity<CreditCardCreateResponse> updateCreditCard(
            @PathVariable Long id,
            @Valid @RequestBody CreateCreditCardRequest request,
            Authentication authentication) {

        Optional<User> userOpt = creditCardService.findUserByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Update credit card using service
        CreditCard creditCard = creditCardService.updateCreditCard(id, request, user);

        CreditCardCreateResponse response = new CreditCardCreateResponse(
            "Credit card updated successfully",
            creditCard.getId(),
            creditCard.getName(),
            creditCard.getLastFourDigits(),
            creditCard.getLimit(),
            creditCard.getBank().getName(),
            creditCard.getCardType(),
            creditCard.getCardholderName()
        );

        return ResponseEntity.ok(response);
    }
}