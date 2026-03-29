package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotNull;

public record CategoryMergeRequest(
    @NotNull(message = "Source category ID is required")
    Long sourceCategoryId,
    @NotNull(message = "Target category ID is required")
    Long targetCategoryId
) {}
