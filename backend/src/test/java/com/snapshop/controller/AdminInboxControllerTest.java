package com.snapshop.controller;

import static com.snapshop.TestCsv.csv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snapshop.IntegrationTest;
import com.snapshop.TestCsv;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@IntegrationTest
class AdminInboxControllerTest {

	static final RequestPostProcessor ADMIN = httpBasic("test-admin", "test-admin-password");

	@TempDir
	static Path inbox;

	@DynamicPropertySource
	static void inboxDir(DynamicPropertyRegistry registry) {
		registry.add("snapshop.import.inbox-dir", () -> inbox.toString());
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcClient jdbc;

	@BeforeEach
	void emptyInboxAndDatabase() throws IOException {
		jdbc.sql("TRUNCATE marketplace_product, product_type, import_batch RESTART IDENTITY CASCADE").update();
		try (Stream<Path> files = Files.walk(inbox)) {
			files.sorted(Comparator.reverseOrder()).filter(p -> !p.equals(inbox)).forEach(p -> p.toFile().delete());
		}
	}

	@Test
	void listsOnlyCsvFilesWaitingInInbox() throws Exception {
		put("round-1.csv", csv().item(1, "100", "E001", "Serum"));
		Files.writeString(inbox.resolve("notes.txt"), "x");
		Files.createDirectories(inbox.resolve("imported"));

		mvc.perform(get("/api/admin/inbox").with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", contains("round-1.csv")))
				.andExpect(jsonPath("$[0].size").isNumber())
				.andExpect(jsonPath("$[0].modifiedAt").isString());
	}

	@Test
	void previewOfRealFileChangesNothingAndMatchesTheRealImport() throws Exception {
		Files.write(inbox.resolve("drpong.csv"), TestCsv.fixture().getBytes());

		mvc.perform(get("/api/admin/inbox/{name}/preview", "drpong.csv").with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fileName").value("drpong.csv"))
				.andExpect(jsonPath("$.rowCount").value(192))
				.andExpect(jsonPath("$.insertedCount").value(192))
				.andExpect(jsonPath("$.newTypeCount").value(122))
				.andExpect(jsonPath("$.newTypes.length()").value(122))
				.andExpect(jsonPath("$.toReview.length()").value(3));
		assertThat(count("SELECT count(*) FROM marketplace_product") + count("SELECT count(*) FROM import_batch"))
				.isZero();

		mvc.perform(post("/api/admin/inbox/{name}/import", "drpong.csv").with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.insertedCount").value(192))
				.andExpect(jsonPath("$.newTypeCount").value(122));
	}

	@Test
	void previewOfNextRoundListsPriceChangesHiddenItemsAndReviews() throws Exception {
		put("r1.csv", csv().item(1, "100", "E001", "Serum").item(2, "120", "E001", "Serum").item(3, "50", "U001", "Cream"));
		mvc.perform(post("/api/admin/inbox/{name}/import", "r1.csv").with(ADMIN)).andExpect(status().isOk());

		put("r2.csv", csv().item(1, "90", "E001", "Serum").item(2, "1.0พัน", "E001", "Serum").item(4, "70", "U009", "Toner"));

		mvc.perform(get("/api/admin/inbox/{name}/preview", "r2.csv").with(ADMIN))
				.andExpect(jsonPath("$.priceChangedCount").value(1))
				.andExpect(jsonPath("$.priceChanges[0].itemId").value(1))
				.andExpect(jsonPath("$.priceChanges[0].name").value("Dr.PONG Serum"))
				.andExpect(jsonPath("$.priceChanges[0].oldPrice").value(100))
				.andExpect(jsonPath("$.priceChanges[0].newPrice").value(90))
				.andExpect(jsonPath("$.toHide[*].itemId", contains(3)))
				.andExpect(jsonPath("$.toReview[*].itemId", contains(2)))
				.andExpect(jsonPath("$.newTypes", contains("Toner")));
	}

	@Test
	void importedFileLeavesInboxForImportedFolder() throws Exception {
		put("r1.csv", csv().item(1, "100", "E001", "Serum"));

		mvc.perform(post("/api/admin/inbox/{name}/import", "r1.csv").with(ADMIN)).andExpect(status().isOk());

		mvc.perform(get("/api/admin/inbox").with(ADMIN)).andExpect(jsonPath("$.length()").value(0));
		assertThat(filesIn("imported")).singleElement().asString().endsWith("-r1.csv");
	}

	@Test
	void discardedFileGoesToRejectedFolderWithoutImporting() throws Exception {
		put("bad.csv", csv().item(1, "100", "E001", "Serum"));

		mvc.perform(post("/api/admin/inbox/{name}/discard", "bad.csv").with(ADMIN)).andExpect(status().isNoContent());

		assertThat(filesIn("rejected")).singleElement().asString().endsWith("-bad.csv");
		assertThat(count("SELECT count(*) FROM import_batch")).isZero();
	}

	@Test
	void invalidFileShowsRowErrorsInPreviewAndStaysInInbox() throws Exception {
		put("broken.csv", csv().item(1, "100", "", "Serum"));

		mvc.perform(get("/api/admin/inbox/{name}/preview", "broken.csv").with(ADMIN))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors", hasItem("แถว 2: รหัสกลุ่มว่าง")));
		mvc.perform(get("/api/admin/inbox").with(ADMIN)).andExpect(jsonPath("$[*].name", contains("broken.csv")));
	}

	@Test
	void refusesNamesOutsideInboxAndMissingFiles() throws Exception {
		Files.writeString(inbox.resolve("notes.txt"), "x");

		mvc.perform(get("/api/admin/inbox/{name}/preview", "notes.txt").with(ADMIN)).andExpect(status().isBadRequest());
		mvc.perform(get("/api/admin/inbox/{name}/preview", "..evil.csv").with(ADMIN)).andExpect(status().isBadRequest());
		mvc.perform(get("/api/admin/inbox/..%2F..%2Fsecret.csv/preview").with(ADMIN)).andExpect(status().isBadRequest());
		mvc.perform(get("/api/admin/inbox/{name}/preview", "missing.csv").with(ADMIN)).andExpect(status().isNotFound());
	}

	@Test
	void inboxNeedsAdminLogin() throws Exception {
		mvc.perform(get("/api/admin/inbox")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/admin/inbox/{name}/import", "x.csv")).andExpect(status().isUnauthorized());
	}

	private static void put(String name, TestCsv csv) throws IOException {
		Files.write(inbox.resolve(name), csv.file().getBytes());
	}

	private static Stream<String> filesIn(String folder) throws IOException {
		return Files.list(inbox.resolve(folder)).map(p -> p.getFileName().toString()).toList().stream();
	}

	private long count(String sql) {
		return jdbc.sql(sql).query(Long.class).single();
	}

}
