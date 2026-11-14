package com.snapshop.service.importing;

import java.text.Normalizer;
import java.util.Locale;

/** Normalized product type name: the key that decides "same name" between import rounds. */
public final class TypeNames {

	private TypeNames() {
	}

	/** Lower case, punctuation and repeated spaces removed: "Dr. PONG BARRIER-X  Serum!" → "dr pong barrier x serum". */
	public static String normalize(String name) {
		return Normalizer.normalize(name, Normalizer.Form.NFKC)
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^\\p{L}\\p{M}\\p{N}]+", " ")
				.strip();
	}

}
