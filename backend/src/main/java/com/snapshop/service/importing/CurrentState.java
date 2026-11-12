package com.snapshop.service.importing;

import java.util.Map;

/**
 * What the database holds for one category before an import.
 *
 * @param activeByItem   current version of each item
 * @param typeByItem     product type of every item ever stored, active or not
 * @param typeByNormName product types by normalized name
 */
public record CurrentState(
		Map<Long, ActiveVersion> activeByItem,
		Map<Long, Long> typeByItem,
		Map<String, Long> typeByNormName) {

	public CurrentState {
		activeByItem = Map.copyOf(activeByItem);
		typeByItem = Map.copyOf(typeByItem);
		typeByNormName = Map.copyOf(typeByNormName);
	}

}
