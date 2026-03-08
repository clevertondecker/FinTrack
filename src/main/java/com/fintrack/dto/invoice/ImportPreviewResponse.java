package com.fintrack.dto.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Response returned after parsing a PDF in the preview step,
 * containing detected card sections and their auto-match results.
 */
public record ImportPreviewResponse(

    @JsonProperty("importId")
    Long importId,

    @JsonProperty("bankName")
    String bankName,

    @JsonProperty("invoiceMonth")
    @JsonFormat(pattern = "yyyy-MM")
    YearMonth invoiceMonth,

    @JsonProperty("dueDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dueDate,

    @JsonProperty("totalAmount")
    BigDecimal totalAmount,

    @JsonProperty("confidence")
    Double confidence,

    @JsonProperty("detectedCards")
    List<DetectedCardMapping> detectedCards,

    @JsonProperty("allCardsMatched")
    boolean allCardsMatched
) {}
