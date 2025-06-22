package com.fintrack.dto.creditcard;

/**
 * DTO for bank detail response.
 *
 * @param message success message
 * @param bank the bank information
 */
public record BankDetailResponse(
    String message,
    BankResponse bank
) {} 