package com.snapshop.service.importing;

import java.util.List;

/** Column headers of the import file. The name arrives split: the text before "Dr." and the rest. */
final class Columns {

	static final String ITEM_ID = "รหัสสินค้า";
	static final String NAME_PREFIX = "namePrefix";
	static final String NAME = "ชื่อสินค้า";
	static final String PRICE = "ราคา";
	static final String SOLD = "ขาย";
	static final String SHOP_NAME = "ชื่อร้านค้า";
	static final String COMMISSION_RATE = "อัตราค่าคอมมิชชัน";
	static final String COMMISSION = "คอมมิชชัน";
	static final String PRODUCT_URL = "ลิงก์สินค้า";
	static final String AFFILIATE_URL = "ลิงก์ข้อเสนอ";
	static final String GROUP_LABEL = "รหัสกลุ่ม";
	static final String GROUP_NAME = "ชื่อกลุ่ม";
	static final String RULE_GROUP_LABEL = "รหัสกลุ่มตามกฎ";
	static final String RULE_GROUP_NAME = "ชื่อกลุ่มตามกฎ";

	static final List<String> ALL = List.of(ITEM_ID, NAME_PREFIX, NAME, PRICE, SOLD, SHOP_NAME, COMMISSION_RATE,
			COMMISSION, PRODUCT_URL, AFFILIATE_URL, GROUP_LABEL, GROUP_NAME, RULE_GROUP_LABEL, RULE_GROUP_NAME);

	private Columns() {
	}

}
