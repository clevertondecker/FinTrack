package com.fintrack.dto.dashboard;

import com.fintrack.domain.creditcard.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditCardOverviewResponse(
    Long cardId,
    String cardName,
    String lastFourDigits,
    String bankName,
    String bankCode,
    LocalDate dueDate,
    BigDecimal currentInvoiceAmount,
    BigDecimal nextInvoiceAmount,
    InvoiceStatus invoiceStatus
) {}
