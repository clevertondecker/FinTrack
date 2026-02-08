package com.fintrack.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Entity representing a connection between two users (Circle of Trust).
 */
@Entity
@Table(name = "user_connections", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "connected_user_id"})
    })
public class UserConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connected_user_id", nullable = false)
    private User connectedUser;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected UserConnection() {
    }

    public UserConnection(User user, User connectedUser) {
        Validate.notNull(user, "User cannot be null");
        Validate.notNull(connectedUser, "Connected user cannot be null");
        Validate.isTrue(!user.equals(connectedUser), "A user cannot connect to themselves");

        this.user = user;
        this.connectedUser = connectedUser;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public User getConnectedUser() {
        return connectedUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserConnection that)) {
            return false;
        }
        return Objects.equals(user, that.user) && Objects.equals(connectedUser, that.connectedUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, connectedUser);
    }
}
