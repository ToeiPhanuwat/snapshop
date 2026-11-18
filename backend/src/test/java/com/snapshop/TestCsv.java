package com.snapshop;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.mock.web.MockMultipartFile;

/** Builds small import files for tests. */
public final class TestCsv {

	public static final String HEADER = "รหัสสินค้า,namePrefix,ชื่อสินค้า,ราคา,ขาย,ชื่อร้านค้า,อัตราค่าคอมมิชชัน,คอมมิชชัน,"
			+ "ลิงก์สินค้า,ลิงก์ข้อเสนอ,รหัสกลุ่ม,ชื่อกลุ่ม,รหัสกลุ่มตามกฎ,ชื่อกลุ่มตามกฎ\n";

	private final StringBuilder body = new StringBuilder(HEADER);

	public static TestCsv csv() {
		return new TestCsv();
	}

	public TestCsv item(long itemId, String price, String label, String groupName) {
		return item(itemId, "Dr.PONG " + groupName, price, "Shop " + itemId, label, groupName);
	}

	/** {@code name} is the full name; the text before the first "Dr." goes to the namePrefix column. */
	public TestCsv item(long itemId, String name, String price, String shop, String label, String groupName) {
		return item(itemId, name, price, shop, label, groupName, "", "");
	}

	/** An item that also belongs to a rule group (กลุ่ม R). */
	public TestCsv ruleItem(long itemId, String price, String label, String groupName, String ruleLabel,
			String ruleName) {
		return item(itemId, "Dr.PONG " + groupName, price, "Shop " + itemId, label, groupName, ruleLabel, ruleName);
	}

	TestCsv item(long itemId, String name, String price, String shop, String label, String groupName,
			String ruleLabel, String ruleName) {
		int dr = name.indexOf("Dr.");
		String prefix = dr > 0 ? name.substring(0, dr).strip() : "";
		String core = dr > 0 ? name.substring(dr) : name;
		body.append(itemId).append(",\"").append(prefix).append("\",\"").append(core).append("\",").append(price)
				.append(",10,\"").append(shop).append("\",5%,฿1.00,https://shopee.co.th/product/77/").append(itemId)
				.append(",https://s.shopee.co.th/a").append(itemId).append(',').append(label).append(",\"")
				.append(groupName).append("\",").append(ruleLabel).append(",\"").append(ruleName).append("\"\n");
		return this;
	}

	/** One full CSV line, for cases the shortcuts above do not cover. */
	public TestCsv line(String csvLine) {
		body.append(csvLine).append('\n');
		return this;
	}

	public MockMultipartFile file() {
		return new MockMultipartFile("file", "test.csv", "text/csv", body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public static MockMultipartFile fixture() {
		try (InputStream in = TestCsv.class.getResourceAsStream("/fixtures/drpong_new_grouped_20261117.csv")) {
			return new MockMultipartFile("file", "drpong_new_grouped_20261117.csv", "text/csv", in.readAllBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
