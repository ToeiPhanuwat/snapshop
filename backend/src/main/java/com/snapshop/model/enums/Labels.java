package com.snapshop.model.enums;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/** Matches Thai labels from files: sara am (U+0E33) and nikhahit + aa (U+0E4D U+0E32) count as the same text. */
final class Labels {

	private Labels() {
	}

	static String normalize(String text) {
		return Normalizer.normalize(text.strip(), Normalizer.Form.NFKC).toLowerCase();
	}

	static <E extends Enum<E>> Optional<E> find(Class<E> type, Function<E, String> label, String text) {
		String wanted = normalize(text);
		return Arrays.stream(type.getEnumConstants())
				.filter(e -> normalize(label.apply(e)).equals(wanted))
				.findFirst();
	}

}
