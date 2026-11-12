package com.snapshop.exception;

import java.util.List;

/** The import file was rejected; every problem found is listed so the admin can fix the file in one pass. */
public class ImportValidationException extends RuntimeException {

	private final List<String> errors;

	public ImportValidationException(List<String> errors) {
		super(String.join("; ", errors));
		this.errors = List.copyOf(errors);
	}

	public List<String> errors() {
		return errors;
	}

}
