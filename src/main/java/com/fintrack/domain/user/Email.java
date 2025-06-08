package com.fintrack.domain.user;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import java.util.Objects;
import java.util.regex.Pattern;

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

    private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Column(nullable = false, unique = true)
    private String value;

    protected Email() {}

    /**
     * Creates a new Email value object after validating the format.
     *
     * @param value the email address as a string. Cannot be blank.
     *
     * @throws IllegalArgumentException if the email is null, blank, or invalid.
     */
    private Email(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        this.value = value;
    }

    /**
     * Factory method to create a new Email value object.
     *
     * @param value the email address as a string. Cannot be blank.
     *
     * @return a validated Email object, Never null if the email is valid.
     *
     * @throws IllegalArgumentException if the email is invalid.
     */
    public static Email of(String value) {
        return new Email(value);
    }

    /**
     * Returns the email address as a string.
     *
     * @return the email value, Never null or blank.
     */
    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return value.equalsIgnoreCase(email.value);
    }

    @Override
    public int hashCode() {
        return value.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}