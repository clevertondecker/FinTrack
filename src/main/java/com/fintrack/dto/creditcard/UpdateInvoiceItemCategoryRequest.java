package com.fintrack.dto.creditcard;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for updating an invoice item's category.
 * Used when users want to categorize items that were imported without a category.
 *
 * @param categoryId the new category ID to assign to the item. Can be null to remove category.
 */
public record UpdateInvoiceItemCategoryRequest(
    @JsonProperty("categoryId")
    Long categoryId
) {}