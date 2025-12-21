package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.Email;
import com.fintrack.dto.creditcard.CreateCreditCardRequest;
import com.fintrack.dto.creditcard.CreditCardResponse;
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

    /**
     * Constructs a new CreditCardService.
     *
     * @param theCreditCardRepository the credit card repository. Must not be null.
     * @param theBankRepository the bank repository. Must not be null.
     * @param theUserRepository the user repository. Must not be null.
     */
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
     * @throws IllegalArgumentException if bank not found or parent card not found.
     */
    public CreditCard createCreditCard(CreateCreditCardRequest request, User user) {
        Bank bank = findBankById(request.bankId());
        CreditCard parentCard = findParentCardIfSpecified(request.parentCardId(), user);

        CreditCard creditCard = CreditCard.of(
            request.name(),
            request.lastFourDigits(),
            request.limit(),
            user,
            bank,
            request.cardType(),
            parentCard,
            request.cardholderName()
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
     * @throws IllegalArgumentException if credit card, bank, or parent card not found.
     */
    public CreditCard updateCreditCard(Long creditCardId, CreateCreditCardRequest request, User user) {
        CreditCard creditCard = getCreditCard(creditCardId, user);
        Bank bank = findBankById(request.bankId());
        CreditCard parentCard = findParentCardIfSpecified(request.parentCardId(), user);

        updateCreditCardFields(creditCard, request, bank, parentCard);
        return creditCardRepository.save(creditCard);
    }

    /**
     * Converts a CreditCard to a CreditCardResponse DTO.
     *
     * @param creditCard the credit card to convert. Cannot be null.
     * @return CreditCardResponse representation of the credit card. Never null.
     * @throws IllegalArgumentException if creditCard is null.
     */
    public CreditCardResponse toCreditCardResponse(CreditCard creditCard) {
        if (creditCard == null) {
            throw new IllegalArgumentException("Credit card cannot be null");
        }
        
        return new CreditCardResponse(
            creditCard.getId(),
            creditCard.getName(),
            creditCard.getLastFourDigits(),
            creditCard.getLimit(),
            creditCard.isActive(),
            creditCard.getBank().getName(),
            creditCard.getCardType(),
            extractParentCardId(creditCard),
            extractParentCardName(creditCard),
            creditCard.getCardholderName(),
            creditCard.getCreatedAt(),
            creditCard.getUpdatedAt()
        );
    }

    /**
     * Converts a CreditCard to a Map DTO.
     * @deprecated Use toCreditCardResponse instead for better type safety.
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
        dto.put("cardType", creditCard.getCardType());
        dto.put("parentCardId", creditCard.getParentCard() != null ? creditCard.getParentCard().getId() : null);
        dto.put("parentCardName", creditCard.getParentCard() != null ? creditCard.getParentCard().getName() : null);
        dto.put("cardholderName", creditCard.getCardholderName());
        dto.put("createdAt", creditCard.getCreatedAt());
        dto.put("updatedAt", creditCard.getUpdatedAt());
        return dto;
    }

    /**
     * Converts a list of credit cards to CreditCardResponse DTOs.
     *
     * @param creditCards the list of credit cards to convert. Cannot be null.
     * @return list of CreditCardResponse DTOs. Never null, may be empty.
     * @throws IllegalArgumentException if creditCards is null.
     */
    public List<CreditCardResponse> toCreditCardResponseList(List<CreditCard> creditCards) {
        if (creditCards == null) {
            throw new IllegalArgumentException("Credit cards list cannot be null");
        }
        
        return creditCards.stream()
            .map(this::toCreditCardResponse)
            .toList();
    }

    /**
     * Converts a list of credit cards to DTOs.
     * @deprecated Use toCreditCardResponseList instead for better type safety.
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

    /**
     * Finds a bank by ID.
     *
     * @param bankId the bank ID. Cannot be null.
     * @return the bank. Never null.
     * @throws IllegalArgumentException if bank not found.
     */
    private Bank findBankById(Long bankId) {
        Optional<Bank> bankOpt = bankRepository.findById(bankId);
        if (bankOpt.isEmpty()) {
            throw new IllegalArgumentException("Bank not found");
        }
        return bankOpt.get();
    }

    /**
     * Finds a parent card if specified.
     *
     * @param parentCardId the parent card ID. Can be null.
     * @param user the authenticated user. Cannot be null.
     * @return the parent card, or null if not specified.
     * @throws IllegalArgumentException if parent card not found.
     */
    private CreditCard findParentCardIfSpecified(Long parentCardId, User user) {
        if (parentCardId == null) {
            return null;
        }
        
        Optional<CreditCard> parentCardOpt = creditCardRepository.findByIdAndOwner(parentCardId, user);
        if (parentCardOpt.isEmpty()) {
            throw new IllegalArgumentException("Parent card not found");
        }
        return parentCardOpt.get();
    }

    /**
     * Updates credit card fields with new values.
     *
     * @param creditCard the credit card to update. Cannot be null.
     * @param request the update request. Cannot be null.
     * @param bank the bank. Cannot be null.
     * @param parentCard the parent card. Can be null.
     */
    private void updateCreditCardFields(CreditCard creditCard, CreateCreditCardRequest request, Bank bank, CreditCard parentCard) {
        creditCard.updateName(request.name());
        creditCard.updateLimit(request.limit());
        creditCard.updateBank(bank);
        creditCard.updateCardType(request.cardType());
        creditCard.updateParentCard(parentCard);
        creditCard.updateCardholderName(request.cardholderName());
    }

    /**
     * Extracts parent card ID safely.
     *
     * @param creditCard the credit card. Cannot be null.
     * @return the parent card ID, or null if no parent card.
     */
    private Long extractParentCardId(CreditCard creditCard) {
        return creditCard.getParentCard() != null ? creditCard.getParentCard().getId() : null;
    }

    /**
     * Extracts parent card name safely.
     *
     * @param creditCard the credit card. Cannot be null.
     * @return the parent card name, or null if no parent card.
     */
    private String extractParentCardName(CreditCard creditCard) {
        return creditCard.getParentCard() != null ? creditCard.getParentCard().getName() : null;
    }
}