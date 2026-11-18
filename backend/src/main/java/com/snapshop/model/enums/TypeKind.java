package com.snapshop.model.enums;

/** Which rule formed a product type. */
public enum TypeKind {

	/** กลุ่ม E: same English name. Every item belongs to one. */
	NAME,

	/** กลุ่ม R: same formula word. An item may belong to one; name groups join it when the admin merges them. */
	RULE

}
