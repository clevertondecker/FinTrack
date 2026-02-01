package com.fintrack.dto.creditcard;

import java.util.List;

/**
 * DTO representing a group of credit cards (parent and its children).
 */
public record CreditCardGroupResponse(
    CreditCardResponse parentCard,
    List<CreditCardResponse> subCards
) {}
