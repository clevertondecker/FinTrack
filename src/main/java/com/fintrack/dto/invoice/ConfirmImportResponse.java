package com.fintrack.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response after confirming a multi-card import.
 */
public record ConfirmImportResponse(

    @JsonProperty("message")
    String message,

    @JsonProperty("importId")
    Long importId,

    @JsonProperty("createdInvoiceIds")
    List<Long> createdInvoiceIds,

    @JsonProperty("itemsImported")
    int itemsImported
) {}
