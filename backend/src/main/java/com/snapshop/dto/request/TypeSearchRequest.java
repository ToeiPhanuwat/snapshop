package com.snapshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Query parameters of GET /api/product-types and GET /api/admin/product-types. */
public record TypeSearchRequest(
		@Size(max = 100) String q,
		@Min(0) Integer page,
		@Min(1) @Max(100) Integer size) {

	public TypeSearchRequest {
		page = page == null ? 0 : page;
		size = size == null ? 24 : size;
	}

}
