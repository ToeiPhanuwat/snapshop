package com.snapshop.exception;

/** The request itself is not acceptable (bad input that validation annotations cannot express). */
public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException(String message) {
		super(message);
	}

}
