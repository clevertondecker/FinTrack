package com.fintrack.dto.creditcard;

/**
 * DTO for bank response.
 *
 * @param id the bank's unique identifier
 * @param code the bank's code
 * @param name the bank's name
 */
public record BankResponse(
    Long id,
    String code,
    String name
) {} 