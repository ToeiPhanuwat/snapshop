package com.snapshop.service.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.snapshop.model.enums.ProductStatus;
import com.snapshop.model.enums.ShopType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FeedPlannerTest {

	final FeedPlanner planner = new FeedPlanner();

	@Test
	void fillsMissingPriceAndShowsItemWaitingForIt() {
		FeedPlan plan = plan(List.of(record(1, 7, "79")), Map.of(1L, target(1, 7, null, ProductStatus.PRICE_PENDING)),
				Set.of(1L));

		assertThat(plan.updates()).singleElement().satisfies(u -> {
			assertThat(u.productId()).isEqualTo(1001L);
			assertThat(u.fillPrice()).isTrue();
		});
		assertThat(plan.newPrices()).isEmpty();
	}

	@Test
	void feedPriceOfAbbreviatedItemThatDiffersMakesNewPriceVersion() {
		FeedPlan plan = plan(List.of(record(1, 7, "79")), Map.of(1L, target(1, 7, "90", ProductStatus.OK)), Set.of(1L));

		assertThat(plan.newPrices()).singleElement().satisfies(n -> {
			assertThat(n.replacesProductId()).isEqualTo(1001L);
			assertThat(n.record().salePrice()).isEqualByComparingTo("79");
		});
		assertThat(plan.updates()).isEmpty();
	}

	@Test
	void feedPriceEqualToStoredPriceOnlyRefreshesExtras() {
		FeedPlan plan = plan(List.of(record(1, 7, "90.00")), Map.of(1L, target(1, 7, "90", ProductStatus.OK)),
				Set.of(1L));

		assertThat(plan.newPrices()).isEmpty();
		assertThat(plan.updates()).singleElement().satisfies(u -> assertThat(u.fillPrice()).isFalse());
	}

	@Test
	void fileSuppliedPriceIsNeverReplacedByTheFeedPrice() {
		FeedPlan plan = plan(List.of(record(1, 7, "79")), Map.of(1L, target(1, 7, "90", ProductStatus.OK)), Set.of());

		assertThat(plan.newPrices()).isEmpty();
		assertThat(plan.updates()).singleElement().satisfies(u -> assertThat(u.fillPrice()).isFalse());
	}

	@Test
	void feedWithoutSalePriceLeavesItemWaiting() {
		FeedPlan plan = plan(List.of(record(1, 7, null)), Map.of(1L, target(1, 7, null, ProductStatus.PRICE_PENDING)),
				Set.of(1L));

		assertThat(plan.updates()).singleElement().satisfies(u -> assertThat(u.fillPrice()).isFalse());
	}

	@Test
	void recordOfAnotherShopOrUnknownItemIsSkipped() {
		FeedPlan plan = plan(List.of(record(1, 8, "79"), record(2, 7, "79")),
				Map.of(1L, target(1, 7, null, ProductStatus.PRICE_PENDING)), Set.of(1L));

		assertThat(plan.updates()).isEmpty();
		assertThat(plan.newPrices()).isEmpty();
		assertThat(plan.skippedCount()).isEqualTo(2);
	}

	private FeedPlan plan(List<FeedRecord> records, Map<Long, FeedTarget> targets, Set<Long> feedPriced) {
		return planner.plan(records, targets, feedPriced);
	}

	private static FeedTarget target(long itemId, long shopId, String price, ProductStatus status) {
		return new FeedTarget(1000 + itemId, itemId, shopId, price == null ? null : new BigDecimal(price), status);
	}

	private static FeedRecord record(long itemId, long shopId, String salePrice) {
		return new FeedRecord(itemId, shopId, salePrice == null ? null : new BigDecimal(salePrice),
				new BigDecimal("100"), 5, new BigDecimal("4.5"), new BigDecimal("4.8"), ShopType.PREFERRED,
				"https://cf/" + itemId);
	}

}
