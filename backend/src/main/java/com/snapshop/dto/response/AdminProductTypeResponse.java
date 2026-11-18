package com.snapshop.dto.response;

import java.time.Instant;

/**
 * @param activeCount  items whose current version is in this type
 * @param visibleCount of those, items with status OK
 * @param onUserSite   whether the user site shows this type
 */
public record AdminProductTypeResponse(
		String code,
		String name,
		int activeCount,
		int visibleCount,
		boolean onUserSite,
		Instant createdAt) {
}
