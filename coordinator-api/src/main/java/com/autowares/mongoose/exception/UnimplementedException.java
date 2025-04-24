package com.autowares.mongoose.exception;

public class UnimplementedException extends AbortProcessingException {
	
	public UnimplementedException() {
		super();
	}

	public UnimplementedException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnimplementedException(String message) {
		super(message);
	}

	public UnimplementedException(Throwable cause) {
		super(cause);
	}

}
