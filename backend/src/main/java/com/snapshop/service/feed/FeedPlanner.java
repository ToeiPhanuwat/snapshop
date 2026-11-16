package com.snapshop.service.feed;

import com.snapshop.service.feed.FeedPlan.NewPrice;
import com.snapshop.service.feed.FeedPlan.Update;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Decides what feed lines change. Pure: same lines and state always give the same plan.
 *
 * <p>The feed always refreshes the extras (shop type, image, stock, ratings, list price). The feed sale price is
 * used only for items whose import file had no usable price: it fills a missing price, and it makes a new price
 * version when it differs from the stored one. A price that came from the file is never replaced by the feed.
 */
@Component
public class FeedPlanner {

	/**
	 * @param feedPriced items whose price in the latest import file was abbreviated, so the feed price is the
	 *                   one to trust
	 */
	public FeedPlan plan(Collection<FeedRecord> records, Map<Long, FeedTarget> activeByItem, Set<Long> feedPriced) {
		List<Update> updates = new ArrayList<>();
		List<NewPrice> newPrices = new ArrayList<>();
		int skipped = 0;
		for (FeedRecord record : records) {
			FeedTarget target = activeByItem.get(record.itemId());
			if (target == null || target.shopId() != record.shopId()) {
				skipped++;
			} else if (changesPrice(target, record, feedPriced)) {
				newPrices.add(new NewPrice(target.productId(), record));
			} else {
				updates.add(new Update(target.productId(), record, target.price() == null && record.salePrice() != null));
			}
		}
		return new FeedPlan(updates, newPrices, skipped);
	}

	private static boolean changesPrice(FeedTarget target, FeedRecord record, Set<Long> feedPriced) {
		return target.price() != null && feedPriced.contains(target.itemId()) && record.salePrice() != null
				&& target.price().compareTo(record.salePrice()) != 0;
	}

}
