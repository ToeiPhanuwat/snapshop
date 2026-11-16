package com.snapshop.controller;

import static com.snapshop.TestCsv.csv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snapshop.IntegrationTest;
import com.snapshop.TestCsv;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class AdminImportControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	@BeforeEach
	void emptyDatabase() {
		jdbc.sql("TRUNCATE marketplace_product, product_type, import_batch RESTART IDENTITY CASCADE").update();
	}

	@Test
	void importsRealFile() throws Exception {
		upload(TestCsv.fixture())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rowCount").value(192))
				.andExpect(jsonPath("$.insertedCount").value(192))
				.andExpect(jsonPath("$.newTypeCount").value(122))
				.andExpect(jsonPath("$.priceChangedCount").value(0));

		assertThat(statusCounts()).containsExactlyInAnyOrderEntriesOf(
				Map.of("OK", 187L, "PRICE_PENDING", 3L, "TEST", 1L, "OUT_OF_STOCK", 1L));
		assertThat(count("SELECT count(*) FROM marketplace_product WHERE name_prefix <> ''")).isEqualTo(95);
	}

	@Test
	void importingSameFileAgainChangesNothing() throws Exception {
		upload(TestCsv.fixture()).andExpect(status().isOk());

		upload(TestCsv.fixture())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.insertedCount").value(0))
				.andExpect(jsonPath("$.priceChangedCount").value(0))
				.andExpect(jsonPath("$.unchangedCount").value(192))
				.andExpect(jsonPath("$.notInLatestCount").value(0))
				.andExpect(jsonPath("$.newTypeCount").value(0));

		assertThat(count("SELECT count(*) FROM marketplace_product")).isEqualTo(192);
		assertThat(count("SELECT count(*) FROM product_type")).isEqualTo(122);
	}

	@Test
	void nextRoundRecordsPriceChangeHidesMissingItemAndAddsNewItemToExistingGroup() throws Exception {
		upload(csv().item(1, "100", "E001", "Serum").item(2, "120", "E001", "Serum").item(3, "50", "U001", "Cream")
				.file());

		upload(csv().item(1, "90", "E005", "Serum").item(2, "120", "E005", "Serum").item(4, "95", "E005", "Serum")
				.file())
				.andExpect(jsonPath("$.priceChangedCount").value(1))
				.andExpect(jsonPath("$.insertedCount").value(1))
				.andExpect(jsonPath("$.unchangedCount").value(1))
				.andExpect(jsonPath("$.notInLatestCount").value(1))
				.andExpect(jsonPath("$.newTypeCount").value(0));

		assertThat(jdbc.sql("""
				SELECT price || ':' || active_flag || ':' || coalesce(deactivation_reason, '-')
				FROM marketplace_product WHERE item_id = 1 ORDER BY product_id""").query(String.class).list())
				.containsExactly("100.00:N:PRICE_CHANGED", "90.00:Y:-");
		assertThat(jdbc.sql("SELECT status FROM marketplace_product WHERE item_id = 3").query(String.class).single())
				.isEqualTo("NOT_IN_LATEST");
		assertThat(count("SELECT count(DISTINCT product_type_id) FROM marketplace_product WHERE item_id IN (1, 2, 4)"))
				.isEqualTo(1);
	}

	@Test
	void rejectsInvalidFileWithReasonsAndStoresNothing() throws Exception {
		upload(csv().item(1, "100", "", "Serum").file())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors", hasItem("แถว 2: รหัสกลุ่มว่าง")));

		assertThat(count("SELECT count(*) FROM import_batch")).isZero();
	}

	@Test
	void requiresAdminLogin() throws Exception {
		mvc.perform(multipart("/api/admin/imports").file(TestCsv.fixture())).andExpect(status().isUnauthorized());
		mvc.perform(multipart("/api/admin/imports").file(TestCsv.fixture()).with(httpBasic("test-admin", "wrong")))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions upload(MockMultipartFile file) throws Exception {
		return mvc.perform(multipart("/api/admin/imports").file(file).with(httpBasic("test-admin", "test-admin-password")));
	}

	private Map<String, Long> statusCounts() {
		return jdbc.sql("SELECT status, count(*) AS n FROM marketplace_product GROUP BY status")
				.query((rs, i) -> Map.entry(rs.getString("status"), rs.getLong("n")))
				.list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private long count(String sql) {
		return jdbc.sql(sql).query(Long.class).single();
	}

}
