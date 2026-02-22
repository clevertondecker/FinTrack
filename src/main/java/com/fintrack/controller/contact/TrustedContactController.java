package com.fintrack.controller.contact;

import com.fintrack.application.contact.TrustedContactService;
import com.fintrack.application.user.UserService;
import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.user.User;
import com.fintrack.dto.contact.CreateTrustedContactRequest;
import com.fintrack.dto.contact.TrustedContactResponse;
import com.fintrack.dto.contact.UpdateTrustedContactRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for trusted contacts (Circle of Trust - Model A).
 * All endpoints require authentication; operations are scoped to the current user (owner).
 */
@RestController
@RequestMapping("/api/trusted-contacts")
public class TrustedContactController {

    private final TrustedContactService trustedContactService;
    private final UserService userService;

    public TrustedContactController(TrustedContactService trustedContactService, UserService userService) {
        this.trustedContactService = trustedContactService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<TrustedContactResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "search", required = false) String search) {
        User owner = userService.getCurrentUser(userDetails.getUsername());
        List<TrustedContactResponse> list = trustedContactService.findByOwner(owner, search)
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<TrustedContactResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateTrustedContactRequest request) {
        User owner = userService.getCurrentUser(userDetails.getUsername());
        var contact = trustedContactService.create(
            owner,
            request.name(),
            request.email(),
            request.tags(),
            request.note()
        );
        return ResponseEntity.ok(toResponse(contact));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrustedContactResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTrustedContactRequest request) {
        User owner = userService.getCurrentUser(userDetails.getUsername());
        var contact = trustedContactService.update(
            owner,
            id,
            request.name(),
            request.email(),
            request.tags(),
            request.note()
        );
        return ResponseEntity.ok(toResponse(contact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User owner = userService.getCurrentUser(userDetails.getUsername());
        trustedContactService.delete(owner, id);
        return ResponseEntity.noContent().build();
    }

    private TrustedContactResponse toResponse(TrustedContact c) {
        return new TrustedContactResponse(
            c.getId(),
            c.getName(),
            c.getEmail(),
            c.getTags(),
            c.getNote(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
