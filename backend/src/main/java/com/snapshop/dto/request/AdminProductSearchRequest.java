package com.snapshop.dto.request;

import com.snapshop.model.enums.ProductStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Query parameters of GET /api/admin/products. */
public record AdminProductSearchRequest(
		ProductStatus status,
		String typeCode,
		@Size(max = 100) String q,
		@Min(0) Integer page,
		@Min(1) @Max(200) Integer size) {

	public AdminProductSearchRequest {
		page = page == null ? 0 : page;
		size = size == null ? 50 : size;
	}

}
