package com.limiter.rate_limiter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(TooManyRequestsException.class)
	public ResponseEntity<String> handleTooManyRequests(TooManyRequestsException exc) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(exc.getMessage());
	}

}
