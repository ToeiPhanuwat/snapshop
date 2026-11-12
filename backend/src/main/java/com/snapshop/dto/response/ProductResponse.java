package com.snapshop.dto.response;

import com.snapshop.model.enums.ShopType;
import java.math.BigDecimal;

/**
 * An offer shown on the user site.
 *
 * @param displayName {@code namePrefix + nameCore}, the name exactly as the shop wrote it
 * @param link        affiliate link, or the product link when there is no affiliate link
 */
public record ProductResponse(
		long itemId,
		String displayName,
		String namePrefix,
		String nameCore,
		String shopName,
		ShopType shopType,
		BigDecimal price,
		String soldText,
		String link,
		String productTypeCode,
		String typeName) {
}
