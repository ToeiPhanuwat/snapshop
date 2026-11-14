package com.snapshop.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds a WHERE clause from optional filters; every value goes in as a named parameter. */
final class SqlWhere {

	private final List<String> conditions = new ArrayList<>();
	private final Map<String, Object> params = new HashMap<>();

	SqlWhere always(String condition) {
		conditions.add(condition);
		return this;
	}

	/** Adds {@code condition} only when {@code value} is not null; the condition refers to {@code :name}. */
	SqlWhere ifPresent(Object value, String condition, String name) {
		if (value != null) {
			conditions.add(condition);
			params.put(name, value);
		}
		return this;
	}

	/** Case-insensitive "contains" on any of the columns; % and _ in the text are matched literally. */
	SqlWhere containsText(String text, String... columns) {
		if (text == null || text.isBlank()) {
			return this;
		}
		List<String> ors = new ArrayList<>();
		for (String column : columns) {
			ors.add(column + " ILIKE :q ESCAPE '\\'");
		}
		conditions.add("(" + String.join(" OR ", ors) + ")");
		params.put("q", "%" + text.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
		return this;
	}

	String sql() {
		return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
	}

	Map<String, Object> params() {
		return params;
	}

}
