package com.snapshop.service.feed;

import java.util.Set;

/**
 * Items to look up in the feed.
 *
 * @param batchId          import that asked for it; null for a manual retry
 * @param feedPricedItemIds items whose import file had no usable price, so the feed price is the one to trust
 */
public record FeedRequest(Long batchId, Set<Long> itemIds, Set<Long> feedPricedItemIds) {

	public FeedRequest {
		itemIds = Set.copyOf(itemIds);
		feedPricedItemIds = Set.copyOf(feedPricedItemIds);
	}

}
