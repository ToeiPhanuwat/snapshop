package com.snapshop.service.feed;

import static com.snapshop.TestCsv.csv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snapshop.IntegrationTest;
import com.snapshop.service.ImportService;
import com.snapshop.service.feed.FeedResult.Outcome;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The "running" row must be stored before the caller gets its answer, so a page that reloads right away already
 * sees the look-up and keeps its button disabled. The feed here blocks until the test lets it go.
 */
@IntegrationTest
@TestPropertySource(properties = "snapshop.feed.url=http://feed.invalid/feed.csv")
class FeedSyncStartTest {

	@Autowired
	ImportService importer;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	@MockitoBean
	FeedClient client;

	CountDownLatch release = new CountDownLatch(1);

	@BeforeEach
	void blockingFeed() {
		jdbc.sql("TRUNCATE feed_sync_run, marketplace_product, product_type, import_batch RESTART IDENTITY CASCADE")
				.update();
		when(client.fetch(anySet())).thenAnswer(call -> {
			release.await(10, TimeUnit.SECONDS);
			return new FeedResult(Map.of(), Outcome.DONE, "ok");
		});
	}

	@AfterEach
	void letTheFeedFinish() throws InterruptedException {
		release.countDown();
		Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
		while (running() > 0 && Instant.now().isBefore(deadline)) {
			Thread.sleep(50);
		}
	}

	@Test
	void importStoresTheRunningLookupBeforeItReturns() throws Exception {
		try (InputStream in = csv().item(1, "100", "E001", "Serum").file().getInputStream()) {
			importer.importFile("DRPONG", "t.csv", in);
		}

		assertThat(running()).isEqualTo(1);
	}

	@Test
	void retryStoresTheRunningLookupBeforeItAnswersAndRefusesASecondOne() throws Exception {
		try (InputStream in = csv().item(1, "100", "E001", "Serum").file().getInputStream()) {
			importer.importFile("DRPONG", "t.csv", in);
		}
		release.countDown();
		letTheFeedFinish();
		release = new CountDownLatch(1);

		mvc.perform(post("/api/admin/feed-syncs").with(httpBasic("test-admin", "test-admin-password")))
				.andExpect(status().isAccepted());

		assertThat(running()).isEqualTo(1);
		mvc.perform(post("/api/admin/feed-syncs").with(httpBasic("test-admin", "test-admin-password")))
				.andExpect(status().isConflict());
	}

	private long running() {
		return jdbc.sql("SELECT count(*) FROM feed_sync_run WHERE outcome = 'RUNNING'").query(Long.class).single();
	}

}
