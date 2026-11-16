package com.snapshop.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snapshop.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@IntegrationTest
class AdminFeedControllerTest {

	static final RequestPostProcessor ADMIN = httpBasic("test-admin", "test-admin-password");

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	@BeforeEach
	void emptyLog() {
		jdbc.sql("TRUNCATE feed_sync_run").update();
	}

	@Test
	void listsRecentLookupsNewestFirst() throws Exception {
		jdbc.sql("""
				INSERT INTO feed_sync_run (started_at, requested_count, found_count, outcome, message)
				VALUES (now(), 3, 3, 'DONE', 'a'), (now(), 5, 0, 'RATE_LIMITED', 'Retry-After: 60')""").update();

		mvc.perform(get("/api/admin/feed-syncs").with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].outcome").value("RATE_LIMITED"))
				.andExpect(jsonPath("$[0].requestedCount").value(5))
				.andExpect(jsonPath("$[1].outcome").value("DONE"));
	}

	@Test
	void retryIsRefusedWhenNoFeedLinkIsConfigured() throws Exception {
		mvc.perform(post("/api/admin/feed-syncs").with(ADMIN)).andExpect(status().isConflict());
	}

	@Test
	void needsAdminLogin() throws Exception {
		mvc.perform(get("/api/admin/feed-syncs")).andExpect(status().isUnauthorized());
	}

}
