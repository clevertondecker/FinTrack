package com.fintrack.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a trusted contact.
 */
public record CreateTrustedContactRequest(
    @NotBlank(message = "Name is required.")
    @Size(min = 1, max = 255)
    String name,

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    @Size(max = 255)
    String email,

    @Size(max = 500)
    String tags,

    @Size(max = 1000)
    String note
) {}
