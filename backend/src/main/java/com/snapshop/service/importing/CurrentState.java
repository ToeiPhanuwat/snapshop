package com.snapshop.service.importing;

import java.util.Map;

/**
 * What the database holds for one category before an import.
 *
 * @param activeByItem       current version of each item
 * @param typeByItem         name group (กลุ่ม E) of every item ever stored, active or not
 * @param typeByNormName     name groups by normalized name
 * @param ruleTypeByItem     rule group (กลุ่ม R) of every item ever stored that has one
 * @param ruleTypeByNormName rule groups by normalized name
 */
public record CurrentState(
		Map<Long, ActiveVersion> activeByItem,
		Map<Long, Long> typeByItem,
		Map<String, Long> typeByNormName,
		Map<Long, Long> ruleTypeByItem,
		Map<String, Long> ruleTypeByNormName) {

	public CurrentState {
		activeByItem = Map.copyOf(activeByItem);
		typeByItem = Map.copyOf(typeByItem);
		typeByNormName = Map.copyOf(typeByNormName);
		ruleTypeByItem = Map.copyOf(ruleTypeByItem);
		ruleTypeByNormName = Map.copyOf(ruleTypeByNormName);
	}

}
