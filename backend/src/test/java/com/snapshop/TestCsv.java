package com.snapshop;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.mock.web.MockMultipartFile;

/** Builds small import files for tests. */
public final class TestCsv {

	public static final String HEADER = "รหัสสินค้า,ชื่อสินค้า,ราคา,ขาย,ชื่อร้านค้า,ประเภทร้านค้า,อัตราค่าคอมมิชชัน,คอมมิชชัน,"
			+ "ลิงก์สินค้า,ลิงก์ข้อเสนอ,ลิงก์รูปภาพ,รหัสกลุ่ม,ชื่อกลุ่ม,วิธีจัดกลุ่ม\n";

	private final StringBuilder body = new StringBuilder(HEADER);

	public static TestCsv csv() {
		return new TestCsv();
	}

	public TestCsv item(long itemId, String price, String label, String groupName) {
		return item(itemId, "Dr.PONG " + groupName, price, "Shop " + itemId, "Mall", label, groupName);
	}

	public TestCsv item(long itemId, String name, String price, String shop, String shopType, String label,
			String groupName) {
		body.append(itemId).append(",\"").append(name).append("\",").append(price).append(",10,\"").append(shop)
				.append("\",").append(shopType).append(",5%,฿1.00,https://shopee.co.th/product/77/").append(itemId)
				.append(",https://s.shopee.co.th/a").append(itemId).append(",,").append(label).append(",\"")
				.append(groupName).append("\",ชื่ออังกฤษตรงกัน\n");
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
		try (InputStream in = TestCsv.class.getResourceAsStream("/fixtures/drpong_merged_20261110.csv")) {
			return new MockMultipartFile("file", "drpong_merged_20261110.csv", "text/csv", in.readAllBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
