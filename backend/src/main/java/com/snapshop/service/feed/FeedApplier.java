package com.snapshop.service.feed;

import com.snapshop.model.entity.MarketplaceProduct;
import com.snapshop.model.enums.ActiveFlag;
import com.snapshop.model.enums.DeactivationReason;
import com.snapshop.repository.ImportLockRepository;
import com.snapshop.repository.MarketplaceProductRepository;
import com.snapshop.service.feed.FeedPlan.NewPrice;
import com.snapshop.service.feed.FeedPlan.Update;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes what the feed brought back, in one short transaction. It takes the same lock as an import, so a feed
 * write never meets an import writing the same rows; the slow download happens before, outside the lock.
 */
@Service
class FeedApplier {

	private final FeedPlanner planner;
	private final ImportLockRepository lock;
	private final MarketplaceProductRepository products;

	FeedApplier(FeedPlanner planner, ImportLockRepository lock, MarketplaceProductRepository products) {
		this.planner = planner;
		this.lock = lock;
		this.products = products;
	}

	@Transactional
	void apply(FeedRequest request, Collection<FeedRecord> records) {
		lock.lockForThisTransaction();
		Map<Long, MarketplaceProduct> activeByItem = products
				.findByItemIdInAndActiveFlag(request.itemIds(), ActiveFlag.Y).stream()
				.collect(Collectors.toMap(MarketplaceProduct::getItemId, Function.identity()));
		FeedPlan plan = planner.plan(records, targets(activeByItem), request.feedPricedItemIds());
		Instant now = Instant.now();
		Map<Long, MarketplaceProduct> byId = new HashMap<>();
		activeByItem.values().forEach(p -> byId.put(p.getId(), p));
		plan.newPrices().forEach(n -> byId.get(n.replacesProductId()).deactivate(DeactivationReason.PRICE_CHANGED, now));
		products.flush();
		plan.updates().forEach(u -> update(byId.get(u.productId()), u, now));
		plan.newPrices().forEach(n -> insertVersion(byId.get(n.replacesProductId()), n, request.batchId(), now));
	}

	private static void update(MarketplaceProduct product, Update u, Instant now) {
		if (u.fillPrice()) {
			product.fillPriceFromFeed(u.record().salePrice());
		}
		product.applyFeed(u.record().extras(), now);
	}

	private void insertVersion(MarketplaceProduct old, NewPrice n, Long batchId, Instant now) {
		MarketplaceProduct next = old.nextPriceVersion(n.record().salePrice(), batchId, now);
		next.applyFeed(n.record().extras(), now);
		products.save(next);
	}

	private static Map<Long, FeedTarget> targets(Map<Long, MarketplaceProduct> activeByItem) {
		return activeByItem.values().stream().collect(Collectors.toMap(MarketplaceProduct::getItemId,
				p -> new FeedTarget(p.getId(), p.getItemId(), p.getShopId(), p.getPrice(), p.getStatus())));
	}

}
