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
				.andExpect(jsonPath("$.rowCount").value(431))
				.andExpect(jsonPath("$.insertedCount").value(431))
				.andExpect(jsonPath("$.newTypeCount").value(205))
				.andExpect(jsonPath("$.priceChangedCount").value(0));

		assertThat(statusCounts()).containsExactlyInAnyOrderEntriesOf(
				Map.of("OK", 313L, "REVIEW", 89L, "PRICE_PENDING", 29L));
		assertThat(count("SELECT count(*) FROM marketplace_product WHERE name_prefix <> ''")).isEqualTo(281);
		assertThat(count("SELECT count(*) FROM marketplace_product WHERE rule_type_id IS NOT NULL")).isEqualTo(96);
		assertThat(count("SELECT count(*) FROM product_type WHERE kind = 'RULE'")).isEqualTo(16);
	}

	@Test
	void storesBothGroupsAndHoldsRuleItemsBackUntilReviewed() throws Exception {
		upload(csv().ruleItem(1, "100", "E001", "Aqua Gel 10 g", "R003", "Calmmea B5 Aqua Gel")
				.ruleItem(2, "120", "E002", "Aqua Gel x6", "R003", "Calmmea B5 Aqua Gel")
				.ruleItem(3, "300", "E003", "CoQ10 30 mg", "R004", "CoQ10")
				.ruleItem(4, "320", "E003", "CoQ10 30 mg", "R004", "CoQ10")
				.item(5, "50", "E004", "Cream").file())
				.andExpect(status().isOk());

		assertThat(jdbc.sql("""
				SELECT mp.item_id || ':' || e.kind || ':' || coalesce(r.type_name, '-') || ':' || mp.status
				FROM marketplace_product mp
				JOIN product_type e ON e.product_type_id = mp.product_type_id
				LEFT JOIN product_type r ON r.product_type_id = mp.rule_type_id
				ORDER BY mp.item_id""").query(String.class).list())
				.containsExactly("1:NAME:Calmmea B5 Aqua Gel:REVIEW", "2:NAME:Calmmea B5 Aqua Gel:REVIEW",
						"3:NAME:CoQ10:OK", "4:NAME:CoQ10:OK", "5:NAME:-:OK");
	}

	@Test
	void storedItemKeepsItsRuleGroupWhenTheNextFileSaysOtherwise() throws Exception {
		upload(csv().ruleItem(1, "100", "E001", "Serum", "R001", "Sachet").item(2, "120", "E002", "Toner").file());

		upload(csv().ruleItem(1, "90", "E001", "Serum", "R007", "Other").ruleItem(2, "120", "E002", "Toner", "R007",
				"Other").file())
				.andExpect(status().isOk());

		assertThat(jdbc.sql("""
				SELECT mp.item_id || ':' || coalesce(r.type_name, '-') || ':' || mp.status
				FROM marketplace_product mp LEFT JOIN product_type r ON r.product_type_id = mp.rule_type_id
				WHERE mp.active_flag = 'Y' ORDER BY mp.item_id""").query(String.class).list())
				.containsExactly("1:Sachet:OK", "2:-:OK");
	}

	@Test
	void importingSameFileAgainChangesNothing() throws Exception {
		upload(TestCsv.fixture()).andExpect(status().isOk());

		upload(TestCsv.fixture())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.insertedCount").value(0))
				.andExpect(jsonPath("$.priceChangedCount").value(0))
				.andExpect(jsonPath("$.unchangedCount").value(431))
				.andExpect(jsonPath("$.notInLatestCount").value(0))
				.andExpect(jsonPath("$.newTypeCount").value(0));

		assertThat(count("SELECT count(*) FROM marketplace_product")).isEqualTo(431);
		assertThat(count("SELECT count(*) FROM product_type")).isEqualTo(221);
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
