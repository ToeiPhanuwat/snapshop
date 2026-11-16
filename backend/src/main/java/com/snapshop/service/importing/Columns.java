package com.snapshop.service.importing;

import java.util.List;

/** Column headers of the F1 import file. Other columns (such as the old shop type and image link) are ignored. */
final class Columns {

	static final String ITEM_ID = "รหัสสินค้า";
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
	static final String GROUPING_METHOD = "วิธีจัดกลุ่ม";

	static final List<String> ALL = List.of(ITEM_ID, NAME, PRICE, SOLD, SHOP_NAME, COMMISSION_RATE,
			COMMISSION, PRODUCT_URL, AFFILIATE_URL, GROUP_LABEL, GROUP_NAME, GROUPING_METHOD);

	private Columns() {
	}

}
