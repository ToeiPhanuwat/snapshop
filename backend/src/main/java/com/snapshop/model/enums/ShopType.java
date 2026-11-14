package com.snapshop.model.enums;

import java.util.Optional;

/** Badge on the Shopee product card. */
public enum ShopType {

	MALL("Mall"),
	PREFERRED("ร้านแนะนำ"),
	REGULAR("ทั่วไป");

	private final String label;

	ShopType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static Optional<ShopType> fromLabel(String text) {
		return Labels.find(ShopType.class, ShopType::label, text);
	}

}
