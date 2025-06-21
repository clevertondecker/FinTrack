package com.fintrack.controller.creditcard;

import com.fintrack.domain.user.UserRepository;
import com.fintrack.dto.creditcard.CreateCreditCardRequest;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

/**
 * REST controller for managing credit cards.
 * Provides endpoints for credit card operations.
 */
@RestController
@RequestMapping("/api/credit-cards")
public class CreditCardController {

    private final CreditCardJpaRepository creditCardRepository;
    private final UserRepository userRepository;
    private final BankJpaRepository bankRepository;

    /**
     * Constructor for CreditCardController.
     *
     * @param theCreditCardRepository the credit card repository. Must not be null.
     * @param theUserRepository the user repository. Must not be null.
     * @param theBankRepository the bank repository. Must not be null.
     */
    public CreditCardController(CreditCardJpaRepository theCreditCardRepository,
                                UserRepository theUserRepository,
                                BankJpaRepository theBankRepository) {
        this.creditCardRepository = theCreditCardRepository;
        this.userRepository = theUserRepository;
        this.bankRepository = theBankRepository;
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

        Optional<User> userOpt = getUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the bank
        Optional<Bank> bankOpt = bankRepository.findById(request.bankId());
        if (bankOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bank not found"));
        }
        Bank bank = bankOpt.get();

        // Create the credit card
        CreditCard creditCard = CreditCard.of(
            request.name(),
            request.lastFourDigits(),
            request.limit(),
            user,
            bank
        );

        // Save the credit card
        CreditCard savedCreditCard = creditCardRepository.save(creditCard);

        return ResponseEntity.ok(Map.of(
            "message", "Credit card created successfully",
            "id", savedCreditCard.getId(),
            "name", savedCreditCard.getName(),
            "lastFourDigits", savedCreditCard.getLastFourDigits(),
            "limit", savedCreditCard.getLimit(),
            "bankName", savedCreditCard.getBank().getName()
        ));
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

        Optional<User> userOpt = getUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Get all credit cards for the user
        List<CreditCard> creditCards = creditCardRepository.findByOwner(user);

        // Convert to DTO format
        List<Map<String, Object>> creditCardDtos = new ArrayList<>();
        for (CreditCard card : creditCards) {
            creditCardDtos.add(Map.of(
                "id", card.getId(),
                "name", card.getName(),
                "lastFourDigits", card.getLastFourDigits(),
                "limit", card.getLimit(),
                "bankName", card.getBank().getName(),
                "active", card.isActive(),
                "createdAt", card.getCreatedAt()
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Credit cards retrieved successfully",
            "creditCards", creditCardDtos,
            "count", creditCardDtos.size()
        ));
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

        Optional<User> userOpt = getUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the credit card
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(id, user);
        if (creditCardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CreditCard creditCard = creditCardOpt.get();

        return ResponseEntity.ok(Map.of(
            "message", "Credit card retrieved successfully",
            "creditCard", Map.of(
                "id", creditCard.getId(),
                "name", creditCard.getName(),
                "lastFourDigits", creditCard.getLastFourDigits(),
                "limit", creditCard.getLimit(),
                "bankName", creditCard.getBank().getName(),
                "bankCode", creditCard.getBank().getCode(),
                "active", creditCard.isActive(),
                "createdAt", creditCard.getCreatedAt(),
                "updatedAt", creditCard.getUpdatedAt()
            )
        ));
    }

    private Optional<User> getUser(final Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }

        String username = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        }

        if (username == null) {
            return Optional.empty();
        }

        try {
            Email email = Email.of(username);
            return userRepository.findByEmail(email);
        } catch (Exception e) {
            return Optional.empty();
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

        if (authentication == null || authentication.getName() == null || authentication.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        Optional<User> userOpt = userRepository.findByEmail(Email.of(authentication.getName()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(id, user);
        if (creditCardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CreditCard creditCard = creditCardOpt.get();

        creditCard.activate();
        creditCardRepository.save(creditCard);

        return ResponseEntity.ok(Map.of(
            "message", "Credit card activated successfully",
            "id", creditCard.getId()
        ));
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

        Optional<User> userOpt = getUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        // Find the credit card
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(id, user);
        if (creditCardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CreditCard creditCard = creditCardOpt.get();

        // Deactivate the credit card
        creditCard.deactivate();
        creditCardRepository.save(creditCard);

        return ResponseEntity.ok(Map.of(
            "message", "Credit card deactivated successfully",
            "id", creditCard.getId()
        ));
    }

    /**
     * Updates an existing credit card for the authenticated user.
     *
     * @param id the credit card ID. Cannot be null.
     * @param request the credit card update request. Must be valid.
     * @param authentication the authenticated user details. Cannot be null.
     *
     * @return a response with the updated credit card information.
     *
     * @throws IllegalArgumentException if the request is invalid.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCreditCard(
            @PathVariable Long id,
            @Valid @RequestBody CreateCreditCardRequest request,
            Authentication authentication) {

        Optional<User> userOpt = getUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();

        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(id, user);
        if (creditCardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CreditCard creditCard = creditCardOpt.get();

        // Note: The last four digits cannot be updated, so we do not set it here.
        creditCard.updateName(request.name());
        creditCard.updateLimit(request.limit());

        Optional<Bank> bankOpt = bankRepository.findById(request.bankId());
        if (bankOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bank not found"));
        }
        creditCard.updateBank(bankOpt.get());

        CreditCard updatedCard = creditCardRepository.save(creditCard);

        return ResponseEntity.ok(Map.of(
            "message", "Credit card updated successfully",
            "id", updatedCard.getId(),
            "name", updatedCard.getName(),
            "lastFourDigits", updatedCard.getLastFourDigits(),
            "limit", updatedCard.getLimit(),
            "bankName", updatedCard.getBank().getName(),
            "active", updatedCard.isActive(),
            "createdAt", updatedCard.getCreatedAt(),
            "updatedAt", updatedCard.getUpdatedAt()
        ));
    }
}