package com.fintrack.dto.creditcard;

public record CategoryCreateResponse(
    String message,
    Long id,
    String name,
    String color
) {} 