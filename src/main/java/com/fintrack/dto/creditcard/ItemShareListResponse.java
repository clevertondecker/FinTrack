package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for item share list data.
 */
public record ItemShareListResponse(
    String message,
    Long invoiceId,
    Long itemId,
    String itemDescription,
    BigDecimal itemAmount,
    List<ItemShareResponse> shares,
    Integer shareCount,
    BigDecimal totalSharedAmount,
    BigDecimal unsharedAmount
) {} 