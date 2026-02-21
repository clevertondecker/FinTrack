package com.fintrack.application.creditcard;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.Email;
import com.fintrack.dto.creditcard.CreateCreditCardRequest;
import com.fintrack.dto.creditcard.CreditCardGroupResponse;
import com.fintrack.dto.creditcard.CreditCardResponse;
import com.fintrack.infrastructure.persistence.contact.TrustedContactJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fintrack.domain.creditcard.CardType;

/**
 * Service for managing credit card-related business logic.
 * Provides operations for credit cards.
 */
@Service
@Transactional
public class CreditCardService {

    /** Placeholder for last four digits when parent card is not loaded. */
    private static final String MASKED_LAST_FOUR_DIGITS = "****";

    private final CreditCardJpaRepository creditCardRepository;
    private final BankJpaRepository bankRepository;
    private final UserRepository userRepository;
    private final TrustedContactJpaRepository trustedContactRepository;

    public CreditCardService(final CreditCardJpaRepository theCreditCardRepository,
                            final BankJpaRepository theBankRepository,
                            final UserRepository theUserRepository,
                            final TrustedContactJpaRepository theTrustedContactRepository) {
        creditCardRepository = theCreditCardRepository;
        bankRepository = theBankRepository;
        userRepository = theUserRepository;
        trustedContactRepository = theTrustedContactRepository;
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
        User assignedUser = findAssignedUserIfSpecified(request.assignedUserId());
        TrustedContact assignedContact = findAssignedContactIfSpecified(request.assignedContactId(), user);

        CreditCard creditCard = CreditCard.of(
            request.name(),
            request.lastFourDigits(),
            request.limit(),
            user,
            bank,
            request.cardType(),
            parentCard,
            request.cardholderName(),
            assignedUser,
            assignedContact
        );

        return creditCardRepository.save(creditCard);
    }

    /**
     * Gets all credit cards the user can see and manage: those they own and those whose parent they own.
     *
     * @param user the authenticated user. Cannot be null.
     * @param includeInactive whether to include inactive cards.
     * @return list of credit cards. Never null, may be empty.
     */
    public List<CreditCard> getUserCreditCards(User user, boolean includeInactive) {
        if (includeInactive) {
            return creditCardRepository.findByOwnerOrParentCardOwner(user);
        }
        return creditCardRepository.findByOwnerOrParentCardOwnerAndActiveTrue(user);
    }

    /**
     * Gets a credit card by ID. User must be the owner or the owner of the card's parent.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the credit card. Never null.
     * @throws IllegalArgumentException if credit card not found or access denied.
     */
    public CreditCard getCreditCard(Long creditCardId, User user) {
        return getCreditCardWithParentPermission(creditCardId, user);
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
     * The user can deactivate a card if they are the owner OR if they own the parent card.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the deactivated credit card. Never null.
     * @throws IllegalArgumentException if credit card not found or user doesn't have permission.
     */
    public CreditCard deactivateCreditCard(Long creditCardId, User user) {
        CreditCard creditCard = getCreditCardWithParentPermission(creditCardId, user);
        creditCard.deactivate();
        return creditCardRepository.save(creditCard);
    }

    /**
     * Gets a credit card by ID, allowing access if user owns the card OR owns the parent card.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param user the authenticated user. Cannot be null.
     * @return the credit card. Never null.
     * @throws IllegalArgumentException if credit card not found or user doesn't have permission.
     */
    private CreditCard getCreditCardWithParentPermission(Long creditCardId, User user) {
        // First, try to find by owner
        Optional<CreditCard> creditCardOpt = creditCardRepository.findByIdAndOwner(creditCardId, user);
        if (creditCardOpt.isPresent()) {
            return creditCardOpt.get();
        }
        
        // If not owner, check if user owns the parent card
        Optional<CreditCard> cardOpt = creditCardRepository.findById(creditCardId);
        if (cardOpt.isEmpty()) {
            throw new IllegalArgumentException("Credit card not found");
        }
        
        CreditCard card = cardOpt.get();
        CreditCard parentCard = card.getParentCard();
        
        if (parentCard != null && parentCard.getOwner().getId().equals(user.getId())) {
            return card;
        }
        
        throw new IllegalArgumentException("Credit card not found or access denied");
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
        CreditCard creditCard = getCreditCardWithParentPermission(creditCardId, user);
        Bank bank = findBankById(request.bankId());
        CreditCard parentCard = findParentCardIfSpecified(request.parentCardId(), user);
        User assignedUser = findAssignedUserIfSpecified(request.assignedUserId());
        TrustedContact assignedContact = findAssignedContactIfSpecified(request.assignedContactId(), user);

        updateCreditCardFields(creditCard, request, bank, parentCard, assignedUser, assignedContact);
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
        User assigned = creditCard.getAssignedUser();
        TrustedContact contact = creditCard.getAssignedContact();
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
            assigned != null ? assigned.getId() : null,
            assigned != null ? assigned.getName() : null,
            contact != null ? contact.getId() : null,
            contact != null ? contact.getName() : null,
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
        User assigned = creditCard.getAssignedUser();
        dto.put("assignedUserId", assigned != null ? assigned.getId() : null);
        dto.put("assignedUserName", assigned != null ? assigned.getName() : null);
        TrustedContact contact = creditCard.getAssignedContact();
        dto.put("assignedContactId", contact != null ? contact.getId() : null);
        dto.put("assignedContactName", contact != null ? contact.getName() : null);
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
     * Groups credit cards by their parent card. Parents missing from the list (e.g. inactive)
     * are loaded from DB in a single batch and included so children are still grouped.
     *
     * @param creditCards the list of credit cards to group. Cannot be null.
     * @return a list of grouped credit card responses. Never null.
     */
    public List<CreditCardGroupResponse> toGroupedCreditCardResponseList(List<CreditCard> creditCards) {
        if (creditCards == null) {
            throw new IllegalArgumentException("Credit cards list cannot be null");
        }
        List<CreditCardResponse> allResponses = toCreditCardResponseList(creditCards);
        Map<Long, CreditCardGroupResponse> groups = new HashMap<>();
        List<CreditCardResponse> standaloneCards = new ArrayList<>();

        buildGroupsFromResponses(allResponses, groups, standaloneCards);
        if (!standaloneCards.isEmpty()) {
            addGroupsForMissingParents(standaloneCards, groups);
        }
        return new ArrayList<>(groups.values());
    }

    private void buildGroupsFromResponses(
            List<CreditCardResponse> allResponses,
            Map<Long, CreditCardGroupResponse> groups,
            List<CreditCardResponse> standaloneCards) {
        for (CreditCardResponse card : allResponses) {
            if (card.parentCardId() == null) {
                groups.put(card.id(), new CreditCardGroupResponse(card, new ArrayList<>()));
            }
        }
        for (CreditCardResponse card : allResponses) {
            if (card.parentCardId() != null) {
                CreditCardGroupResponse group = groups.get(card.parentCardId());
                if (group != null) {
                    group.subCards().add(card);
                } else {
                    standaloneCards.add(card);
                }
            }
        }
    }

    private void addGroupsForMissingParents(
            List<CreditCardResponse> standaloneCards,
            Map<Long, CreditCardGroupResponse> groups) {
        Map<Long, List<CreditCardResponse>> byParentId = standaloneCards.stream()
            .collect(Collectors.groupingBy(CreditCardResponse::parentCardId));
        Set<Long> parentIds = byParentId.keySet();
        Map<Long, CreditCard> parentsById = creditCardRepository.findAllById(parentIds).stream()
            .collect(Collectors.toMap(CreditCard::getId, c -> c));

        for (Map.Entry<Long, List<CreditCardResponse>> e : byParentId.entrySet()) {
            Long parentId = e.getKey();
            List<CreditCardResponse> children = e.getValue();
            CreditCardResponse parentResponse = Optional.ofNullable(parentsById.get(parentId))
                .map(this::toCreditCardResponse)
                .orElseGet(() -> buildSyntheticParentResponse(parentId, children.get(0)));
            groups.put(parentId, new CreditCardGroupResponse(parentResponse, children));
        }
    }

    private CreditCardResponse buildSyntheticParentResponse(Long parentId, CreditCardResponse firstChild) {
        String name = firstChild.parentCardName() != null ? firstChild.parentCardName() : "Cartão " + parentId;
        return new CreditCardResponse(
            parentId,
            name,
            MASKED_LAST_FOUR_DIGITS,
            BigDecimal.ZERO,
            true,
            firstChild.bankName(),
            CardType.PHYSICAL,
            null, null, null,
            null, null,
            null, null,
            null, null
        );
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
     * Finds a parent card if specified. User must be owner or owner of that card's parent.
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
        try {
            return getCreditCardWithParentPermission(parentCardId, user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Parent card not found", e);
        }
    }

    /**
     * Finds the assigned user if specified.
     *
     * @param assignedUserId the assigned user ID. Can be null.
     * @return the user, or null if not specified.
     * @throws IllegalArgumentException if user not found.
     */
    private User findAssignedUserIfSpecified(Long assignedUserId) {
        if (assignedUserId == null) {
            return null;
        }
        Optional<User> userOpt = userRepository.findById(assignedUserId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Assigned user not found");
        }
        return userOpt.get();
    }

    /**
     * Finds the assigned contact if specified, scoped to the owner.
     *
     * @param assignedContactId the contact ID. Can be null.
     * @param owner the card owner. Cannot be null.
     * @return the contact, or null if not specified.
     * @throws IllegalArgumentException if contact not found or doesn't belong to owner.
     */
    private TrustedContact findAssignedContactIfSpecified(Long assignedContactId, User owner) {
        if (assignedContactId == null) {
            return null;
        }
        return trustedContactRepository.findByIdAndOwner(assignedContactId, owner)
            .orElseThrow(() -> new IllegalArgumentException("Assigned contact not found"));
    }

    /**
     * Updates all mutable fields of a credit card.
     * Assignment is mutually exclusive: setting a contact clears the user, and vice versa.
     * When both are null, any previous assignment is cleared.
     */
    private void updateCreditCardFields(
            CreditCard creditCard,
            CreateCreditCardRequest request,
            Bank bank,
            CreditCard parentCard,
            User assignedUser,
            TrustedContact assignedContact) {
        creditCard.updateName(request.name());
        creditCard.updateLimit(request.limit());
        creditCard.updateBank(bank);
        creditCard.updateCardType(request.cardType());
        creditCard.updateParentCard(parentCard);
        creditCard.updateCardholderName(request.cardholderName());
        updateCardAssignment(creditCard, assignedUser, assignedContact);
    }

    /**
     * Sets the card assignment to either a user or a contact (mutually exclusive).
     * Clears any previous assignment when switching types or when both are null.
     */
    private void updateCardAssignment(CreditCard creditCard, User assignedUser, TrustedContact assignedContact) {
        if (assignedContact != null) {
            creditCard.updateAssignedContact(assignedContact);
        } else {
            creditCard.updateAssignedUser(assignedUser);
            creditCard.updateAssignedContact(null);
        }
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