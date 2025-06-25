package com.fintrack.dto.creditcard;

import java.util.List;

/**
 * Response DTO for user's shares list.
 */
public record MySharesResponse(
    String message,
    List<MyShareResponse> shares,
    Integer shareCount
) {} 