package com.snapshop.dto.response;

import java.math.BigDecimal;

/**
 * A product type on the user site, with how many shops sell it and the price range.
 *
 * @param imageUrl image of the cheapest offer that has one; null when no offer has an image
 */
public record ProductTypeSummaryResponse(String code, String name, int offerCount, BigDecimal minPrice,
		BigDecimal maxPrice, String imageUrl) {
}
