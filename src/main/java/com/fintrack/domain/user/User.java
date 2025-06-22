package com.fintrack.domain.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * Entity representing a system user.
 * Contains user information and business rules.
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

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    /**
     * Protected constructor for JPA only.
     */
    protected User() {}

    /**
     * Private constructor for User. Use the static factory method to create instances.
     *
     * @param theName the user's name. Must not be null or blank.
     * @param theEmail the user's email. Must be a valid Email.
     * @param thePassword the user's password. Must not be null or blank.
     * @param theRoles the user's roles. Must not be null or empty.
     */
    private User(final String theName, final Email theEmail,
                 final String thePassword, final Set<Role> theRoles) {
        Validate.notBlank(theName, "Name must not be null or blank.");
        Validate.notNull(theEmail, "Email must not be null.");
        Validate.notBlank(thePassword, "Password must not be null or blank.");
        Validate.notNull(theRoles, "Roles must not be null.");

        name = theName;
        email = theEmail;
        password = thePassword;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        roles = theRoles;
    }

    /**
     * Static factory method to create a new User instance.
     *
     * @param name the user's name. Cannot be null or blank.
     * @param email the user's email as a string. Cannot be null.
     * @param password the user's password. Cannot be null or blank.
     * @param roles the user's roles. Must not be null or empty.
     * @return a validated User entity. Never null.
     */
    public static User of(final String name, final String email,
                          final String password, final Set<Role> roles) {
        return new User(name, Email.of(email), password, roles);
    }

    /**
     * Gets the user's unique identifier.
     *
     * @return the user's ID. May be null if not persisted.
     */
    public Long getId() { return id; }

    /**
     * Gets the user's name.
     *
     * @return the user's name. Never null or blank.
     */
    public String getName() { return name; }

    /**
     * Gets the user's email.
     *
     * @return the user's email. Never null.
     */
    public Email getEmail() { return email; }

    /**
     * Gets the user's creation timestamp.
     *
     * @return the creation timestamp. Never null.
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Gets the user's last update timestamp.
     *
     * @return the last update timestamp. Never null.
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Gets the user's password.
     *
     * @return the user's password. Never null or blank.
     */
    public String getPassword() { return password; }

    /**
     * Gets the user's roles.
     *
     * @return the user's roles. Never null, may be empty.
     */
    public Set<Role> getRoles() { return roles; }

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