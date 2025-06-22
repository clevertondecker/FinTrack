package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for item share creation.
 */
public record ItemShareCreateResponse(
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