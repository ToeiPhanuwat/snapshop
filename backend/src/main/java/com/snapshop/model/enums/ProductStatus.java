package com.snapshop.model.enums;

/** Status of the current version of an item. Only {@code OK} items can appear on the user site. */
public enum ProductStatus {
	OK,
	/** Was not in the latest import file; hidden until it comes back or an admin decides. */
	NOT_IN_LATEST,
	TEST,
	OUT_OF_STOCK,
	/** Waiting for an admin decision; stays until resolved. */
	REVIEW,
	/** The file showed an abbreviated price; the price comes from the feed. Hidden until it arrives. */
	PRICE_PENDING
}
