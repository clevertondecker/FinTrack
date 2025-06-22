package com.fintrack.dto.creditcard;

import java.util.List;

/**
 * DTO for bank list response.
 *
 * @param message success message
 * @param banks list of banks
 * @param count total number of banks
 */
public record BankListResponse(
    String message,
    List<BankResponse> banks,
    int count
) {} 