package com.snapshop.dto.response;

import java.time.Instant;

import com.snapshop.model.enums.TypeKind;

/**
 * @param kind           name group (กลุ่ม E) or rule group (กลุ่ม R)
 * @param mergedIntoCode the rule group a name group was merged into; null when not merged
 * @param activeCount  items whose current version is in this type
 * @param visibleCount of those, items with status OK
 * @param onUserSite   whether the user site shows this type
 */
public record AdminProductTypeResponse(
		String code,
		String name,
		TypeKind kind,
		String mergedIntoCode,
		int activeCount,
		int visibleCount,
		boolean onUserSite,
		Instant createdAt) {
}
