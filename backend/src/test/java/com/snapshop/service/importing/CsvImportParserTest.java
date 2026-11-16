package com.snapshop.service.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.snapshop.exception.ImportValidationException;
import com.snapshop.model.enums.GroupingMethod;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CsvImportParserTest {

	static final String HEADER = "รหัสสินค้า,ชื่อสินค้า,ราคา,ขาย,ชื่อร้านค้า,อัตราค่าคอมมิชชัน,คอมมิชชัน,"
			+ "ลิงก์สินค้า,ลิงก์ข้อเสนอ,รหัสกลุ่ม,ชื่อกลุ่ม,วิธีจัดกลุ่ม\n";

	final CsvImportParser parser = new CsvImportParser();

	@Test
	void parsesRealMergedFile() throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/fixtures/drpong_merged_20261110.csv")) {
			List<ImportRow> rows = parser.parse(in);

			assertThat(rows).hasSize(192);
			assertThat(rows.stream().filter(r -> !r.name().prefix().isEmpty())).hasSize(95);
			assertThat(rows.stream().filter(r -> !r.name().core().regionMatches(true, 0, "Dr.", 0, 3))).hasSize(1);
			assertThat(rows.stream().map(ImportRow::fileGroupLabel).distinct()).hasSize(122);
			assertThat(rows.stream().filter(r -> r.price() == null)).hasSize(3);
		}
	}

	@Test
	void readsEveryColumnIntoTypedValues() {
		ImportRow row = parseOne("28995304161,Promo Dr. PONG SERUM,\"1,311\",200พัน+,Dr. PONG Store,5%,฿15.55,"
				+ "https://shopee.co.th/product/1882933019/28995304161,https://s.shopee.co.th/abc,"
				+ "E001,Dr. PONG SERUM,ชื่ออังกฤษตรงกัน");

		assertThat(row.lineNumber()).isEqualTo(2);
		assertThat(row.itemId()).isEqualTo(28995304161L);
		assertThat(row.shopId()).isEqualTo(1882933019L);
		assertThat(row.shopName()).isEqualTo("Dr. PONG Store");
		assertThat(row.name()).isEqualTo(new ProductName("Promo ", "Dr. PONG SERUM"));
		assertThat(row.price()).isEqualByComparingTo("1311");
		assertThat(row.soldText()).isEqualTo("200พัน+");
		assertThat(row.commissionRate()).isEqualByComparingTo("5");
		assertThat(row.commissionAmount()).isEqualByComparingTo("15.55");
		assertThat(row.productUrl()).isEqualTo("https://shopee.co.th/product/1882933019/28995304161");
		assertThat(row.affiliateUrl()).isEqualTo("https://s.shopee.co.th/abc");
		assertThat(row.fileGroupLabel()).isEqualTo("E001");
		assertThat(row.groupName()).isEqualTo("Dr. PONG SERUM");
		assertThat(row.groupingMethod()).isEqualTo(GroupingMethod.ENGLISH_NAME_MATCH);
	}

	@Test
	void abbreviatedPriceBecomesUnknownAndBlankLinksBecomeNull() {
		ImportRow row = parseOne("1,Dr.X,1.0พัน,,s,5%,฿1.0พัน,https://shopee.co.th/product/9/1,,U001,Dr.X,ไม่มีคู่");

		assertThat(row.price()).isNull();
		assertThat(row.commissionAmount()).isNull();
		assertThat(row.affiliateUrl()).isNull();
		assertThat(row.groupingMethod()).isEqualTo(GroupingMethod.NO_MATCH);
	}

	@Test
	void acceptsThaiLabelsWrittenWithEitherSaraAmEncoding() {
		// "ตามกฎคำระบุสูตร" with U+0E33 and with U+0E4D U+0E32
		Map<Long, ImportRow> rows = byItem(parse(line(1, "ตามกฎคำระบุสูตร") + line(2, "ตามกฎคําระบุสูตร")));

		assertThat(rows.values()).allSatisfy(r -> assertThat(r.groupingMethod()).isEqualTo(GroupingMethod.FORMULA_RULE));
	}

	@Test
	void ignoresLegacyShopTypeAndImageColumns() {
		String header = "รหัสสินค้า,ชื่อสินค้า,ราคา,ขาย,ชื่อร้านค้า,ประเภทร้านค้า,อัตราค่าคอมมิชชัน,คอมมิชชัน,"
				+ "ลิงก์สินค้า,ลิงก์ข้อเสนอ,ลิงก์รูปภาพ,รหัสกลุ่ม,ชื่อกลุ่ม,วิธีจัดกลุ่ม\n";

		List<ImportRow> rows = parser.parse(stream(header
				+ "1,Dr.X,10,,s,ไม่รู้จัก,5%,฿1,https://shopee.co.th/product/9/1,,https://img/x.webp,U001,Dr.X,ไม่มีคู่\n"));

		assertThat(rows).hasSize(1);
	}

	@Test
	void rejectsFileWithAllErrorsListedByLine() {
		String csv = "1,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/1,,U001,Dr.X,ไม่รู้จัก\n"
				+ "abc,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/2,,U002,Dr.X,ไม่มีคู่\n"
				+ "3,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/999,,U003,Dr.X,ไม่มีคู่\n"
				+ "1,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/1,,U001,Dr.X,ไม่มีคู่\n"
				+ "5,Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/5,,,,ไม่มีคู่\n";

		assertThatThrownBy(() -> parse(csv))
				.isInstanceOfSatisfying(ImportValidationException.class, e -> assertThat(e.errors()).containsExactly(
						"แถว 2: วิธีจัดกลุ่มไม่รู้จัก 'ไม่รู้จัก'",
						"แถว 3: รหัสสินค้าไม่ใช่ตัวเลข 'abc'",
						"แถว 4: ลิงก์สินค้าไม่ตรงกับรหัสสินค้า 'https://shopee.co.th/product/9/999'",
						"แถว 5: รหัสสินค้าซ้ำกับแถว 2",
						"แถว 6: รหัสกลุ่มว่าง",
						"แถว 6: ชื่อกลุ่มว่าง"));
	}

	@Test
	void rejectsFileMissingColumns() {
		assertThatThrownBy(() -> parser.parse(stream("รหัสสินค้า,ชื่อสินค้า\n1,Dr.X\n")))
				.isInstanceOfSatisfying(ImportValidationException.class,
						e -> assertThat(e.errors()).first().asString().startsWith("ไม่พบคอลัมน์: ราคา"));
	}

	@Test
	void rejectsEmptyFile() {
		assertThatThrownBy(() -> parse(""))
				.isInstanceOfSatisfying(ImportValidationException.class,
						e -> assertThat(e.errors()).containsExactly("ไฟล์ไม่มีข้อมูลสินค้า"));
	}

	private static String line(long itemId, String method) {
		return itemId + ",Dr.X,10,,s,5%,฿1,https://shopee.co.th/product/9/" + itemId + ",,R001,Dr.X," + method + "\n";
	}

	private ImportRow parseOne(String line) {
		return parse(line + "\n").getFirst();
	}

	private List<ImportRow> parse(String body) {
		return parser.parse(stream(HEADER + body));
	}

	private static Map<Long, ImportRow> byItem(List<ImportRow> rows) {
		return rows.stream().collect(Collectors.toMap(ImportRow::itemId, Function.identity()));
	}

	private static InputStream stream(String s) {
		return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
	}

}
