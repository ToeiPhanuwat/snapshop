package com.snapshop.model.enums;

import java.util.Optional;

/** How the file grouped a product with others before import. */
public enum GroupingMethod {

	ENGLISH_NAME_MATCH("ชื่ออังกฤษตรงกัน"),
	FORMULA_RULE("ตามกฎคำระบุสูตร"),
	NO_MATCH("ไม่มีคู่");

	private final String label;

	GroupingMethod(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static Optional<GroupingMethod> fromLabel(String text) {
		return Labels.find(GroupingMethod.class, GroupingMethod::label, text);
	}

}
