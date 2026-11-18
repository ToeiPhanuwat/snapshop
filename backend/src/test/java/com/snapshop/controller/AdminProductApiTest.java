package com.snapshop.controller;

import static com.snapshop.TestCsv.csv;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snapshop.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@IntegrationTest
class AdminProductApiTest {

	static final RequestPostProcessor ADMIN = httpBasic("test-admin", "test-admin-password");

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	@BeforeEach
	void importCatalog() throws Exception {
		jdbc.sql("TRUNCATE marketplace_product, product_type, import_batch RESTART IDENTITY CASCADE").update();
		mvc.perform(multipart("/api/admin/imports").file(PublicApiTest.baseCatalog().file()).with(ADMIN))
				.andExpect(status().isOk());
	}

	@Test
	void adminSeesEveryCurrentItemAndCanFilterByStatus() throws Exception {
		mvc.perform(get("/api/admin/products").with(ADMIN))
				.andExpect(jsonPath("$.total").value(5));
		mvc.perform(get("/api/admin/products").param("status", "PRICE_PENDING").with(ADMIN))
				.andExpect(jsonPath("$.items[*].itemId", contains(5)))
				.andExpect(jsonPath("$.items[0].onUserSite").value(false));
	}

	@Test
	void adminSeesSingleItemGroupsMarkedOffSite() throws Exception {
		mvc.perform(get("/api/admin/product-types").param("q", "cream").with(ADMIN))
				.andExpect(jsonPath("$.items[0].name").value("Cream"))
				.andExpect(jsonPath("$.items[0].activeCount").value(1))
				.andExpect(jsonPath("$.items[0].visibleCount").value(1))
				.andExpect(jsonPath("$.items[0].onUserSite").value(false));
	}

	@Test
	void resolvingConflictReviewPutsNewShopOnUserSite() throws Exception {
		// file group X001 mixes Serum item 1 and Cream item 3: new item 7 joins one of them and needs review
		mvc.perform(multipart("/api/admin/imports").file(csv()
				.item(1, "promo Dr.PONG Serum 15ml", "100", "Shop A", "X001", "Dr.PONG Serum")
				.item(2, "Dr.PONG Serum 16ml", "120", "Shop B", "E001", "Dr.PONG Serum")
				.item(3, "50", "X001", "Cream")
				.item(7, "Dr.PONG Serum 30ml", "150", "Shop G", "X001", "Dr.PONG Serum")
				.file()).with(ADMIN));
		mvc.perform(get("/api/admin/products").param("status", "REVIEW").with(ADMIN))
				.andExpect(jsonPath("$.items[*].itemId", contains(7)));

		mvc.perform(post("/api/admin/products/{itemId}/resolve", 7).with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OK"))
				.andExpect(jsonPath("$.onUserSite").value(true));
		mvc.perform(get("/api/product-types/{code}", code("Dr.PONG Serum")))
				.andExpect(jsonPath("$.offerCount").value(3));
	}

	@Test
	void resolvingIsRefusedWhenNotInReviewOrPriceStillUnknown() throws Exception {
		mvc.perform(post("/api/admin/products/{itemId}/resolve", 1).with(ADMIN)).andExpect(status().isConflict());
		mvc.perform(post("/api/admin/products/{itemId}/resolve", 5).with(ADMIN)).andExpect(status().isConflict());
	}

	@Test
	void movingItemToAnotherGroupMovesItsWholeHistory() throws Exception {
		mvc.perform(json(post("/api/admin/products/{itemId}/move", 3), "{\"productTypeCode\":\"" + code("Dr.PONG Serum") + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productTypeCode").value(code("Dr.PONG Serum")));

		mvc.perform(get("/api/product-types/{code}", code("Dr.PONG Serum")))
				.andExpect(jsonPath("$.offerCount").value(3));
	}

	@Test
	void movingToUnknownGroupIsNotFoundAndBlankCodeIsBadRequest() throws Exception {
		mvc.perform(json(post("/api/admin/products/{itemId}/move", 3), "{\"productTypeCode\":\"PT999999\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(json(post("/api/admin/products/{itemId}/move", 3), "{\"productTypeCode\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deactivatingDiscontinuedItemRemovesItFromBothSites() throws Exception {
		mvc.perform(post("/api/admin/products/{itemId}/deactivate", 2).with(ADMIN)).andExpect(status().isNoContent());

		mvc.perform(get("/api/products/{itemId}", 2)).andExpect(status().isNotFound());
		mvc.perform(get("/api/product-types")).andExpect(jsonPath("$.total").value(0));
		mvc.perform(get("/api/admin/products").with(ADMIN)).andExpect(jsonPath("$.total").value(4));
		mvc.perform(post("/api/admin/products/{itemId}/deactivate", 2).with(ADMIN)).andExpect(status().isNotFound());
	}

	@Test
	void adminEndpointsNeedLogin() throws Exception {
		mvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/admin/products/{itemId}/deactivate", 2)).andExpect(status().isUnauthorized());
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private String code(String typeName) {
		return jdbc.sql("SELECT product_type_code FROM product_type WHERE type_name = ?").param(typeName)
				.query(String.class).single();
	}

}
