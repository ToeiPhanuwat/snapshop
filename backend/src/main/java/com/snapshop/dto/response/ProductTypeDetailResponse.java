package com.snapshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** @param offers cheapest first */
public record ProductTypeDetailResponse(String code, String name, int offerCount, BigDecimal minPrice,
		BigDecimal maxPrice, List<ProductResponse> offers) {

	public static ProductTypeDetailResponse of(ProductTypeSummaryResponse s, List<ProductResponse> offers) {
		return new ProductTypeDetailResponse(s.code(), s.name(), s.offerCount(), s.minPrice(), s.maxPrice(), offers);
	}

}
