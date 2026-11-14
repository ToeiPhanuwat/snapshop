package com.snapshop.service;

import com.snapshop.dto.response.ImportPreviewResponse;
import com.snapshop.dto.response.ImportPreviewResponse.ItemLine;
import com.snapshop.dto.response.ImportPreviewResponse.PriceChange;
import com.snapshop.model.entity.ImportBatch;
import com.snapshop.model.entity.MarketplaceProduct;
import com.snapshop.model.enums.ProductStatus;
import com.snapshop.service.importing.ImportPlan;
import com.snapshop.service.importing.ImportPlan.NewType;
import com.snapshop.service.importing.ImportPlan.NewVersion;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Turns an import plan into the lists the admin reads before deciding to import. */
final class ImportPreviews {

	private ImportPreviews() {
	}

	static ImportPreviewResponse of(String fileName, ImportBatch.Counts counts, ImportPlan plan,
			Map<Long, MarketplaceProduct> activeById) {
		return new ImportPreviewResponse(fileName, counts.rows(), counts.inserted(), counts.priceChanged(),
				counts.unchanged(), counts.notInLatest(), counts.newTypes(),
				plan.newTypes().stream().map(NewType::name).toList(),
				priceChanges(plan, activeById),
				plan.notInLatest().stream().map(id -> line(activeById.get(id))).toList(),
				toReview(plan, activeById));
	}

	private static List<PriceChange> priceChanges(ImportPlan plan, Map<Long, MarketplaceProduct> activeById) {
		return plan.newVersions().stream()
				.filter(v -> v.replacesProductId() != null)
				.map(v -> new PriceChange(v.row().itemId(), v.row().name().raw(),
						activeById.get(v.replacesProductId()).getPrice(), v.row().price()))
				.toList();
	}

	private static List<ItemLine> toReview(ImportPlan plan, Map<Long, MarketplaceProduct> activeById) {
		Stream<ItemLine> newVersions = plan.newVersions().stream()
				.filter(v -> v.status() == ProductStatus.REVIEW)
				.map(ImportPreviews::line);
		Stream<ItemLine> seenAgain = plan.seenAgain().stream()
				.filter(s -> s.status() == ProductStatus.REVIEW)
				.map(s -> line(activeById.get(s.productId())));
		return Stream.concat(newVersions, seenAgain).sorted(Comparator.comparingLong(ItemLine::itemId)).toList();
	}

	private static ItemLine line(NewVersion v) {
		return new ItemLine(v.row().itemId(), v.row().name().raw());
	}

	private static ItemLine line(MarketplaceProduct p) {
		return new ItemLine(p.getItemId(), p.getNameRaw());
	}

}
