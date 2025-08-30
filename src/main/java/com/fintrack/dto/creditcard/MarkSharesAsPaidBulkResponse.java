package com.fintrack.dto.creditcard;

import java.util.List;

/**
 * DTO response for bulk marking item shares as paid.
 */
public record MarkSharesAsPaidBulkResponse(
    String message,
    int updatedCount,
    List<ItemShareResponse> updatedShares
) {}
