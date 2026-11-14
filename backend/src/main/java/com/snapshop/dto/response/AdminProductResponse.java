package com.snapshop.dto.response;

import com.snapshop.model.enums.ProductStatus;
import com.snapshop.model.enums.ShopType;
import java.math.BigDecimal;
import java.time.Instant;

/** The current version of an item as the admin sees it, including whether users can see it. */
public record AdminProductResponse(
		long productId,
		long itemId,
		String displayName,
		String shopName,
		ShopType shopType,
		BigDecimal price,
		ProductStatus status,
		String productTypeCode,
		String typeName,
		boolean onUserSite,
		Instant lastSeenAt,
		String link) {
}
