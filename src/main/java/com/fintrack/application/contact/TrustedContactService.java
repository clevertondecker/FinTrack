package com.fintrack.application.contact;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.persistence.contact.TrustedContactJpaRepository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for CRUD of trusted contacts (Circle of Trust - Model A).
 * All operations are scoped to the owner (logged-in user).
 */
@Service
public class TrustedContactService {

    private final TrustedContactJpaRepository repository;

    public TrustedContactService(TrustedContactJpaRepository repository) {
        this.repository = repository;
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
        return repository.save(contact);
    }

    @Transactional
    public TrustedContact update(User owner, Long id, String name, String email, String tags, String note) {
        TrustedContact contact = repository.findByIdAndOwner(id, owner)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        if (email != null && !email.isBlank()) {
            String emailNorm = email.trim().toLowerCase();
            if (!emailNorm.equals(contact.getEmail())
                && repository.existsByOwnerAndEmail(owner, emailNorm)) {
                throw new IllegalArgumentException("A contact with this email already exists in your circle.");
            }
        }
        contact.update(name, email, tags, note);
        return repository.save(contact);
    }

    @Transactional
    public void delete(User owner, Long id) {
        TrustedContact contact = repository.findByIdAndOwner(id, owner)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        repository.delete(contact);
    }

    @Transactional(readOnly = true)
    public TrustedContact findByIdAndOwner(Long id, User owner) {
        return repository.findByIdAndOwner(id, owner)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
    }
}
