package com.snapshop.service.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.snapshop.model.enums.GroupingMethod;
import com.snapshop.model.enums.ProductStatus;
import com.snapshop.service.importing.ImportPlan.NewType;
import com.snapshop.service.importing.ImportPlan.NewVersion;
import com.snapshop.service.importing.ImportPlan.SeenAgain;
import com.snapshop.service.importing.TypeRef.Created;
import com.snapshop.service.importing.TypeRef.Existing;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ImportPlannerTest {

	final ImportPlanner planner = new ImportPlanner();
	final State state = new State();

	// ---- groups ----

	@Test
	void newGroupGetsOneNewTypeForAllItsItems() {
		ImportPlan plan = plan(row(1, "E001", "Dr.PONG Serum", 100), row(2, "E001", "Dr.PONG Serum", 120));

		assertThat(plan.newTypes()).containsExactly(
				new NewType("dr pong serum", "Dr.PONG Serum", GroupingMethod.ENGLISH_NAME_MATCH));
		assertThat(plan.newItemCount()).isEqualTo(2);
		assertThat(plan.priceChangeCount()).isZero();
		assertThat(plan.newVersions()).extracting(NewVersion::type)
				.containsExactly(new Created("dr pong serum"), new Created("dr pong serum"));
	}

	@Test
	void knownItemKeepsItsTypeEvenWhenFileSplitsItFromFormerGroupMates() {
		state.active(1, 10, 100).active(2, 10, 120);

		ImportPlan plan = plan(row(1, "E001", "Serum A", 100), row(2, "U001", "Serum B", 120));

		assertThat(plan.newTypes()).isEmpty();
		assertThat(plan.newVersions()).isEmpty();
		assertThat(plan.seenAgain()).extracting(SeenAgain::productId).containsExactlyInAnyOrder(1001L, 1002L);
	}

	@Test
	void newItemJoinsTypeOfKnownGroupMate() {
		state.active(1, 10, 100);

		ImportPlan plan = plan(row(1, "E001", "Serum", 100), row(2, "E001", "Serum", 90));

		assertThat(plan.newTypes()).isEmpty();
		assertNewVersion(single(plan.newVersions()), 2, new Existing(10), ProductStatus.OK);
	}

	@Test
	void newItemAloneJoinsExistingTypeWithSameNormalizedName() {
		state.type("dr pong barrier x serum", 10);

		ImportPlan plan = plan(row(5, "U001", "Dr. PONG BARRIER-X  Serum!", 300));

		assertThat(plan.newTypes()).isEmpty();
		assertThat(single(plan.newVersions()).type()).isEqualTo(new Existing(10));
	}

	@Test
	void itemOnceStoredButNoLongerActiveStillKeepsItsType() {
		state.history(7, 10);

		ImportPlan plan = plan(row(7, "U001", "Other name", 100));

		assertThat(plan.newTypes()).isEmpty();
		assertThat(single(plan.newVersions()).type()).isEqualTo(new Existing(10));
	}

	@Test
	void fileGroupSpanningTwoKnownTypesDoesNotMergeAndFlagsNewItemsForReview() {
		state.active(1, 20, 100).active(2, 10, 100).active(3, 10, 100);

		ImportPlan plan = plan(row(1, "E001", "S", 100), row(2, "E001", "S", 100), row(3, "E001", "S", 100),
				row(4, "E001", "S", 100));

		assertNewVersion(single(plan.newVersions()), 4, new Existing(10), ProductStatus.REVIEW);
		assertThat(plan.seenAgain()).extracting(SeenAgain::status).containsOnly(ProductStatus.OK);
	}

	@Test
	void twoFileGroupsWithSameNormalizedNameShareOneNewType() {
		ImportPlan plan = plan(row(1, "E001", "Dr.PONG Serum", 100), row(2, "R001", "dr pong serum", 100));

		assertThat(plan.newTypes()).hasSize(1);
		assertThat(plan.newVersions()).extracting(NewVersion::type).containsOnly(new Created("dr pong serum"));
	}

	// ---- prices ----

	@Test
	void samePriceOnlyMarksItemSeen() {
		state.active(1, 10, 100);

		ImportPlan plan = plan(row(1, "E001", "S", 100));

		assertThat(plan.newVersions()).isEmpty();
		assertThat(plan.seenAgain()).containsExactly(new SeenAgain(1001, ProductStatus.OK));
	}

	@Test
	void changedPriceReplacesActiveVersionAndKeepsType() {
		state.active(1, 10, 100);

		ImportPlan plan = plan(row(1, "E001", "S", 90));

		NewVersion v = single(plan.newVersions());
		assertThat(plan.priceChangeCount()).isEqualTo(1);
		assertThat(plan.newItemCount()).isZero();
		assertThat(v.replacesProductId()).isEqualTo(1001L);
		assertThat(v.type()).isEqualTo(new Existing(10));
		assertThat(v.row().price()).isEqualByComparingTo("90");
		assertThat(plan.seenAgain()).isEmpty();
	}

	@Test
	void unknownPriceIsNotComparedAndNewItemWaitsForTheFeed() {
		state.active(1, 10, 100);

		ImportPlan plan = plan(row(1, "E001", "S", null), row(2, "E001", "S", null));

		assertThat(plan.seenAgain()).containsExactly(new SeenAgain(1001, ProductStatus.OK));
		assertNewVersion(single(plan.newVersions()), 2, new Existing(10), ProductStatus.PRICE_PENDING);
	}

	@Test
	void unknownPriceOfItemStillWithoutPriceKeepsWaitingForTheFeed() {
		state.activeWithoutPrice(1, 10);

		ImportPlan plan = plan(row(1, "E001", "S", null));

		assertThat(plan.seenAgain()).containsExactly(new SeenAgain(1001, ProductStatus.PRICE_PENDING));
	}

	@Test
	void knownPriceReplacesWaitingForTheFeed() {
		state.activeWithoutPrice(1, 10);

		ImportPlan plan = plan(row(1, "E001", "S", 100));

		NewVersion v = single(plan.newVersions());
		assertThat(v.status()).isEqualTo(ProductStatus.OK);
		assertThat(v.replacesProductId()).isEqualTo(1001L);
	}

	@Test
	void unknownPriceInConflictedGroupIsReviewNotWaiting() {
		state.active(1, 20, 100).active(2, 10, 100);

		ImportPlan plan = plan(row(1, "E001", "S", 100), row(2, "E001", "S", 100), row(3, "E001", "S", null));

		assertThat(single(plan.newVersions()).status()).isEqualTo(ProductStatus.REVIEW);
	}

	// ---- items to look up in the feed ----

	@Test
	void feedLookupCoversNewItemsPriceChangesAndUnknownPricesOnly() {
		state.active(1, 10, 100).active(2, 10, 100).active(3, 10, 100).active(4, 10, 100);

		ImportPlan plan = plan(row(1, "E001", "S", 100), row(2, "E001", "S", 90), row(3, "E001", "S", null),
				row(5, "E001", "S", 50), row(4, "E001", "S", 100));

		assertThat(plan.feedItemIds()).containsExactly(2L, 3L, 5L);
	}

	@Test
	void feedLookupRetriesItemsStillWaitingForThePrice() {
		state.activeWithoutPrice(1, 10);

		ImportPlan plan = plan(row(1, "E001", "S", null));

		assertThat(plan.feedItemIds()).containsExactly(1L);
	}

	@Test
	void reviewStaysUntilAdminResolvesIt() {
		state.active(1, 10, 100, ProductStatus.REVIEW).active(2, 10, 100, ProductStatus.REVIEW);

		ImportPlan plan = plan(row(1, "E001", "S", 100), row(2, "E001", "S", 90));

		assertThat(plan.seenAgain()).containsExactly(new SeenAgain(1001, ProductStatus.REVIEW));
		assertThat(single(plan.newVersions()).status()).isEqualTo(ProductStatus.REVIEW);
	}

	// ---- items missing from this round ----

	@Test
	void activeItemMissingFromFileIsMarkedNotInLatestOnce() {
		state.active(1, 10, 100).active(2, 10, 100, ProductStatus.NOT_IN_LATEST).active(3, 10, 100);

		ImportPlan plan = plan(row(3, "E001", "S", 100));

		assertThat(plan.notInLatest()).containsExactly(1001L);
	}

	@Test
	void itemBackInFileIsShownAgain() {
		state.active(1, 10, 100, ProductStatus.NOT_IN_LATEST);

		ImportPlan plan = plan(row(1, "E001", "S", 100));

		assertThat(plan.seenAgain()).containsExactly(new SeenAgain(1001, ProductStatus.OK));
	}

	// ---- special items ----

	@Test
	void testShopAndOutOfStockItemsGetTheirOwnStatus() {
		ImportRow test = row(1, "U001", "S", 1, "Dr.PONG Collagen", "prangtest super duper");
		ImportRow soldOut = row(2, "U002", "S", 100, "[สินค้าหมด งดสั่ง] Dr.PONG 444", "Dr. Pong shop");

		ImportPlan plan = plan(test, soldOut);

		assertThat(plan.newVersions()).extracting(NewVersion::status)
				.containsExactly(ProductStatus.TEST, ProductStatus.OUT_OF_STOCK);
	}

	// ---- helpers ----

	private ImportPlan plan(ImportRow... rows) {
		return planner.plan(List.of(rows), state.build());
	}

	private static void assertNewVersion(NewVersion v, long itemId, TypeRef type, ProductStatus status) {
		assertThat(v.row().itemId()).isEqualTo(itemId);
		assertThat(v.type()).isEqualTo(type);
		assertThat(v.status()).isEqualTo(status);
		assertThat(v.replacesProductId()).isNull();
	}

	private static <T> T single(List<T> list) {
		assertThat(list).hasSize(1);
		return list.getFirst();
	}

	private static ImportRow row(long itemId, String label, String groupName, Integer price) {
		return row(itemId, label, groupName, price, "Dr.PONG item", "Shop");
	}

	private static ImportRow row(long itemId, String label, String groupName, Integer price, String name, String shop) {
		return new ImportRow(2, itemId, 9, shop, ProductName.of(name),
				price == null ? null : BigDecimal.valueOf(price), null, null, null,
				"https://shopee.co.th/product/9/" + itemId, null, label, groupName,
				GroupingMethod.ENGLISH_NAME_MATCH);
	}

	/** Existing database state for one category; product id = 1000 + item id. */
	static class State {
		final Map<Long, ActiveVersion> active = new HashMap<>();
		final Map<Long, Long> typeByItem = new HashMap<>();
		final Map<String, Long> typeByName = new HashMap<>();

		State active(long itemId, long typeId, int price) {
			return active(itemId, typeId, price, ProductStatus.OK);
		}

		State active(long itemId, long typeId, int price, ProductStatus status) {
			active.put(itemId, new ActiveVersion(1000 + itemId, itemId, typeId, BigDecimal.valueOf(price), status));
			typeByItem.put(itemId, typeId);
			return this;
		}

		State activeWithoutPrice(long itemId, long typeId) {
			active.put(itemId, new ActiveVersion(1000 + itemId, itemId, typeId, null, ProductStatus.PRICE_PENDING));
			typeByItem.put(itemId, typeId);
			return this;
		}

		State history(long itemId, long typeId) {
			typeByItem.put(itemId, typeId);
			return this;
		}

		State type(String normName, long typeId) {
			typeByName.put(normName, typeId);
			return this;
		}

		CurrentState build() {
			return new CurrentState(active, typeByItem, typeByName);
		}
	}

}
