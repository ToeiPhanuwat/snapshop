package com.snapshop.model.enums;

/** Status of the current version of an item. Only {@code OK} items can appear on the user site. */
public enum ProductStatus {
	OK,
	/** Was not in the latest import file; hidden until it comes back or an admin decides. */
	NOT_IN_LATEST,
	TEST,
	OUT_OF_STOCK,
	/** Waiting for an admin decision; stays until resolved. */
	REVIEW
}
