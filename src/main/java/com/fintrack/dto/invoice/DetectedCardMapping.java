package com.fintrack.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents one credit card detected in a PDF during import preview,
 * along with its auto-match result against registered cards.
 */
public record DetectedCardMapping(

    @JsonProperty("detectedLastFourDigits")
    String detectedLastFourDigits,

    @JsonProperty("detectedCardName")
    String detectedCardName,

    @JsonProperty("matchedCreditCardId")
    Long matchedCreditCardId,

    @JsonProperty("matchedCardName")
    String matchedCardName,

    @JsonProperty("autoMatched")
    boolean autoMatched,

    @JsonProperty("ambiguous")
    boolean ambiguous,

    @JsonProperty("candidateCardIds")
    List<Long> candidateCardIds,

    @JsonProperty("items")
    List<ParsedInvoiceData.ParsedInvoiceItem> items,

    @JsonProperty("subtotal")
    BigDecimal subtotal
) {}
