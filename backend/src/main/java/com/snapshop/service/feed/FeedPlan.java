package com.snapshop.service.feed;

import java.util.List;

/**
 * What a feed look-up changes, decided without touching the database.
 *
 * @param updates     current versions that take the feed extras, and maybe their first price
 * @param newPrices   current versions to replace with a new price version
 * @param skippedCount feed lines not used: unknown item, or a different shop id than the stored one
 */
public record FeedPlan(List<Update> updates, List<NewPrice> newPrices, int skippedCount) {

	public FeedPlan {
		updates = List.copyOf(updates);
		newPrices = List.copyOf(newPrices);
	}

	/** @param fillPrice the version has no price yet and takes the feed sale price */
	public record Update(long productId, FeedRecord record, boolean fillPrice) {
	}

	public record NewPrice(long replacesProductId, FeedRecord record) {
	}

}
