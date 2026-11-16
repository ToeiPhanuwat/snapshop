package com.snapshop.service.feed;

import com.snapshop.model.entity.MarketplaceProduct.FeedExtras;
import com.snapshop.model.enums.ShopType;
import java.math.BigDecimal;

/**
 * One feed line for an item, already checked.
 *
 * @param salePrice price after discount; null when the feed has none
 * @param listPrice price before discount
 * @param imageUrl  null when blank
 */
public record FeedRecord(
		long itemId,
		long shopId,
		BigDecimal salePrice,
		BigDecimal listPrice,
		Integer stock,
		BigDecimal itemRating,
		BigDecimal shopRating,
		ShopType shopType,
		String imageUrl) {

	public FeedExtras extras() {
		return new FeedExtras(shopType, listPrice, stock, itemRating, shopRating, imageUrl);
	}

}
