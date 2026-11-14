package com.snapshop.service.importing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Product name split at the first "Dr." (any case); {@code prefix + core} is always the original name. */
public record ProductName(String prefix, String core) {

	private static final Pattern DR = Pattern.compile("dr\\.", Pattern.CASE_INSENSITIVE);

	public static ProductName of(String raw) {
		Matcher m = DR.matcher(raw);
		if (!m.find()) {
			return new ProductName("", raw);
		}
		return new ProductName(raw.substring(0, m.start()), raw.substring(m.start()));
	}

	public String raw() {
		return prefix + core;
	}

}
