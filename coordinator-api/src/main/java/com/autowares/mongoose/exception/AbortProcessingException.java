package com.autowares.mongoose.exception;

public class AbortProcessingException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public AbortProcessingException() {
		super();
	}

	public AbortProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public AbortProcessingException(String message) {
		super(message);
	}

	public AbortProcessingException(Throwable cause) {
		super(cause);
	}

}
