package com.snapshop.service.importing;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import com.snapshop.service.importing.ImportPlan.NewType;
import com.snapshop.service.importing.TypeRef.Created;
import com.snapshop.service.importing.TypeRef.Existing;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rule-group ("กลุ่ม R") decisions for one import, part of {@link ImportPlanner}.
 *
 * <ul>
 * <li>A stored item keeps the rule group it has, whatever the file says, and is never reviewed again.</li>
 * <li>A new item joins the rule group with the same normalized name, or a new one. Joining a stored rule group
 * reopens it for the admin.</li>
 * <li>A new item in a rule group passes automatically when its name group holds at least
 * {@value #SHOWS_ON_ITS_OWN} rows in the file, since that name group shows on the site on its own; otherwise it waits
 * for the admin to merge it.</li>
 * </ul>
 */
final class RulePlanner {

	/** Rows a name group needs to show on the site by itself, as in the view {@code public_product}. */
	static final int SHOWS_ON_ITS_OWN = 2;

	private final CurrentState state;
	private final Map<String, Long> rowsByNameGroup;
	private final Map<String, NewType> newRuleTypes = new LinkedHashMap<>();
	private final Set<Long> autoPassed = new TreeSet<>();
	private final Set<Long> reopened = new TreeSet<>();

	RulePlanner(List<ImportRow> rows, CurrentState state) {
		this.state = state;
		this.rowsByNameGroup = rows.stream().collect(groupingBy(ImportRow::fileGroupLabel, counting()));
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
			reopened.add(byName);
			return new Existing(byName);
		}
		newRuleTypes.putIfAbsent(normName, new NewType(normName, row.ruleGroup().name()));
		return new Created(normName);
	}

	boolean needsReview(ImportRow row, boolean known) {
		if (known || row.ruleGroup() == null) {
			return false;
		}
		if (rowsByNameGroup.get(row.fileGroupLabel()) >= SHOWS_ON_ITS_OWN) {
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

	/** Stored rule groups a new item joins; the admin should look at them again. */
	List<Long> reopened() {
		return List.copyOf(reopened);
	}

}
