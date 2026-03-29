package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CategoryReorderRequest(
    @NotEmpty(message = "Category IDs list is required")
    List<Long> orderedIds
) {}
