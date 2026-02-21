package com.fintrack.application.user;

import java.util.List;
import java.util.Set;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserConnection;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.infrastructure.persistence.contact.TrustedContactJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.user.UserConnectionJpaRepository;
import com.fintrack.infrastructure.security.PasswordService;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for user management operations:
 * registration, Circle of Trust (connected users), and connection logic.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String MSG_USER_NOT_FOUND = "User not found with email: ";
    private static final String MSG_CANNOT_CONNECT_SELF = "You cannot connect to yourself";
    private static final String MSG_ALREADY_CONNECTED = "Users are already connected";

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final UserConnectionJpaRepository userConnectionRepository;
    private final TrustedContactJpaRepository trustedContactRepository;
    private final CreditCardJpaRepository creditCardRepository;

    public UserService(final UserRepository theUserRepository,
                       final PasswordService thePasswordService,
                       final UserConnectionJpaRepository theUserConnectionRepository,
                       final TrustedContactJpaRepository theTrustedContactRepository,
                       final CreditCardJpaRepository theCreditCardRepository) {
        Validate.notNull(theUserRepository, "The userRepository cannot be null.");
        Validate.notNull(thePasswordService, "The passwordService cannot be null.");
        Validate.notNull(theUserConnectionRepository, "The userConnectionRepository cannot be null.");
        Validate.notNull(theTrustedContactRepository, "The trustedContactRepository cannot be null.");
        Validate.notNull(theCreditCardRepository, "The creditCardRepository cannot be null.");

        userRepository = theUserRepository;
        passwordService = thePasswordService;
        userConnectionRepository = theUserConnectionRepository;
        trustedContactRepository = theTrustedContactRepository;
        creditCardRepository = theCreditCardRepository;
    }

    /**
     * Returns all users connected to the given user (Circle of Trust).
     *
     * @param user the user whose connections to load
     * @return list of connected users, never null
     */
    public List<User> getConnectedUsers(User user) {
        return userConnectionRepository.findConnectedUsers(user);
    }

    /**
     * Connects two users bidirectionally (Circle of Trust).
     *
     * @param user the user initiating the connection
     * @param emailToConnect email of the user to connect with
     * @throws IllegalArgumentException if user not found, connecting to self, or already connected
     */
    @Transactional
    public void connectUsers(User user, String emailToConnect) {
        User connectedUser = userRepository.findByEmail(Email.of(emailToConnect))
            .orElseThrow(() -> new IllegalArgumentException(MSG_USER_NOT_FOUND + emailToConnect));

        if (user.equals(connectedUser)) {
            throw new IllegalArgumentException(MSG_CANNOT_CONNECT_SELF);
        }

        if (userConnectionRepository.findByUserAndConnectedUser(user, connectedUser).isPresent()) {
            throw new IllegalArgumentException(MSG_ALREADY_CONNECTED);
        }

        userConnectionRepository.save(new UserConnection(user, connectedUser));
        userConnectionRepository.save(new UserConnection(connectedUser, user));
    }

    /**
     * Registers a new user in the system.
     *
     * @param name the user's name. Cannot be blank.
     *
     * @param email the user's email. Cannot be blank.
     *
     * @param rawPassword the raw password to be encoded. Cannot be blank.
     *
     * @param roles the user's roles. Cannot be null or empty.
     *
     * @return the created user entity. Never null.
     */
    @Transactional
    public User registerUser(final String name, final String email,
                             final String rawPassword, final Set<Role> roles) {
        String encodedPassword = passwordService.encodePassword(rawPassword);
        User user = User.createLocalUser(name, email, encodedPassword, roles);
        User savedUser = userRepository.save(user);

        migrateContactAssignments(savedUser);

        return savedUser;
    }

    /**
     * When a new user registers, finds any {@link TrustedContact}s with the same email
     * and migrates credit card assignments from contact to user.
     * Also creates bidirectional {@link UserConnection}s with the contact owners.
     */
    private void migrateContactAssignments(User newUser) {
        String email = newUser.getEmail().getEmail();
        List<TrustedContact> matchingContacts = trustedContactRepository.findByEmail(email);

        if (matchingContacts.isEmpty()) {
            return;
        }

        for (TrustedContact contact : matchingContacts) {
            migrateCardsFromContact(contact, newUser);
            createBidirectionalConnection(contact.getOwner(), newUser);
        }
    }

    private void migrateCardsFromContact(TrustedContact contact, User newUser) {
        List<CreditCard> cards = creditCardRepository.findByAssignedContact(contact);
        for (CreditCard card : cards) {
            card.updateAssignedUser(newUser);
        }
        if (!cards.isEmpty()) {
            creditCardRepository.saveAll(cards);
            logger.info("Migrated {} card(s) from contact {} to user {}",
                cards.size(), contact.getId(), newUser.getId());
        }
    }

    private void createBidirectionalConnection(User owner, User newUser) {
        if (owner.getId().equals(newUser.getId())) {
            return;
        }
        if (userConnectionRepository.findByUserAndConnectedUser(owner, newUser).isPresent()) {
            return;
        }
        userConnectionRepository.save(new UserConnection(owner, newUser));
        userConnectionRepository.save(new UserConnection(newUser, owner));
        logger.info("Auto-connected owner {} with newly registered user {}",
            owner.getId(), newUser.getId());
    }
}