package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.Email;
import com.fintrack.dto.creditcard.CreateCreditCardRequest;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service for managing credit card-related business logic.
 * Provides operations for credit cards.
 */
@Service
@Transactional
public class CreditCardService {

    private final CreditCardJpaRepository creditCardRepository;
    private final BankJpaRepository bankRepository;
    private final UserRepository userRepository;

    public CreditCardService(final CreditCardJpaRepository theCreditCardRepository,
                            final BankJpaRepository theBankRepository,
                            final UserRepository theUserRepository) {
        creditCardRepository = theCreditCardRepository;
        bankRepository = theBankRepository;
        userRepository = theUserRepository;
    }

    /**
     * Finds a user by email from Authentication.
     *
     * @param username the username (email) from Authentication. Can be null or empty.
     * @return an Optional containing the user if found, empty otherwise. Never null.
     */
    public Optional<User> findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
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
     * Creates a new credit card for a user.
     *
     * @param request the credit card creation request. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the created credit card. Never null.
     * @throws IllegalArgumentException if bank not found.
     */
    public CreditCard createCreditCard(CreateCreditCardRequest request, User user) {
        // Find the bank
        Optional<Bank> bankOpt = bankRepository.findById(request.bankId());
        if (bankOpt.isEmpty()) {
            throw new IllegalArgumentException("Bank not found");
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

        return creditCardRepository.save(creditCard);
    }

    /**
     * Gets all credit cards for a user.
     *
     * @param user the authenticated user. Cannot be null.
     * @return list of credit cards. Never null, may be empty.
     */
    public List<CreditCard> getUserCreditCards(User user) {
        return creditCardRepository.findByOwner(user);
    }

    /**
     * Gets a specific credit card by ID for a user.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the credit card. Never null.
     * @throws IllegalArgumentException if credit card not found or doesn't belong to user.
     */
    public CreditCard getCreditCard(Long creditCardId, User user) {
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(creditCardId, user);
        if (creditCardOpt.isEmpty()) {
            throw new IllegalArgumentException("Credit card not found");
        }
        return creditCardOpt.get();
    }

    /**
     * Activates a credit card for a user.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the activated credit card. Never null.
     * @throws IllegalArgumentException if credit card not found or doesn't belong to user.
     */
    public CreditCard activateCreditCard(Long creditCardId, User user) {
        CreditCard creditCard = getCreditCard(creditCardId, user);
        creditCard.activate();
        return creditCardRepository.save(creditCard);
    }

    /**
     * Deactivates a credit card for a user.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the deactivated credit card. Never null.
     * @throws IllegalArgumentException if credit card not found or doesn't belong to user.
     */
    public CreditCard deactivateCreditCard(Long creditCardId, User user) {
        CreditCard creditCard = getCreditCard(creditCardId, user);
        creditCard.deactivate();
        return creditCardRepository.save(creditCard);
    }

    /**
     * Updates a credit card for a user.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param request the credit card update request. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the updated credit card. Never null.
     * @throws IllegalArgumentException if credit card or bank not found.
     */
    public CreditCard updateCreditCard(Long creditCardId, CreateCreditCardRequest request, User user) {
        // Find the credit card
        CreditCard creditCard = getCreditCard(creditCardId, user);

        // Find the bank
        Optional<Bank> bankOpt = bankRepository.findById(request.bankId());
        if (bankOpt.isEmpty()) {
            throw new IllegalArgumentException("Bank not found");
        }
        Bank bank = bankOpt.get();

        // Update the credit card using specific update methods
        creditCard.updateName(request.name());
        creditCard.updateLimit(request.limit());
        creditCard.updateBank(bank);

        return creditCardRepository.save(creditCard);
    }

    /**
     * Converts a CreditCard to a Map DTO.
     *
     * @param creditCard the credit card to convert. Cannot be null.
     * @return Map representation of the credit card. Never null.
     */
    public Map<String, Object> toCreditCardDto(CreditCard creditCard) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", creditCard.getId());
        dto.put("name", creditCard.getName());
        dto.put("lastFourDigits", creditCard.getLastFourDigits());
        dto.put("limit", creditCard.getLimit());
        dto.put("bankName", creditCard.getBank().getName());
        dto.put("bankCode", creditCard.getBank().getCode());
        dto.put("active", creditCard.isActive());
        dto.put("createdAt", creditCard.getCreatedAt());
        dto.put("updatedAt", creditCard.getUpdatedAt());
        return dto;
    }

    /**
     * Converts a list of credit cards to DTOs.
     *
     * @param creditCards the list of credit cards to convert. Cannot be null.
     * @return list of credit card DTOs. Never null, may be empty.
     */
    public List<Map<String, Object>> toCreditCardDtos(List<CreditCard> creditCards) {
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (CreditCard creditCard : creditCards) {
            dtos.add(toCreditCardDto(creditCard));
        }
        return dtos;
    }
}