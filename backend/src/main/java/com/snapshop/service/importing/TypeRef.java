package com.snapshop.service.importing;

/** The product type a new version belongs to: one already stored, or one this import creates. */
public sealed interface TypeRef {

	record Existing(long productTypeId) implements TypeRef {
	}

	/** @param normName key of the {@link ImportPlan.NewType} to be created */
	record Created(String normName) implements TypeRef {
	}

}
