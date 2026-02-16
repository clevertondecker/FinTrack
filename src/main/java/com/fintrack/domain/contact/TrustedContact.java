package com.fintrack.domain.contact;

import com.fintrack.domain.user.User;

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
 * Entity representing a trusted contact (Circle of Trust - Model A).
 * Offline contact owned by a user; used for suggestion/selection when splitting invoice items.
 * Contact does not need to exist as a system user.
 */
@Entity
@Table(name = "trusted_contacts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"owner_user_id", "email"})
    })
public class TrustedContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    /** Optional comma-separated tags. */
    @Column(length = 500)
    private String tags;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected TrustedContact() {
    }

    private TrustedContact(User owner, String name, String email, String tags, String note) {
        Validate.notNull(owner, "Owner must not be null");
        Validate.notBlank(name, "Name must not be blank");
        Validate.notBlank(email, "Email must not be blank");

        this.owner = owner;
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.tags = tags != null && !tags.isBlank() ? tags.trim() : null;
        this.note = note != null && !note.isBlank() ? note.trim() : null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static TrustedContact create(User owner, String name, String email, String tags, String note) {
        return new TrustedContact(owner, name, email, tags, note);
    }

    public void update(String name, String email, String tags, String note) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (email != null && !email.isBlank()) {
            this.email = email.trim().toLowerCase();
        }
        this.tags = tags != null && !tags.isBlank() ? tags.trim() : null;
        this.note = note != null && !note.isBlank() ? note.trim() : null;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTags() {
        return tags;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TrustedContact that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
