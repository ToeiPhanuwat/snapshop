package com.snapshop.model.enums;

/** Why a version stopped being current. */
public enum DeactivationReason {
	/** A newer version with another price replaced it; it is price history. */
	PRICE_CHANGED,
	/** An admin checked the item and it is no longer sold. */
	DISCONTINUED
}
