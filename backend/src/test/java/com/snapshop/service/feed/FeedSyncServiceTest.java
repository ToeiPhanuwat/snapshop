package com.snapshop.service.feed;

import static com.snapshop.TestCsv.csv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snapshop.IntegrationTest;
import com.snapshop.TestCsv;
import com.snapshop.dto.response.ImportResultResponse;
import com.snapshop.exception.BusinessRuleException;
import com.snapshop.model.enums.ShopType;
import com.snapshop.service.ImportService;
import com.snapshop.service.feed.FeedResult.Outcome;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@IntegrationTest
@RecordApplicationEvents
class FeedSyncServiceTest {

	@Autowired
	FeedSyncService sync;

	@Autowired
	ImportService importer;

	@Autowired
	JdbcClient jdbc;

	@MockitoBean
	FeedClient client;

	@Autowired
	ApplicationEvents events;

	@BeforeEach
	void emptyDatabase() {
		jdbc.sql("TRUNCATE feed_sync_run, marketplace_product, product_type, import_batch RESTART IDENTITY CASCADE")
				.update();
	}

	@Test
	void feedFillsPriceOfAbbreviatedItemAndExtrasOfEveryItem() throws IOException {
		ImportResultResponse batch = importFile(csv().item(1, "100", "E001", "Serum").item(2, "1.0พัน", "E001", "Serum"));
		assertThat(status(2)).isEqualTo("PRICE_PENDING");
		when(client.fetch(anySet())).thenReturn(done(record(1, "100", ShopType.MALL), record(2, "79", ShopType.PREFERRED)));

		var run = sync.sync(new FeedRequest(batch.batchId(), Set.of(1L, 2L), Set.of(2L)));

		assertThat(run.outcome()).isEqualTo("DONE");
		assertThat(run.requestedCount()).isEqualTo(2);
		assertThat(run.foundCount()).isEqualTo(2);
		assertThat(status(2)).isEqualTo("OK");
		assertThat(price(2)).isEqualByComparingTo("79");
		assertThat(text("SELECT shop_type FROM marketplace_product WHERE item_id = 2")).isEqualTo("PREFERRED");
		assertThat(text("SELECT image_url FROM marketplace_product WHERE item_id = 1")).isEqualTo("https://cf/1");
		assertThat(count("SELECT count(*) FROM marketplace_product WHERE feed_synced_at IS NOT NULL")).isEqualTo(2);
		assertThat(count("SELECT count(*) FROM public_product")).isEqualTo(2);
	}

	@Test
	void changedFeedPriceOfAbbreviatedItemKeepsThePriceHistory() throws IOException {
		ImportResultResponse first = importFile(csv().item(1, "100", "E001", "Serum").item(2, "1.0พัน", "E001", "Serum"));
		when(client.fetch(anySet())).thenReturn(done(record(2, "79", ShopType.REGULAR)));
		sync.sync(new FeedRequest(first.batchId(), Set.of(2L), Set.of(2L)));
		ImportResultResponse second = importFile(csv().item(1, "100", "E001", "Serum").item(2, "1.1พัน", "E001", "Serum"));
		when(client.fetch(anySet())).thenReturn(done(record(2, "69", ShopType.REGULAR)));

		sync.sync(new FeedRequest(second.batchId(), Set.of(2L), Set.of(2L)));

		assertThat(price(2)).isEqualByComparingTo("69");
		assertThat(count("SELECT count(*) FROM marketplace_product WHERE item_id = 2")).isEqualTo(2);
		assertThat(count("SELECT count(*) FROM marketplace_product WHERE item_id = 2 AND active_flag = 'N'"))
				.isEqualTo(1);
		assertThat(text("SELECT image_url FROM marketplace_product WHERE item_id = 2 AND active_flag = 'Y'"))
				.isEqualTo("https://cf/2");
	}

	@Test
	void failedLookupIsRecordedAndItemStaysWaiting() throws IOException {
		ImportResultResponse batch = importFile(csv().item(1, "100", "E001", "Serum").item(2, "1.0พัน", "E001", "Serum"));
		when(client.fetch(anySet())).thenReturn(new FeedResult(Map.of(), Outcome.RATE_LIMITED, "Retry-After: 1800"));

		var run = sync.sync(new FeedRequest(batch.batchId(), Set.of(2L), Set.of(2L)));

		assertThat(run.outcome()).isEqualTo("RATE_LIMITED");
		assertThat(run.message()).contains("1800");
		assertThat(status(2)).isEqualTo("PRICE_PENDING");
	}

	@Test
	void importAsksForNewItemsPriceChangesAndAbbreviatedPrices() throws IOException {
		importFile(csv().item(1, "100", "E001", "Serum").item(2, "100", "E001", "Serum").item(3, "1.0พัน", "E001", "Serum"));

		ImportResultResponse second = importFile(csv().item(1, "100", "E001", "Serum").item(2, "90", "E001", "Serum")
				.item(3, "1.0พัน", "E001", "Serum").item(4, "50", "U001", "Cream"));

		var requests = events.stream(FeedRequest.class).toList();
		assertThat(requests).hasSize(2);
		FeedRequest latest = requests.getLast();
		assertThat(latest.batchId()).isEqualTo(second.batchId());
		assertThat(latest.itemIds()).containsExactlyInAnyOrder(2L, 3L, 4L);
		assertThat(latest.feedPricedItemIds()).containsExactly(3L);
	}

	@Test
	void retryAsksForItemsStillWaitingOrNeverSynced() throws IOException {
		ImportResultResponse batch = importFile(csv().item(1, "100", "E001", "Serum").item(2, "1.0พัน", "E001", "Serum"));
		when(client.fetch(anySet())).thenReturn(done(record(1, "100", ShopType.MALL)));
		sync.sync(new FeedRequest(batch.batchId(), Set.of(1L), Set.of()));

		FeedRequest retry = sync.pendingRequest();

		assertThat(retry.itemIds()).containsExactly(2L);
		assertThat(retry.feedPricedItemIds()).isEmpty();
	}

	@Test
	void retryWithNothingToDoIsRefusedWithoutAskingTheFeed() throws IOException {
		importFile(csv().item(1, "100", "E001", "Serum"));
		jdbc.sql("UPDATE marketplace_product SET feed_synced_at = now()").update();

		assertThatThrownBy(() -> sync.pendingRequest()).isInstanceOf(BusinessRuleException.class);
		verify(client, never()).fetch(anySet());
	}

	private ImportResultResponse importFile(TestCsv csv) throws IOException {
		try (var in = csv.file().getInputStream()) {
			return importer.importFile("DRPONG", "t.csv", in);
		}
	}

	private static FeedResult done(FeedRecord... records) {
		return new FeedResult(
				java.util.Arrays.stream(records).collect(java.util.stream.Collectors.toMap(FeedRecord::itemId, r -> r)),
				Outcome.DONE, "ok");
	}

	private static FeedRecord record(long itemId, String salePrice, ShopType shopType) {
		return new FeedRecord(itemId, 77, new BigDecimal(salePrice), new BigDecimal("150"), 9, new BigDecimal("4.5"),
				new BigDecimal("4.8"), shopType, "https://cf/" + itemId);
	}

	private String status(long itemId) {
		return text("SELECT status FROM marketplace_product WHERE item_id = " + itemId + " AND active_flag = 'Y'");
	}

	private BigDecimal price(long itemId) {
		return jdbc.sql("SELECT price FROM marketplace_product WHERE item_id = " + itemId + " AND active_flag = 'Y'")
				.query(BigDecimal.class).single();
	}

	private String text(String sql) {
		return jdbc.sql(sql).query(String.class).single();
	}

	private long count(String sql) {
		return jdbc.sql(sql).query(Long.class).single();
	}

}
