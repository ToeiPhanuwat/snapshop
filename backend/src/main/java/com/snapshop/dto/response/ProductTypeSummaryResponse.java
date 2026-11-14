package com.snapshop.dto.response;

import java.math.BigDecimal;

/** A product type on the user site, with how many shops sell it and the price range. */
public record ProductTypeSummaryResponse(String code, String name, int offerCount, BigDecimal minPrice,
		BigDecimal maxPrice) {
}
