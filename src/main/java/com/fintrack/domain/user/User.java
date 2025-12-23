package com.fintrack.domain.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

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

    /** The user's unique identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user's name. */
    @Column(nullable = false)
    private String name;

    /** The user's email address. */
    @Embedded
    private Email email;

    /** The user's password (encoded). */
    @Column(nullable = false)
    private String password;

    /** The user's creation timestamp. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** The user's last update timestamp. */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** The user's roles. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    /** The authentication provider. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    /**
     * Protected constructor for JPA only.
     */
    protected User() {}

    /**
     * Private constructor for User. Use the static factory methods to create instances.
     *
     * @param name the user's name. Must not be null or blank.
     * @param email the user's email. Must be a valid Email.
     * @param password the user's password. Must not be null or blank.
     * @param roles the user's roles. Must not be null or empty.
     * @param provider the authentication provider. Must not be null.
     */
    private User(final String name, final Email email,
                 final String password, final Set<Role> roles, final AuthProvider provider) {
        Validate.notBlank(name, "Name must not be null or blank.");
        Validate.notNull(email, "Email must not be null.");
        Validate.notNull(roles, "Roles must not be null.");
        Validate.notNull(provider, "Provider must not be null.");
        
        // Only validate password for local users
        if (provider == AuthProvider.LOCAL) {
            Validate.notBlank(password, "Password must not be null or blank.");
        }

        this.name = name;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.provider = provider;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Creates a new local user with email/password authentication.
     *
     * @param name the user's name. Cannot be null or blank.
     * @param email the user's email as a string. Cannot be null.
     * @param password the user's password. Cannot be null or blank.
     * @param roles the user's roles. Must not be null or empty.
     * @return a validated User entity with LOCAL provider. Never null.
     */
    public static User createLocalUser(final String name, final String email,
                                      final String password, final Set<Role> roles) {
        return new User(name, Email.of(email), password, roles, AuthProvider.LOCAL);
    }

    /**
     * Creates a new OAuth2 user without password.
     *
     * @param name the user's name. Cannot be null or blank.
     * @param email the user's email as a string. Cannot be null.
     * @param roles the user's roles. Must not be null or empty.
     * @param provider the OAuth2 provider. Must be an OAuth2 provider.
     * @return a validated User entity with OAuth2 provider. Never null.
     * @throws IllegalArgumentException if provider is not an OAuth2 provider.
     */
    public static User createOAuth2User(final String name, final String email,
                                       final Set<Role> roles, final AuthProvider provider) {
        Validate.isTrue(provider.isOAuth2(), "Provider must be an OAuth2 provider");
        return new User(name, Email.of(email), "", roles, provider);
    }

    /**
     * Creates a new local user with email/password authentication.
     * @deprecated Use {@link #createLocalUser(String, String, String, Set)} instead.
     * This method is kept for backward compatibility.
     *
     * @param name the user's name. Cannot be null or blank.
     * @param email the user's email as a string. Cannot be null.
     * @param password the user's password. Cannot be null or blank.
     * @param roles the user's roles. Must not be null or empty.
     * @return a validated User entity with LOCAL provider. Never null.
     */
    @Deprecated
    public static User of(final String name, final String email,
                          final String password, final Set<Role> roles) {
        return createLocalUser(name, email, password, roles);
    }

    /**
     * Gets the user's unique identifier.
     *
     * @return the user's ID. May be null if not persisted.
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the user's name.
     *
     * @return the user's name. Never null or blank.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the user's email.
     *
     * @return the user's email. Never null.
     */
    public Email getEmail() {
        return email;
    }

    /**
     * Gets the user's creation timestamp.
     *
     * @return the creation timestamp. Never null.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the user's last update timestamp.
     *
     * @return the last update timestamp. Never null.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Gets the user's password.
     *
     * @return the user's password. Never null or blank.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the user's roles.
     *
     * @return the user's roles. Never null, may be empty.
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * Gets the user's authentication provider.
     *
     * @return the authentication provider. Never null.
     */
    public AuthProvider getProvider() { 
        return provider; 
    }

    /**
     * Checks if this user was created through OAuth2 authentication.
     *
     * @return true if the user is an OAuth2 user, false otherwise.
     */
    public boolean isOAuth2User() {
        return provider.isOAuth2();
    }

    /**
     * Checks if this user was created through local authentication.
     *
     * @return true if the user is a local user, false otherwise.
     */
    public boolean isLocalUser() {
        return provider == AuthProvider.LOCAL;
    }

    /**
     * Sets the user's authentication provider.
     * This method should be used with caution as it can change the authentication flow.
     *
     * @param provider the authentication provider. Cannot be null.
     * @throws IllegalArgumentException if provider is null.
     */
    public void setProvider(AuthProvider provider) {
        Validate.notNull(provider, "Provider must not be null.");
        this.provider = provider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}