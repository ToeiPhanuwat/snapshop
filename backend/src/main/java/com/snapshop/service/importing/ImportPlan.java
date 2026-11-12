package com.snapshop.service.importing;

import com.snapshop.model.enums.GroupingMethod;
import com.snapshop.model.enums.ProductStatus;
import java.util.List;
import java.util.Objects;

/**
 * Everything one import will change, decided without touching the database.
 *
 * @param newTypes    product types to create
 * @param newVersions versions to insert as the current one (new items, and items whose price changed)
 * @param seenAgain   current versions that stay, with their status for this round
 * @param notInLatest current versions not in this file, to be hidden
 */
public record ImportPlan(
		List<NewType> newTypes,
		List<NewVersion> newVersions,
		List<SeenAgain> seenAgain,
		List<Long> notInLatest) {

	public ImportPlan {
		newTypes = List.copyOf(newTypes);
		newVersions = List.copyOf(newVersions);
		seenAgain = List.copyOf(seenAgain);
		notInLatest = List.copyOf(notInLatest);
	}

	public long priceChangeCount() {
		return newVersions.stream().filter(v -> v.replacesProductId() != null).count();
	}

	public long newItemCount() {
		return newVersions.size() - priceChangeCount();
	}

	public record NewType(String normName, String name, GroupingMethod groupingMethod) {
	}

	/** @param replacesProductId current version this one replaces because the price changed; null for a new item */
	public record NewVersion(ImportRow row, TypeRef type, ProductStatus status, Long replacesProductId) {

		public NewVersion {
			Objects.requireNonNull(row);
			Objects.requireNonNull(type);
			Objects.requireNonNull(status);
		}

	}

	public record SeenAgain(long productId, ProductStatus status) {
	}

}
