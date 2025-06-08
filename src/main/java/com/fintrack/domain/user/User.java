package com.fintrack.domain.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Entity representing a system user.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private Email email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Protected constructor for JPA only.
     */
    protected User() {}

    /**
     * Private constructor for User. Use the static factory method to create
     * instances.
     *
     * @param theEmail the user's name. Must not be null or blank.
     *
     * @param theEmail the user's email. Must be a valid Email.
     *
     * @param thePassword the user's password. Must not be null or blank.
     */
    private User(String theName, Email theEmail, String thePassword) {
        Validate.notBlank(name, "Name must not be null or blank");
        Validate.notNull(email, "Email must not be null");
        Validate.notBlank(password, "Password must not be null or blank");
        name = theName;
        email = theEmail;
        password = thePassword;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Static factory method to create a new User instance.
     *
     * @param name the user's name. Cannot be null or blank.
     *
     * @param email the user's email as a string. Cannot be null.
     *
     * @param password the user's password. Cannot be null or blank.
     *
     * @return a validated User entity. Never null.
     */
    public static User of(String name, String email, String password) {
        return new User(name, Email.of(email), password);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Email getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}