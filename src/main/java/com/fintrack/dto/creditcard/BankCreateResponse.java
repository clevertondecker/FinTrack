package com.fintrack.dto.creditcard;

/**
 * DTO for bank creation response.
 *
 * @param message success message
 * @param id the created bank's unique identifier
 * @param code the created bank's code
 * @param name the created bank's name
 */
public record BankCreateResponse(
    String message,
    Long id,
    String code,
    String name
) {} 