package com.snapshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * What importing a file would change. Nothing has been written yet.
 *
 * @param newTypes     names of the product types the import would create
 * @param priceChanges items whose price would change
 * @param toHide       current items missing from the file, which would become NOT_IN_LATEST
 * @param toReview     items that would be in REVIEW after the import
 * @param feedItemCount items the system will look up in the feed once the import is done
 */
public record ImportPreviewResponse(
		String fileName,
		int rowCount,
		int insertedCount,
		int priceChangedCount,
		int unchangedCount,
		int notInLatestCount,
		int newTypeCount,
		List<String> newTypes,
		List<PriceChange> priceChanges,
		List<ItemLine> toHide,
		List<ItemLine> toReview,
		int feedItemCount) {

	public record PriceChange(long itemId, String name, BigDecimal oldPrice, BigDecimal newPrice) {
	}

	public record ItemLine(long itemId, String name) {
	}

}
