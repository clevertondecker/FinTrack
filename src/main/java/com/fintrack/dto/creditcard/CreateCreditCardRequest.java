package com.fintrack.dto.creditcard;

import com.fintrack.domain.creditcard.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO for creating a new credit card.
 * Contains validation annotations for request validation.
 */
public record CreateCreditCardRequest(
    
    @NotBlank(message = "Credit card name is required.")
    @Size(min = 2, max = 100, message = "Credit card name must be between 2 and 100 characters.")
    String name,
    
    @NotBlank(message = "Last four digits are required.")
    @Pattern(regexp = "\\d{4}", message = "Last four digits must be exactly 4 digits.")
    String lastFourDigits,
    
    @NotNull(message = "Credit limit is required.")
    @Positive(message = "Credit limit must be positive.")
    BigDecimal limit,
    
    @NotNull(message = "Bank ID is required.")
    Long bankId,
    
    @NotNull(message = "Card type is required.")
    CardType cardType,
    
    Long parentCardId,
    
    @Size(max = 100, message = "Cardholder name must be at most 100 characters.")
    String cardholderName,
    
    /** ID of the user this card is assigned to (who uses it). Null means the card is for the owner. */
    Long assignedUserId
) {} 