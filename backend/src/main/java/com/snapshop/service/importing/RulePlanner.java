package com.snapshop.service.importing;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import com.snapshop.service.importing.ImportPlan.NewType;
import com.snapshop.service.importing.TypeRef.Created;
import com.snapshop.service.importing.TypeRef.Existing;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Rule-group ("กลุ่ม R") decisions for one import, part of {@link ImportPlanner}.
 *
 * <ul>
 * <li>A stored item keeps the rule group it has, whatever the file says, and is never reviewed again.</li>
 * <li>A new item joins the rule group with the same normalized name, or a new one.</li>
 * <li>A new item in a rule group waits for review, unless its rule group holds exactly the items of its name group
 * in the file: then both rules agree and it passes automatically.</li>
 * </ul>
 */
final class RulePlanner {

	private final CurrentState state;
	private final Set<Long> agreeingItems;
	private final Map<String, NewType> newRuleTypes = new LinkedHashMap<>();
	private final Set<Long> autoPassed = new TreeSet<>();

	RulePlanner(List<ImportRow> rows, CurrentState state) {
		this.state = state;
		this.agreeingItems = agreeingItems(rows);
	}

	/** The rule group of the row's item; null when it has none. */
	TypeRef ruleTypeOf(ImportRow row, boolean known) {
		if (known) {
			Long stored = state.ruleTypeByItem().get(row.itemId());
			return stored == null ? null : new Existing(stored);
		}
		if (row.ruleGroup() == null) {
			return null;
		}
		String normName = TypeNames.normalize(row.ruleGroup().name());
		Long byName = state.ruleTypeByNormName().get(normName);
		if (byName != null) {
			return new Existing(byName);
		}
		newRuleTypes.putIfAbsent(normName, new NewType(normName, row.ruleGroup().name()));
		return new Created(normName);
	}

	boolean needsReview(ImportRow row, boolean known) {
		if (known || row.ruleGroup() == null) {
			return false;
		}
		if (agreeingItems.contains(row.itemId())) {
			autoPassed.add(row.itemId());
			return false;
		}
		return true;
	}

	List<NewType> newRuleTypes() {
		return List.copyOf(newRuleTypes.values());
	}

	List<Long> autoPassed() {
		return List.copyOf(autoPassed);
	}

	/** Items whose rule group in the file has exactly the same items as their name group in the file. */
	private static Set<Long> agreeingItems(List<ImportRow> rows) {
		List<ImportRow> ruleRows = rows.stream().filter(r -> r.ruleGroup() != null).toList();
		Map<String, Set<Long>> byName = members(rows, ImportRow::fileGroupLabel);
		Map<String, Set<Long>> byRule = members(ruleRows, r -> r.ruleGroup().label());
		return ruleRows.stream()
				.filter(r -> byRule.get(r.ruleGroup().label()).equals(byName.get(r.fileGroupLabel())))
				.map(ImportRow::itemId)
				.collect(toSet());
	}

	private static Map<String, Set<Long>> members(List<ImportRow> rows, Function<ImportRow, String> label) {
		return rows.stream().collect(groupingBy(label, mapping(ImportRow::itemId, toSet())));
	}

}
