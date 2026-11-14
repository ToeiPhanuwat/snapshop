package com.snapshop.controller;

import static com.snapshop.TestCsv.csv;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snapshop.IntegrationTest;
import com.snapshop.TestCsv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class PublicApiTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	@BeforeEach
	void importCatalog() throws Exception {
		jdbc.sql("TRUNCATE marketplace_product, product_type, import_batch RESTART IDENTITY CASCADE").update();
		upload(baseCatalog());
	}

	/** Serum: 2 shops; Cream: 1 shop; Toner: 1 shop OK + 1 shop REVIEW (abbreviated price). */
	static TestCsv baseCatalog() {
		return csv()
				.item(1, "promo Dr.PONG Serum 15ml", "100", "Shop A", "Mall", "E001", "Dr.PONG Serum")
				.item(2, "Dr.PONG Serum 16ml", "120", "Shop B", "ร้านแนะนำ", "E001", "Dr.PONG Serum")
				.item(3, "50", "U001", "Cream")
				.item(4, "80", "E002", "Toner")
				.item(5, "1.0พัน", "E002", "Toner");
	}

	@Test
	void listsOnlyGroupsWithAtLeastTwoVisibleShops() throws Exception {
		mvc.perform(get("/api/product-types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.items[0].name").value("Dr.PONG Serum"))
				.andExpect(jsonPath("$.items[0].code").isString())
				.andExpect(jsonPath("$.items[0].offerCount").value(2))
				.andExpect(jsonPath("$.items[0].minPrice").value(100))
				.andExpect(jsonPath("$.items[0].maxPrice").value(120));
	}

	@Test
	void groupDetailListsOffersCheapestFirstWithDisplayNameAndLink() throws Exception {
		String code = serumCode();

		mvc.perform(get("/api/product-types/{code}", code))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.offers[*].itemId", contains(1, 2)))
				.andExpect(jsonPath("$.offers[0].displayName").value("promo Dr.PONG Serum 15ml"))
				.andExpect(jsonPath("$.offers[0].namePrefix").value("promo "))
				.andExpect(jsonPath("$.offers[0].shopType").value("MALL"))
				.andExpect(jsonPath("$.offers[0].link").value("https://s.shopee.co.th/a1"))
				.andExpect(jsonPath("$.offers[1].shopType").value("PREFERRED"));
	}

	@Test
	void hiddenGroupIsNotFound() throws Exception {
		String creamCode = jdbc.sql("SELECT product_type_code FROM product_type WHERE type_name = 'Cream'")
				.query(String.class).single();

		mvc.perform(get("/api/product-types/{code}", creamCode)).andExpect(status().isNotFound());
	}

	@Test
	void productsCanBeFilteredAndSorted() throws Exception {
		mvc.perform(get("/api/products")).andExpect(jsonPath("$.items[*].itemId", contains(1, 2)));
		mvc.perform(get("/api/products").param("sort", "PRICE_DESC"))
				.andExpect(jsonPath("$.items[*].itemId", contains(2, 1)));
		mvc.perform(get("/api/products").param("shopType", "PREFERRED"))
				.andExpect(jsonPath("$.items[*].itemId", contains(2)));
		mvc.perform(get("/api/products").param("q", "15ML"))
				.andExpect(jsonPath("$.items[*].itemId", contains(1)));
		mvc.perform(get("/api/products").param("typeCode", serumCode()).param("size", "1").param("page", "1"))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.total").value(2));
	}

	@Test
	void rejectsUnknownSortAndHugePage() throws Exception {
		mvc.perform(get("/api/products").param("sort", "DROP TABLE")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/products").param("size", "1000")).andExpect(status().isBadRequest());
	}

	@Test
	void productDetailHasPriceHistory() throws Exception {
		upload(csv()
				.item(1, "promo Dr.PONG Serum 15ml", "90", "Shop A", "Mall", "E001", "Dr.PONG Serum")
				.item(2, "Dr.PONG Serum 16ml", "120", "Shop B", "ร้านแนะนำ", "E001", "Dr.PONG Serum"));

		mvc.perform(get("/api/products/{itemId}", 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.product.price").value(90))
				.andExpect(jsonPath("$.priceHistory[*].price", contains(100.0)));
	}

	@Test
	void linkFallsBackToProductUrlWhenNoAffiliateLink() throws Exception {
		upload(baseCatalog().line(
				"6,Dr.PONG Serum 20ml,130,0,Shop C,ทั่วไป,5%,฿1,https://shopee.co.th/product/77/6,,,E001,Dr.PONG Serum,ชื่ออังกฤษตรงกัน"));

		mvc.perform(get("/api/products/{itemId}", 6))
				.andExpect(jsonPath("$.product.link").value("https://shopee.co.th/product/77/6"));
	}

	@Test
	void hiddenProductIsNotFound() throws Exception {
		mvc.perform(get("/api/products/{itemId}", 3)).andExpect(status().isNotFound());
		mvc.perform(get("/api/products/{itemId}", 999)).andExpect(status().isNotFound());
	}

	private String serumCode() {
		return jdbc.sql("SELECT product_type_code FROM product_type WHERE type_name = 'Dr.PONG Serum'")
				.query(String.class).single();
	}

	private void upload(TestCsv csv) throws Exception {
		mvc.perform(multipart("/api/admin/imports").file(csv.file()).with(httpBasic("test-admin", "test-admin-password")))
				.andExpect(status().isOk());
	}

}
