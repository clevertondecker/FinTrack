package com.fintrack.dto.contact;

import java.time.LocalDateTime;

/**
 * Response DTO for a trusted contact.
 */
public record TrustedContactResponse(
    Long id,
    String name,
    String email,
    String tags,
    String note,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
