package com.snapshop.service.feed;

import com.snapshop.dto.response.FeedSyncRunResponse;
import com.snapshop.exception.BusinessRuleException;
import com.snapshop.model.enums.ActiveFlag;
import com.snapshop.model.enums.ProductStatus;
import com.snapshop.repository.FeedSyncRunRepository;
import com.snapshop.repository.MarketplaceProductRepository;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Looks items up in the feed and stores what it says. Download first (slow, no lock, no transaction), then one
 * short write. Every look-up is logged in feed_sync_run so an admin can see what failed.
 */
@Service
public class FeedSyncService {

	private static final Logger LOG = LoggerFactory.getLogger(FeedSyncService.class);

	private final FeedClient client;
	private final FeedApplier applier;
	private final FeedSyncRunRepository runs;
	private final MarketplaceProductRepository products;

	public FeedSyncService(FeedClient client, FeedApplier applier, FeedSyncRunRepository runs,
			MarketplaceProductRepository products) {
		this.client = client;
		this.applier = applier;
		this.runs = runs;
		this.products = products;
	}

	/** Same as {@link #sync} but on the single feed thread, so the caller does not wait for the download. */
	@Async("feedSyncExecutor")
	public void syncAsync(FeedRequest request) {
		sync(request);
	}

	public FeedSyncRunResponse sync(FeedRequest request) {
		long runId = runs.start(request.batchId(), request.itemIds().size(), Instant.now());
		try {
			FeedResult result = client.fetch(request.itemIds());
			applier.apply(request, result.found().values());
			runs.finish(runId, result.found().size(), result.outcome().name(), result.message(), Instant.now());
		} catch (RuntimeException e) {
			LOG.error("Feed look-up {} failed", runId, e);
			runs.finish(runId, 0, FeedResult.Outcome.FAILED.name(), e.getClass().getSimpleName(), Instant.now());
		}
		return runs.find(runId);
	}

	/** Items still waiting for a price, or never looked up. Refused when there are none or one is running. */
	public FeedRequest pendingRequest() {
		if (runs.hasRunning()) {
			throw new BusinessRuleException("กำลังดึงฟีดอยู่ รอให้เสร็จก่อน");
		}
		Set<Long> ids = Set.copyOf(products.findItemIdsToLookUp(ActiveFlag.Y, ProductStatus.PRICE_PENDING));
		if (ids.isEmpty()) {
			throw new BusinessRuleException("ไม่มีรายการที่ต้องดึงจากฟีด");
		}
		return new FeedRequest(null, ids, Set.of());
	}

}
