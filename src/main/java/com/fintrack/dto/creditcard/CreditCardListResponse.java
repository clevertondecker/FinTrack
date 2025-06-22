package com.fintrack.dto.creditcard;

import java.util.List;

public record CreditCardListResponse(
    String message,
    List<CreditCardResponse> creditCards,
    int count
) {} 