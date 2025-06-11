package com.fintrack.domain.user;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.lang3.Validate;

/**
 * Represents an Email as a Value Object in the User domain.
 *
 * This class encapsulates the business concept of a user's email address,
 * ensuring that only valid, well-formed emails can exist in the domain model.
 *
 * Example: Email.of("user@example.com");
 */
@Embeddable
public class Email {

    private static final EmailValidator validator =
      EmailValidator.getInstance();

    @Column(name="email", nullable = false, unique = true)
    private String email;

    protected Email() {}

    /**
     * Creates a new Email value object after validating the format.
     *
     * @param emailValue the email address as a string. Cannot be blank.
     *
     * @throws IllegalArgumentException if the email is null, blank, or invalid.
     */
    private Email(String emailValue) {
        String trimmed = emailValue == null ? null : emailValue.trim();
        Validate.notBlank(trimmed, "Email cannot be null or blank");
        Validate.isTrue(validator.isValid(trimmed), "Invalid email format");

        email = trimmed.toLowerCase();
    }

    /**
     * Static factory method to create an Email instance.
     * This method validates the email format and creates an immutable Email object.
     *
     * @param email the email string to validate and wrap
     *
     * @return a validated Email instance. Never null.
     *
     * @throws IllegalArgumentException if the email is invalid
     */
    public static Email of(final String email) {
        return new Email(email);
    }

    /**
     * Returns the email address as a string.
     *
     * @return the email value, Never null or blank.
     */
    public String getEmail() { return email; }

    @Override
    public boolean equals(final Object theO) {
        if (this == theO) {
            return true;
        }
        if (theO == null || getClass() != theO.getClass()) {
            return false;
        }
        Email email1 = (Email) theO;
        return Objects.equals(getEmail(), email1.getEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getEmail());
    }

    @Override
    public String toString() {
        return email;
    }
}