package com.snapshop.service.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.snapshop.exception.ImportValidationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvImportParserTest {

	static final String HEADER = "รหัสสินค้า,namePrefix,ชื่อสินค้า,ราคา,ขาย,ชื่อร้านค้า,อัตราค่าคอมมิชชัน,คอมมิชชัน,"
			+ "ลิงก์สินค้า,ลิงก์ข้อเสนอ,รหัสกลุ่ม,ชื่อกลุ่ม,รหัสกลุ่มตามกฎ,ชื่อกลุ่มตามกฎ\n";

	final CsvImportParser parser = new CsvImportParser();

	@Test
	void parsesRealGroupedFile() throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/fixtures/drpong_new_grouped_20261117.csv")) {
			List<ImportRow> rows = parser.parse(in);

			assertThat(rows).hasSize(431);
			assertThat(rows.stream().filter(r -> !r.name().prefix().isEmpty())).hasSize(281);
			assertThat(rows.stream().map(ImportRow::fileGroupLabel).distinct()).hasSize(205);
			assertThat(rows.stream().filter(r -> r.ruleGroup() != null)).hasSize(96);
			assertThat(rows.stream().filter(r -> r.ruleGroup() != null).map(r -> r.ruleGroup().label()).distinct())
					.hasSize(16);
			assertThat(rows.stream().filter(r -> r.price() == null)).hasSize(33);
		}
	}

	@Test
	void readsEveryColumnIntoTypedValues() {
		ImportRow row = parseOne("28995304161,Promo,Dr. PONG SERUM,\"1,311\",200พัน+,Dr. PONG Store,5%,฿15.55,"
				+ "https://shopee.co.th/product/1882933019/28995304161,https://s.shopee.co.th/abc,"
				+ "E001,Dr. PONG SERUM,R001,Serum Collection");

		assertThat(row.lineNumber()).isEqualTo(2);
		assertThat(row.itemId()).isEqualTo(28995304161L);
		assertThat(row.shopId()).isEqualTo(1882933019L);
		assertThat(row.shopName()).isEqualTo("Dr. PONG Store");
		assertThat(row.name()).isEqualTo(new ProductName("Promo", "Dr. PONG SERUM"));
		assertThat(row.price()).isEqualByComparingTo("1311");
		assertThat(row.soldText()).isEqualTo("200พัน+");
		assertThat(row.commissionRate()).isEqualByComparingTo("5");
		assertThat(row.commissionAmount()).isEqualByComparingTo("15.55");
		assertThat(row.productUrl()).isEqualTo("https://shopee.co.th/product/1882933019/28995304161");
		assertThat(row.affiliateUrl()).isEqualTo("https://s.shopee.co.th/abc");
		assertThat(row.fileGroupLabel()).isEqualTo("E001");
		assertThat(row.groupName()).isEqualTo("Dr. PONG SERUM");
		assertThat(row.ruleGroup()).isEqualTo(new RuleGroup("R001", "Serum Collection"));
	}

	@Test
	void blankValuesBecomeUnknown() {
		ImportRow row = parseOne("1,,Dr.X,1.0พัน,,s,5%,฿1.0พัน,https://shopee.co.th/product/9/1,,E001,Dr.X,,");

		assertThat(row.name()).isEqualTo(new ProductName("", "Dr.X"));
		assertThat(row.price()).isNull();
		assertThat(row.commissionAmount()).isNull();
		assertThat(row.affiliateUrl()).isNull();
		assertThat(row.ruleGroup()).isNull();
	}

	@Test
	void rejectsFileWithAllErrorsListedByLine() {
		String csv = "1,,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/1,,E001,Dr.X,R001,\n"
				+ "abc,,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/2,,E002,Dr.X,,\n"
				+ "3,,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/999,,E003,Dr.X,,\n"
				+ "1,,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/1,,E001,Dr.X,,\n"
				+ "5,,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/5,,,,,Rule\n"
				+ "6,promo,,10,,s,5%,฿1,https://shopee.co.th/product/9/6,,E006,Dr.X,,\n";

		assertThatThrownBy(() -> parse(csv))
				.isInstanceOfSatisfying(ImportValidationException.class, e -> assertThat(e.errors()).containsExactly(
						"แถว 2: รหัสกลุ่มตามกฎกับชื่อกลุ่มตามกฎต้องมีทั้งคู่หรือว่างทั้งคู่",
						"แถว 3: รหัสสินค้าไม่ใช่ตัวเลข 'abc'",
						"แถว 4: ลิงก์สินค้าไม่ตรงกับรหัสสินค้า 'https://shopee.co.th/product/9/999'",
						"แถว 5: รหัสสินค้าซ้ำกับแถว 2",
						"แถว 6: รหัสกลุ่มว่าง",
						"แถว 6: ชื่อกลุ่มว่าง",
						"แถว 6: รหัสกลุ่มตามกฎกับชื่อกลุ่มตามกฎต้องมีทั้งคู่หรือว่างทั้งคู่",
						"แถว 7: ชื่อสินค้าว่าง"));
	}

	@Test
	void rejectsOldFormatWithoutTheNewColumns() {
		String old = "รหัสสินค้า,ชื่อสินค้า,ราคา,ขาย,ชื่อร้านค้า,อัตราค่าคอมมิชชัน,คอมมิชชัน,"
				+ "ลิงก์สินค้า,ลิงก์ข้อเสนอ,รหัสกลุ่ม,ชื่อกลุ่ม,วิธีจัดกลุ่ม\n"
				+ "1,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/1,,U001,Dr.X,ไม่มีคู่\n";

		assertThatThrownBy(() -> parser.parse(stream(old)))
				.isInstanceOfSatisfying(ImportValidationException.class, e -> assertThat(e.errors())
						.containsExactly("ไม่พบคอลัมน์: namePrefix, รหัสกลุ่มตามกฎ, ชื่อกลุ่มตามกฎ"));
	}

	@Test
	void rejectsFileMissingColumns() {
		assertThatThrownBy(() -> parser.parse(stream("รหัสสินค้า,ชื่อสินค้า\n1,Dr.X\n")))
				.isInstanceOfSatisfying(ImportValidationException.class,
						e -> assertThat(e.errors()).first().asString().startsWith("ไม่พบคอลัมน์: namePrefix, ราคา"));
	}

	@Test
	void rejectsEmptyFile() {
		assertThatThrownBy(() -> parse(""))
				.isInstanceOfSatisfying(ImportValidationException.class,
						e -> assertThat(e.errors()).containsExactly("ไฟล์ไม่มีข้อมูลสินค้า"));
	}

	private ImportRow parseOne(String line) {
		return parse(line + "\n").getFirst();
	}

	private List<ImportRow> parse(String body) {
		return parser.parse(stream(HEADER + body));
	}

	private static InputStream stream(String s) {
		return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	}

}
