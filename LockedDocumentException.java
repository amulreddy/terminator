package com.autowares.mongoose.exception;


public class LockedDocumentException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public LockedDocumentException() {
		super();
	}

	public LockedDocumentException(String message, Throwable cause) {
		super(message, cause);
	}

	public LockedDocumentException(String message) {
		super(message);
	}

	public LockedDocumentException(Throwable cause) {
		super(cause);
	}

}
