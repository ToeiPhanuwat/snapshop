package com.snapshop.exception;

/** The request is well formed but the current data does not allow it. */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}
