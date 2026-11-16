package com.snapshop.service;

import com.snapshop.dto.response.ImportPreviewResponse;
import com.snapshop.dto.response.ImportResultResponse;
import com.snapshop.model.entity.ImportBatch;
import com.snapshop.model.entity.MarketplaceProduct;
import com.snapshop.model.entity.ProductType;
import com.snapshop.model.enums.ActiveFlag;
import com.snapshop.model.enums.DeactivationReason;
import com.snapshop.repository.ImportBatchRepository;
import com.snapshop.repository.ImportLockRepository;
import com.snapshop.repository.ItemType;
import com.snapshop.repository.MarketplaceProductRepository;
import com.snapshop.repository.ProductTypeRepository;
import com.snapshop.service.importing.ActiveVersion;
import com.snapshop.service.importing.CsvImportParser;
import com.snapshop.service.importing.CurrentState;
import com.snapshop.service.importing.ImportPlan;
import com.snapshop.service.importing.ImportPlan.NewVersion;
import com.snapshop.service.importing.ImportPlan.SeenAgain;
import com.snapshop.service.importing.ImportPlanner;
import com.snapshop.service.importing.ImportRow;
import com.snapshop.service.importing.TypeRef;
import com.snapshop.service.feed.FeedRequest;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports one file in one transaction: parse, lock, load state, plan, apply the plan. */
@Service
public class ImportService {

	private final CsvImportParser parser;
	private final ImportPlanner planner;
	private final ImportLockRepository lock;
	private final ProductTypeRepository types;
	private final MarketplaceProductRepository products;
	private final ImportBatchRepository batches;
	private final ApplicationEventPublisher events;

	public ImportService(CsvImportParser parser, ImportPlanner planner, ImportLockRepository lock,
			ProductTypeRepository types, MarketplaceProductRepository products, ImportBatchRepository batches,
			ApplicationEventPublisher events) {
		this.parser = parser;
		this.planner = planner;
		this.lock = lock;
		this.types = types;
		this.products = products;
		this.batches = batches;
		this.events = events;
	}

	@Transactional
	public ImportResultResponse importFile(String category, String fileName, InputStream file) {
		List<ImportRow> rows = parser.parse(file);
		lock.lockForThisTransaction();
		Prepared prepared = prepare(category, rows);
		Instant now = Instant.now();
		ImportBatch batch = batches.save(new ImportBatch(category, fileName, prepared.counts(), now));
		new PlanApplier(category, batch.getId(), now, prepared.activeById()).apply(prepared.plan());
		events.publishEvent(feedRequest(batch.getId(), rows, prepared.plan()));
		return ImportResultResponse.from(batch);
	}

	/** The look-up in the feed that follows a committed import; items without a usable file price rely on it. */
	private static FeedRequest feedRequest(long batchId, List<ImportRow> rows, ImportPlan plan) {
		Set<Long> noFilePrice = rows.stream().filter(r -> r.price() == null).map(ImportRow::itemId)
				.collect(Collectors.toSet());
		return new FeedRequest(batchId, Set.copyOf(plan.feedItemIds()), noFilePrice);
	}

	/** What {@link #importFile} would change, computed by the same code, without writing anything. */
	@Transactional(readOnly = true)
	public ImportPreviewResponse preview(String category, String fileName, InputStream file) {
		Prepared prepared = prepare(category, parser.parse(file));
		return ImportPreviews.of(fileName, prepared.counts(), prepared.plan(), prepared.activeById());
	}

	private Prepared prepare(String category, List<ImportRow> rows) {
		List<MarketplaceProduct> active = products.findInCategory(category, ActiveFlag.Y);
		ImportPlan plan = planner.plan(rows, loadState(category, active));
		return new Prepared(counts(rows, plan), plan, byId(active));
	}

	private record Prepared(ImportBatch.Counts counts, ImportPlan plan, Map<Long, MarketplaceProduct> activeById) {
	}

	private CurrentState loadState(String category, List<MarketplaceProduct> active) {
		Map<Long, ActiveVersion> activeByItem = active.stream().collect(Collectors.toMap(
				MarketplaceProduct::getItemId,
				p -> new ActiveVersion(p.getId(), p.getItemId(), p.getProductTypeId(), p.getPrice(), p.getStatus())));
		Map<Long, Long> typeByItem = new HashMap<>();
		for (ItemType it : products.findItemTypesInCategory(category)) {
			typeByItem.put(it.itemId(), it.productTypeId());
		}
		Map<String, Long> typeByName = types.findByCategoryCode(category).stream()
				.collect(Collectors.toMap(ProductType::getTypeNameNorm, ProductType::getId));
		return new CurrentState(activeByItem, typeByItem, typeByName);
	}

	private static ImportBatch.Counts counts(List<ImportRow> rows, ImportPlan plan) {
		return new ImportBatch.Counts(rows.size(), (int) plan.newItemCount(), (int) plan.priceChangeCount(),
				plan.seenAgain().size(), plan.notInLatest().size(), plan.newTypes().size());
	}

	private static Map<Long, MarketplaceProduct> byId(List<MarketplaceProduct> active) {
		return active.stream().collect(Collectors.toMap(MarketplaceProduct::getId, Function.identity()));
	}

	/** Writes a plan. Old versions are switched off and flushed before new ones go in (one 'Y' row per item). */
	private final class PlanApplier {

		private final String category;
		private final long batchId;
		private final Instant now;
		private final Map<Long, MarketplaceProduct> activeById;
		private final Map<String, Long> createdTypeIds = new HashMap<>();

		PlanApplier(String category, long batchId, Instant now, Map<Long, MarketplaceProduct> activeById) {
			this.category = category;
			this.batchId = batchId;
			this.now = now;
			this.activeById = activeById;
		}

		void apply(ImportPlan plan) {
			plan.newTypes().forEach(t -> createdTypeIds.put(t.normName(),
					types.save(new ProductType(category, t.name(), t.normName(), t.groupingMethod(), now)).getId()));
			plan.newVersions().stream().filter(v -> v.replacesProductId() != null)
					.forEach(v -> activeById.get(v.replacesProductId()).deactivate(DeactivationReason.PRICE_CHANGED, now));
			products.flush();
			plan.newVersions().forEach(this::insert);
			for (SeenAgain s : plan.seenAgain()) {
				activeById.get(s.productId()).markSeen(s.status(), batchId, now);
			}
			plan.notInLatest().forEach(id -> activeById.get(id).markNotInLatest());
		}

		private void insert(NewVersion v) {
			products.save(new MarketplaceProduct(listing(v.row()), typeId(v.type()), v.status(), batchId, now));
		}

		private long typeId(TypeRef ref) {
			return switch (ref) {
				case TypeRef.Existing e -> e.productTypeId();
				case TypeRef.Created c -> createdTypeIds.get(c.normName());
			};
		}

		private static MarketplaceProduct.Listing listing(ImportRow r) {
			return new MarketplaceProduct.Listing(r.itemId(), r.shopId(), r.shopName(), r.name().prefix(),
					r.name().core(), r.price(), r.soldText(), r.commissionRate(), r.commissionAmount(),
					r.productUrl(), r.affiliateUrl());
		}

	}

}
