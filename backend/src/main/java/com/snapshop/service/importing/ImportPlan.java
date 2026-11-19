package com.snapshop.service.importing;

import com.snapshop.model.enums.ProductStatus;
import java.util.List;
import java.util.Objects;

/**
 * Everything one import will change, decided without touching the database.
 *
 * @param newTypes          name groups (กลุ่ม E) to create
 * @param newRuleTypes      rule groups (กลุ่ม R) to create
 * @param newVersions       versions to insert as the current one (new items, and items whose price changed)
 * @param seenAgain         current versions that stay, with their status for this round
 * @param notInLatest       current versions not in this file, to be hidden
 * @param feedItemIds       items to look up in the feed afterwards, ascending
 * @param autoPassedItemIds new items in a rule group that skip review because their name group shows on its own, ascending
 * @param reopenedRuleTypeIds stored rule groups a new item joins, to show to the admin again, ascending
 */
public record ImportPlan(
		List<NewType> newTypes,
		List<NewType> newRuleTypes,
		List<NewVersion> newVersions,
		List<SeenAgain> seenAgain,
		List<Long> notInLatest,
		List<Long> feedItemIds,
		List<Long> autoPassedItemIds,
		List<Long> reopenedRuleTypeIds) {

	public ImportPlan {
		newTypes = List.copyOf(newTypes);
		newRuleTypes = List.copyOf(newRuleTypes);
		newVersions = List.copyOf(newVersions);
		seenAgain = List.copyOf(seenAgain);
		notInLatest = List.copyOf(notInLatest);
		feedItemIds = List.copyOf(feedItemIds);
		autoPassedItemIds = List.copyOf(autoPassedItemIds);
		reopenedRuleTypeIds = List.copyOf(reopenedRuleTypeIds);
	}

	public long priceChangeCount() {
		return newVersions.stream().filter(v -> v.replacesProductId() != null).count();
	}

	public long newItemCount() {
		return newVersions.size() - priceChangeCount();
	}

	/** New items in a rule group that wait for the admin to compare it with the name groups. */
	public long ruleReviewCount() {
		return newVersions.stream()
				.filter(v -> v.replacesProductId() == null && v.ruleType() != null && v.status() == ProductStatus.REVIEW)
				.count();
	}

	public record NewType(String normName, String name) {
	}

	/**
	 * @param ruleType          the item's rule group; null when it has none
	 * @param replacesProductId current version this one replaces because the price changed; null for a new item
	 */
	public record NewVersion(ImportRow row, TypeRef type, TypeRef ruleType, ProductStatus status,
			Long replacesProductId) {

		public NewVersion {
			Objects.requireNonNull(row);
			Objects.requireNonNull(type);
			Objects.requireNonNull(status);
		}

	}

	public record SeenAgain(long productId, ProductStatus status) {
	}

}
