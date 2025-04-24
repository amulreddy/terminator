package com.autowares.mongoose.exception;

public class ContinueProcessingException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public ContinueProcessingException() {
		super();
	}

	public ContinueProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContinueProcessingException(String message) {
		super(message);
	}

	public ContinueProcessingException(Throwable cause) {
		super(cause);
	}

}
