package com.snapshop.dto.response;

import com.snapshop.model.enums.ProductStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * A rule group (กลุ่ม R) with items waiting for review, and the name groups (กลุ่ม E) its items sit in.
 *
 * @param pendingCount items of this rule group still in REVIEW
 */
public record RuleGroupReviewResponse(String code, String name, int pendingCount, List<NameGroup> nameGroups) {

	/**
	 * @param mergedIntoCode the rule group this name group was already merged into; null when not merged
	 * @param items          every current item of the name group
	 */
	public record NameGroup(String code, String name, String mergedIntoCode, List<Item> items) {
	}

	/**
	 * @param price  null while unknown
	 * @param inRule whether the item belongs to this rule group
	 */
	public record Item(long itemId, String displayName, String shopName, BigDecimal price, ProductStatus status,
			boolean inRule) {
	}

}
