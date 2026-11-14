package com.snapshop.service.importing;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import com.snapshop.model.enums.ProductStatus;
import com.snapshop.service.importing.ImportPlan.NewType;
import com.snapshop.service.importing.ImportPlan.NewVersion;
import com.snapshop.service.importing.ImportPlan.SeenAgain;
import com.snapshop.service.importing.TypeRef.Created;
import com.snapshop.service.importing.TypeRef.Existing;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Decides what an import changes. Pure: same rows and state always give the same plan.
 *
 * <p>Group rules, per group label in the file:
 * <ol>
 * <li>an item already stored keeps its type, whatever the file says;</li>
 * <li>a new item takes the type of its known group mates (the most common one);</li>
 * <li>otherwise the type whose normalized name equals the file's group name;</li>
 * <li>otherwise a new type.</li>
 * </ol>
 * When known group mates belong to different types, nothing is merged and new items are marked REVIEW.
 */
@Component
public class ImportPlanner {

	public ImportPlan plan(List<ImportRow> rows, CurrentState state) {
		PlanBuilder plan = new PlanBuilder(state);
		rows.stream()
				.collect(Collectors.groupingBy(ImportRow::fileGroupLabel, LinkedHashMap::new, Collectors.toList()))
				.values()
				.forEach(plan::addFileGroup);
		plan.hideMissing(rows);
		return plan.build();
	}

	private static final class PlanBuilder {

		private final CurrentState state;
		private final Map<String, NewType> newTypes = new LinkedHashMap<>();
		private final List<NewVersion> newVersions = new ArrayList<>();
		private final List<SeenAgain> seenAgain = new ArrayList<>();
		private final List<Long> notInLatest = new ArrayList<>();

		PlanBuilder(CurrentState state) {
			this.state = state;
		}

		void addFileGroup(List<ImportRow> group) {
			Map<Long, Long> knownTypeCounts = group.stream()
					.map(r -> state.typeByItem().get(r.itemId()))
					.filter(Objects::nonNull)
					.collect(groupingBy(Function.identity(), counting()));
			boolean conflict = knownTypeCounts.size() > 1;
			TypeRef typeForNewItems = group.stream().allMatch(this::isKnown) ? null
					: typeForNewItems(group.getFirst(), knownTypeCounts);
			for (ImportRow row : group) {
				boolean known = isKnown(row);
				addRow(row, known ? new Existing(state.typeByItem().get(row.itemId())) : typeForNewItems,
						conflict && !known);
			}
		}

		void hideMissing(List<ImportRow> rows) {
			Set<Long> inFile = rows.stream().map(ImportRow::itemId).collect(Collectors.toSet());
			state.activeByItem().values().stream()
					.filter(v -> !inFile.contains(v.itemId()) && v.status() != ProductStatus.NOT_IN_LATEST)
					.map(ActiveVersion::productId)
					.sorted()
					.forEach(notInLatest::add);
		}

		ImportPlan build() {
			return new ImportPlan(List.copyOf(newTypes.values()), newVersions, seenAgain, notInLatest);
		}

		private boolean isKnown(ImportRow row) {
			return state.typeByItem().containsKey(row.itemId());
		}

		private TypeRef typeForNewItems(ImportRow first, Map<Long, Long> knownTypeCounts) {
			if (!knownTypeCounts.isEmpty()) {
				return new Existing(mostCommon(knownTypeCounts));
			}
			String normName = TypeNames.normalize(first.groupName());
			Long byName = state.typeByNormName().get(normName);
			if (byName != null) {
				return new Existing(byName);
			}
			newTypes.putIfAbsent(normName, new NewType(normName, first.groupName(), first.groupingMethod()));
			return new Created(normName);
		}

		private void addRow(ImportRow row, TypeRef type, boolean needsReview) {
			ProductStatus status = statusOf(row, needsReview);
			ActiveVersion active = state.activeByItem().get(row.itemId());
			if (active == null) {
				newVersions.add(new NewVersion(row, type, status, null));
				return;
			}
			ProductStatus kept = keepReview(active.status(), status);
			if (row.price() == null || samePrice(active.price(), row.price())) {
				seenAgain.add(new SeenAgain(active.productId(), kept));
			} else {
				newVersions.add(new NewVersion(row, type, kept, active.productId()));
			}
		}

	}

	/** Most members; on a tie the oldest (smallest id) type, so the result never depends on map order. */
	private static long mostCommon(Map<Long, Long> counts) {
		return counts.entrySet().stream()
				.max(Comparator.<Entry<Long, Long>>comparingLong(Entry::getValue)
						.thenComparing(Entry::getKey, Comparator.reverseOrder()))
				.orElseThrow()
				.getKey();
	}

	static ProductStatus statusOf(ImportRow row, boolean needsReview) {
		if (row.shopName().toLowerCase(Locale.ROOT).contains("test")) {
			return ProductStatus.TEST;
		}
		if (row.name().raw().contains("สินค้าหมด")) {
			return ProductStatus.OUT_OF_STOCK;
		}
		if (row.price() == null || needsReview) {
			return ProductStatus.REVIEW;
		}
		return ProductStatus.OK;
	}

	/** REVIEW stays until an admin resolves it. */
	private static ProductStatus keepReview(ProductStatus before, ProductStatus now) {
		return before == ProductStatus.REVIEW && now == ProductStatus.OK ? ProductStatus.REVIEW : now;
	}

	private static boolean samePrice(BigDecimal stored, BigDecimal incoming) {
		return stored != null && stored.compareTo(incoming) == 0;
	}

}
