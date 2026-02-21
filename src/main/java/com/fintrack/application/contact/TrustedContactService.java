package com.fintrack.application.contact;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.ItemShareRepository;
import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserConnection;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.infrastructure.persistence.contact.TrustedContactJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.user.UserConnectionJpaRepository;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for CRUD of trusted contacts (Circle of Trust - Model A).
 * All operations are scoped to the owner (logged-in user).
 * Automatically creates a {@link UserConnection} when the contact's email
 * matches a registered user.
 */
@Service
public class TrustedContactService {

    private static final Logger logger = LoggerFactory.getLogger(TrustedContactService.class);
    private static final String MSG_CONTACT_NOT_FOUND = "Contact not found";

    private final TrustedContactJpaRepository repository;
    private final UserRepository userRepository;
    private final UserConnectionJpaRepository userConnectionRepository;
    private final CreditCardJpaRepository creditCardRepository;
    private final ItemShareRepository itemShareRepository;

    public TrustedContactService(final TrustedContactJpaRepository theRepository,
                                 final UserRepository theUserRepository,
                                 final UserConnectionJpaRepository theUserConnectionRepository,
                                 final CreditCardJpaRepository theCreditCardRepository,
                                 final ItemShareRepository theItemShareRepository) {
        Validate.notNull(theRepository, "The repository cannot be null.");
        Validate.notNull(theUserRepository, "The userRepository cannot be null.");
        Validate.notNull(theUserConnectionRepository, "The userConnectionRepository cannot be null.");
        Validate.notNull(theCreditCardRepository, "The creditCardRepository cannot be null.");
        Validate.notNull(theItemShareRepository, "The itemShareRepository cannot be null.");

        repository = theRepository;
        userRepository = theUserRepository;
        userConnectionRepository = theUserConnectionRepository;
        creditCardRepository = theCreditCardRepository;
        itemShareRepository = theItemShareRepository;
    }

    @Transactional(readOnly = true)
    public List<TrustedContact> findByOwner(User owner, String search) {
        if (StringUtils.isNotBlank(search)) {
            return repository.findByOwnerAndSearch(owner, search.trim());
        }
        return repository.findByOwnerOrderByNameAsc(owner);
    }

    @Transactional
    public TrustedContact create(User owner, String name, String email, String tags, String note) {
        String emailNorm = email != null ? email.trim().toLowerCase() : "";
        if (repository.existsByOwnerAndEmail(owner, emailNorm)) {
            throw new IllegalArgumentException("A contact with this email already exists in your circle.");
        }
        TrustedContact contact = TrustedContact.create(owner, name, email, tags, note);
        TrustedContact saved = repository.save(contact);
        autoConnectIfRegistered(owner, emailNorm);
        return saved;
    }

    @Transactional
    public TrustedContact update(User owner, Long id, String name, String email, String tags, String note) {
        TrustedContact contact = repository.findByIdAndOwner(id, owner)
            .orElseThrow(() -> new IllegalArgumentException(MSG_CONTACT_NOT_FOUND));
        if (email != null && !email.isBlank()) {
            String emailNorm = email.trim().toLowerCase();
            if (!emailNorm.equals(contact.getEmail())
                && repository.existsByOwnerAndEmail(owner, emailNorm)) {
                throw new IllegalArgumentException("A contact with this email already exists in your circle.");
            }
        }
        contact.update(name, email, tags, note);
        TrustedContact saved = repository.save(contact);
        if (email != null && !email.isBlank()) {
            autoConnectIfRegistered(owner, email.trim().toLowerCase());
        }
        return saved;
    }

    @Transactional
    public void delete(User owner, Long id) {
        TrustedContact contact = repository.findByIdAndOwner(id, owner)
            .orElseThrow(() -> new IllegalArgumentException(MSG_CONTACT_NOT_FOUND));
        clearContactReferences(contact);
        repository.delete(contact);
    }

    /**
     * Removes all references to a trusted contact before deletion:
     * clears credit card assignments and removes item shares.
     */
    private void clearContactReferences(TrustedContact contact) {
        List<CreditCard> cards = creditCardRepository.findByAssignedContact(contact);
        for (CreditCard card : cards) {
            card.updateAssignedContact(null);
        }
        if (!cards.isEmpty()) {
            creditCardRepository.saveAll(cards);
            logger.info("Cleared card assignment for {} card(s) linked to contact {}",
                    cards.size(), contact.getId());
        }

        List<ItemShare> shares = itemShareRepository.findByTrustedContact(contact);
        if (!shares.isEmpty()) {
            itemShareRepository.deleteAll(shares);
            logger.info("Removed {} share(s) linked to contact {}",
                    shares.size(), contact.getId());
        }
    }

    @Transactional(readOnly = true)
    public TrustedContact findByIdAndOwner(Long id, User owner) {
        return repository.findByIdAndOwner(id, owner)
            .orElseThrow(() -> new IllegalArgumentException(MSG_CONTACT_NOT_FOUND));
    }

    /**
     * If the given email belongs to a registered user (other than the owner),
     * creates a bidirectional {@link UserConnection} so that the user shows up
     * in dropdowns that depend on connected users (e.g. credit card assignment).
     */
    private void autoConnectIfRegistered(User owner, String email) {
        Optional<User> targetOpt = userRepository.findByEmail(Email.of(email));
        if (targetOpt.isEmpty()) {
            return;
        }
        User target = targetOpt.get();
        if (target.getId().equals(owner.getId())) {
            return;
        }
        if (userConnectionRepository.findByUserAndConnectedUser(owner, target).isPresent()) {
            logger.debug("UserConnection already exists between {} and {}", owner.getId(), target.getId());
            return;
        }
        userConnectionRepository.save(new UserConnection(owner, target));
        userConnectionRepository.save(new UserConnection(target, owner));
        logger.info("Auto-connected user {} with registered contact {}", owner.getId(), target.getId());
    }
}
