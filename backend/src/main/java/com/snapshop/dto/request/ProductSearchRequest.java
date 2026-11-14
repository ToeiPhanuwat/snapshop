package com.snapshop.dto.request;

import com.snapshop.model.enums.ShopType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Query parameters of GET /api/products. */
public record ProductSearchRequest(
		String typeCode,
		ShopType shopType,
		@Size(max = 100) String q,
		ProductSort sort,
		@Min(0) Integer page,
		@Min(1) @Max(100) Integer size) {

	public ProductSearchRequest {
		sort = sort == null ? ProductSort.PRICE_ASC : sort;
		page = page == null ? 0 : page;
		size = size == null ? 24 : size;
	}

}
