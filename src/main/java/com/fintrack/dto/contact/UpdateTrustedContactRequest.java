package com.fintrack.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a trusted contact.
 */
public record UpdateTrustedContactRequest(
    @Size(min = 1, max = 255)
    String name,

    @Email(message = "Invalid email format.")
    @Size(max = 255)
    String email,

    @Size(max = 500)
    String tags,

    @Size(max = 1000)
    String note
) {}
