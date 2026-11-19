package com.snapshop.controller;

import static com.snapshop.TestCsv.csv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snapshop.IntegrationTest;
import com.snapshop.TestCsv;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Reviewing a rule group (กลุ่ม R) against the name groups (กลุ่ม E) its items sit in.
 *
 * <p>Catalog: R003 has items 11 (E011), 12 and 13 (E012), and 16 (E013, alone, no price); E011 also holds 15 and 33,
 * E012 also holds 14. Only 16 waits for review: the other name groups show on their own. R009 has 31 (E031, with 32)
 * and 33 (E011): nothing waits, but it can still be merged. R004 sits in one name group, E021, and is never listed.
 */
@IntegrationTest
class AdminRuleGroupApiTest {

	static final RequestPostProcessor ADMIN = httpBasic("test-admin", "test-admin-password");

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	@BeforeEach
	void importCatalog() throws Exception {
		jdbc.sql("TRUNCATE marketplace_product, product_type, import_batch RESTART IDENTITY CASCADE").update();
		upload(catalog());
	}

	@Test
	void listsRuleGroupsToReviewOrMergeWithTheNameGroupsTheyTouch() throws Exception {
		assertThat(statuses(11, 12, 13, 16, 31, 33)).isEqualTo("11:OK 12:OK 13:OK 16:REVIEW 31:OK 33:OK");
		mvc.perform(get("/api/admin/rule-groups").with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", contains("Calmmea B5 Aqua Gel", "Other")))
				.andExpect(jsonPath("$[0].code").value(code("Calmmea B5 Aqua Gel")))
				.andExpect(jsonPath("$[0].pendingCount").value(1))
				.andExpect(jsonPath("$[0].nameGroups[*].code",
						contains(code("Aqua Gel 10 g"), code("Aqua Gel x6"), code("Aqua Gel 50 g"))))
				.andExpect(jsonPath("$[0].nameGroups[0].mergedIntoCode").doesNotExist())
				.andExpect(jsonPath("$[0].nameGroups[0].items[*].itemId", contains(11, 15, 33)))
				.andExpect(jsonPath("$[0].nameGroups[0].items[*].inRule", contains(true, false, false)))
				.andExpect(jsonPath("$[0].nameGroups[2].items[0].price").doesNotExist())
				.andExpect(jsonPath("$[0].nameGroups[2].items[0].status").value("REVIEW"))
				.andExpect(jsonPath("$[1].pendingCount").value(0))
				.andExpect(jsonPath("$[1].nameGroups[*].code", contains(code("Aqua Gel 10 g"), code("Other cream"))));
	}

	@Test
	void confirmingMergesTickedNameGroupsUnderTheRuleNameAndPassesEveryRuleItem() throws Exception {
		confirm("Calmmea B5 Aqua Gel", "Aqua Gel 10 g", "Aqua Gel x6", "Aqua Gel 50 g")
				.andExpect(status().isNoContent());

		assertThat(statuses(11, 12, 16, 33)).isEqualTo("11:OK 12:OK 16:PRICE_PENDING 33:OK");
		mvc.perform(get("/api/product-types/{code}", code("Calmmea B5 Aqua Gel")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Calmmea B5 Aqua Gel"))
				.andExpect(jsonPath("$.offers[*].itemId", contains(33, 11, 15, 12, 13, 14)));
		mvc.perform(get("/api/admin/rule-groups").with(ADMIN))
				.andExpect(jsonPath("$[*].name", contains("Other")));
	}

	@Test
	void untickedNameGroupKeepsItsOwnPlaceAndItsRuleItemsStillPass() throws Exception {
		confirm("Calmmea B5 Aqua Gel", "Aqua Gel x6").andExpect(status().isNoContent());

		assertThat(statuses(11, 12, 16)).isEqualTo("11:OK 12:OK 16:PRICE_PENDING");
		mvc.perform(get("/api/product-types/{code}", code("Aqua Gel 10 g")))
				.andExpect(jsonPath("$.offers[*].itemId", contains(33, 11, 15)));
		mvc.perform(get("/api/product-types/{code}", code("Calmmea B5 Aqua Gel")))
				.andExpect(jsonPath("$.offers[*].itemId", contains(12, 13, 14)));
	}

	@Test
	void mergedNameGroupCodeLeadsToTheRuleGroupItWasMergedInto() throws Exception {
		confirm("Calmmea B5 Aqua Gel", "Aqua Gel x6").andExpect(status().isNoContent());

		mvc.perform(get("/api/product-types/{code}", code("Aqua Gel x6")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(code("Calmmea B5 Aqua Gel")))
				.andExpect(jsonPath("$.name").value("Calmmea B5 Aqua Gel"));
		mvc.perform(get("/api/product-types/{code}", code("Aqua Gel 10 g")))
				.andExpect(jsonPath("$.code").value(code("Aqua Gel 10 g")));
	}

	@Test
	void mergedNameGroupWhoseRuleGroupIsHiddenIsNotFound() throws Exception {
		confirm("Calmmea B5 Aqua Gel", "Aqua Gel 50 g").andExpect(status().isNoContent());

		mvc.perform(get("/api/product-types/{code}", code("Aqua Gel 50 g"))).andExpect(status().isNotFound());
	}

	@Test
	void ruleGroupWithNothingWaitingIsMergedTheSameWayAndLeavesTheList() throws Exception {
		confirm("Other", "Other cream").andExpect(status().isNoContent());

		mvc.perform(get("/api/product-types/{code}", code("Other")))
				.andExpect(jsonPath("$.offers[*].itemId", contains(31, 32)));
		mvc.perform(get("/api/admin/rule-groups").with(ADMIN))
				.andExpect(jsonPath("$[*].name", contains("Calmmea B5 Aqua Gel")));
	}

	@Test
	void newItemJoiningAConfirmedRuleGroupBringsItBackToTheList() throws Exception {
		confirm("Other", "Other cream").andExpect(status().isNoContent());

		upload(catalog().ruleItem(34, "80", "E031", "Other cream", "R009", "Other"));

		assertThat(statuses(34)).isEqualTo("34:OK");
		mvc.perform(get("/api/admin/rule-groups").with(ADMIN))
				.andExpect(jsonPath("$[*].name", contains("Calmmea B5 Aqua Gel", "Other")));
	}

	@Test
	void nameGroupMergedIntoOneRuleGroupCannotJoinAnother() throws Exception {
		confirm("Calmmea B5 Aqua Gel", "Aqua Gel 10 g").andExpect(status().isNoContent());

		mvc.perform(get("/api/admin/rule-groups").with(ADMIN))
				.andExpect(jsonPath("$[*].name", contains("Other")))
				.andExpect(jsonPath("$[0].nameGroups[*].mergedIntoCode",
						containsInAnyOrder(null, code("Calmmea B5 Aqua Gel"))));
		confirm("Other", "Aqua Gel 10 g").andExpect(status().isConflict());
	}

	@Test
	void confirmingTwiceEndsInTheSameState() throws Exception {
		confirm("Calmmea B5 Aqua Gel", "Aqua Gel x6").andExpect(status().isNoContent());
		confirm("Calmmea B5 Aqua Gel", "Aqua Gel x6").andExpect(status().isNoContent());

		assertThat(statuses(11, 12, 16)).isEqualTo("11:OK 12:OK 16:PRICE_PENDING");
		assertThat(count("merged_into_type_id IS NOT NULL")).isEqualTo(1);
		assertThat(count("merge_reviewed_at IS NOT NULL")).isEqualTo(1);
	}

	@Test
	void refusesNameGroupsOutsideTheRuleGroupAndUnknownCodes() throws Exception {
		confirm("Calmmea B5 Aqua Gel", "CoQ10 30 mg").andExpect(status().isConflict());
		confirm("Calmmea B5 Aqua Gel", "Calmmea B5 Aqua Gel").andExpect(status().isConflict());
		mvc.perform(post("/api/admin/rule-groups/{code}/confirm", "PT999999").with(ADMIN)
				.contentType(MediaType.APPLICATION_JSON).content("{\"mergeTypeCodes\":[]}"))
				.andExpect(status().isNotFound());
		mvc.perform(post("/api/admin/rule-groups/{code}/confirm", code("Aqua Gel 10 g")).with(ADMIN)
				.contentType(MediaType.APPLICATION_JSON).content("{\"mergeTypeCodes\":[]}"))
				.andExpect(status().isNotFound());
		assertThat(statuses(16)).isEqualTo("16:REVIEW");
		assertThat(count("merge_reviewed_at IS NOT NULL")).isZero();
	}

	@Test
	void itemCannotBeMovedIntoARuleGroupByHand() throws Exception {
		mvc.perform(post("/api/admin/products/{itemId}/move", 14).with(ADMIN).contentType(MediaType.APPLICATION_JSON)
				.content("{\"productTypeCode\":\"" + code("Calmmea B5 Aqua Gel") + "\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void reviewingOneRuleItemWithoutPriceLeavesItWaitingForTheFeed() throws Exception {
		mvc.perform(post("/api/admin/products/{itemId}/resolve", 16).with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PRICE_PENDING"));
	}

	@Test
	void adminGroupListShowsKindMembersAndMerge() throws Exception {
		confirm("Calmmea B5 Aqua Gel", "Aqua Gel x6").andExpect(status().isNoContent());

		mvc.perform(get("/api/admin/product-types").param("q", "aqua gel").with(ADMIN))
				.andExpect(jsonPath("$.items[*].name",
						contains("Aqua Gel 10 g", "Aqua Gel 50 g", "Aqua Gel x6", "Calmmea B5 Aqua Gel")))
				.andExpect(jsonPath("$.items[*].kind", contains("NAME", "NAME", "NAME", "RULE")))
				.andExpect(jsonPath("$.items[2].mergedIntoCode").value(code("Calmmea B5 Aqua Gel")))
				.andExpect(jsonPath("$.items[3].activeCount").value(4))
				.andExpect(jsonPath("$.items[3].onUserSite").value(true));
	}

	@Test
	void requiresAdminLogin() throws Exception {
		mvc.perform(get("/api/admin/rule-groups")).andExpect(status().isUnauthorized());
	}

	private static TestCsv catalog() {
		return csv()
				.ruleItem(11, "100", "E011", "Aqua Gel 10 g", "R003", "Calmmea B5 Aqua Gel")
				.ruleItem(12, "500", "E012", "Aqua Gel x6", "R003", "Calmmea B5 Aqua Gel")
				.ruleItem(13, "510", "E012", "Aqua Gel x6", "R003", "Calmmea B5 Aqua Gel")
				.item(14, "520", "E012", "Aqua Gel x6")
				.item(15, "110", "E011", "Aqua Gel 10 g")
				.ruleItem(16, "1.0พัน", "E013", "Aqua Gel 50 g", "R003", "Calmmea B5 Aqua Gel")
				.ruleItem(21, "300", "E021", "CoQ10 30 mg", "R004", "CoQ10")
				.ruleItem(22, "320", "E021", "CoQ10 30 mg", "R004", "CoQ10")
				.ruleItem(31, "70", "E031", "Other cream", "R009", "Other")
				.item(32, "75", "E031", "Other cream")
				.ruleItem(33, "90", "E011", "Aqua Gel 10 g", "R009", "Other");
	}

	private void upload(TestCsv csv) throws Exception {
		mvc.perform(multipart("/api/admin/imports").file(csv.file()).with(ADMIN)).andExpect(status().isOk());
	}

	private long count(String productTypeCondition) {
		return jdbc.sql("SELECT count(*) FROM product_type WHERE " + productTypeCondition).query(Long.class).single();
	}

	private ResultActions confirm(String ruleName, String... nameGroups) throws Exception {
		String codes = Arrays.stream(nameGroups).map(n -> "\"" + code(n) + "\"").collect(Collectors.joining(","));
		return mvc.perform(post("/api/admin/rule-groups/{code}/confirm", code(ruleName)).with(ADMIN)
				.contentType(MediaType.APPLICATION_JSON).content("{\"mergeTypeCodes\":[" + codes + "]}"));
	}

	private String code(String typeName) {
		return jdbc.sql("SELECT product_type_code FROM product_type WHERE type_name = ?").param(typeName)
				.query(String.class).single();
	}

	private String statuses(long... itemIds) {
		return Arrays.stream(itemIds).mapToObj(id -> id + ":" + jdbc.sql(
				"SELECT status FROM marketplace_product WHERE active_flag = 'Y' AND item_id = ?").param(id)
				.query(String.class).single()).collect(Collectors.joining(" "));
	}

}
