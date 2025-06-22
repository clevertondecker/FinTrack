package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for bank creation request.
 *
 * @param code the bank's code (must be 2-10 characters, alphanumeric)
 * @param name the bank's name (must be 2-100 characters, only letters, numbers and spaces)
 */
public record BankCreateRequest(
    @NotBlank(message = "Bank code is required")
    @Size(min = 2, max = 10, message = "Bank code must be between 2 and 10 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Bank code must contain only uppercase letters and numbers")
    String code,
    
    @NotBlank(message = "Bank name is required")
    @Size(min = 2, max = 100, message = "Bank name must be between 2 and 100 characters")
    @Pattern(regexp = "^[\\p{L}0-9 ]+$", message = "Bank name must contain only letters, numbers and spaces")
    String name
) {} 