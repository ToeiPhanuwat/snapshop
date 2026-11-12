package com.snapshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

/** Schema rules: the database itself refuses illegal product states. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class SnapshopApplicationTests {

	@Autowired
	JdbcClient jdbc;

	long typeId;
	long batchId;

	@BeforeEach
	void seed() {
		jdbc.sql("TRUNCATE marketplace_product, product_type, import_batch RESTART IDENTITY CASCADE").update();
		typeId = jdbc.sql("""
				INSERT INTO product_type (category_code, type_name, type_name_norm, grouping_method)
				VALUES ('DRPONG', 'Serum', 'serum', 'NO_MATCH') RETURNING product_type_id""").query(Long.class).single();
		batchId = jdbc.sql("""
				INSERT INTO import_batch (category_code, source_files, row_count, inserted_count, price_changed_count,
				  unchanged_count, not_in_latest_count, new_type_count)
				VALUES ('DRPONG', 'a.csv', 0, 0, 0, 0, 0, 0) RETURNING batch_id""").query(Long.class).single();
	}

	@Test
	void productTypeCodeIsGeneratedBySystem() {
		String code = jdbc.sql("SELECT product_type_code FROM product_type WHERE product_type_id = ?")
				.param(typeId).query(String.class).single();
		assertThat(code).matches("PT\\d{6}");
	}

	@Test
	void onlyOneActiveRowPerItem() {
		insertProduct(111L, "Dr.X", "", "Dr.X", 'Y');
		assertThatThrownBy(() -> insertProduct(111L, "Dr.X", "", "Dr.X", 'Y'))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void namePartsMustRebuildRawName() {
		assertThatThrownBy(() -> insertProduct(222L, "Promo Dr.X", "Promo", "Dr.X", 'Y'))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void insertProduct(long itemId, String raw, String prefix, String core, char flag) {
		jdbc.sql("""
				INSERT INTO marketplace_product (item_id, shop_id, shop_name, shop_type, name_prefix, name_core, name_raw,
				  product_type_id, price, product_url, status, active_flag, import_batch_id, last_seen_batch_id, last_seen_at)
				VALUES (?, 1, 'shop', 'MALL', ?, ?, ?, ?, 100, 'https://shopee.co.th/product/1/1', 'OK', ?, ?, ?, now())""")
				.params(itemId, prefix, core, raw, typeId, String.valueOf(flag), batchId, batchId).update();
	}

}
