package com.autowares.mongoose.config;

import org.apache.camel.CamelExecutionException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CoordinatorResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	/*
	 * Unwraps camel exceptions and maps them to response entities so we 
	 * don't just respond back with a 500
	 */
	
	@ExceptionHandler(value = { CamelExecutionException.class })
	protected ResponseEntity<Object> handleCamel(RuntimeException ex, WebRequest request) throws Exception {
		Exception root = (Exception) ExceptionUtils.getRootCause(ex);
		return handleCoordinatorExceptions(root, request);

	}

	@ExceptionHandler(value ={ HttpClientErrorException.class })
	public final ResponseEntity<Object> handleCoordinatorExceptions(Exception ex, WebRequest request) throws Exception {

		if (ex instanceof HttpClientErrorException) {
			HttpClientErrorException hc = (HttpClientErrorException) ex;
			return handleExceptionInternal(hc, hc.getResponseBodyAsString(), hc.getResponseHeaders(), hc.getStatusCode(), request);
		}
		
		throw ex;

	}
}
