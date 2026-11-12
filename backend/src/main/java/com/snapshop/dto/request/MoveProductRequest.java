package com.snapshop.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/admin/products/{itemId}/move. */
public record MoveProductRequest(@NotBlank String productTypeCode) {
}
