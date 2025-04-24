package com.autowares.mongoose.exception;


public class RetryLaterException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public RetryLaterException() {
		super();
	}

	public RetryLaterException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetryLaterException(String message) {
		super(message);
	}

	public RetryLaterException(Throwable cause) {
		super(cause);
	}

}
