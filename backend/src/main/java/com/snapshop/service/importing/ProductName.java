package com.snapshop.service.importing;

/** Product name as the file gives it: the text before "Dr." and the rest. */
public record ProductName(String prefix, String core) {

	/** The whole name for reading and for word checks such as "สินค้าหมด". */
	public String raw() {
		return prefix.isEmpty() ? core : prefix + " " + core;
	}

}
