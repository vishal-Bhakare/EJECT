package com.edtech.coe.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// Our own rule violations: duplicate email, not found, bad coeId...
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
		log.warn("Business error: {}", ex.getMessage());
		return build(ex.getStatus(), ex.getMessage(), request, null);
	}

	// @Valid failures on request bodies
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

		return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
	}

	// Malformed JSON, or an invalid enum value such as "role": "BOSS"
	// Malformed JSON, an invalid enum value such as "role": "BOSS", or a bad date
	// Malformed JSON, an invalid enum value such as "role": "BOSS", or a bad date or time
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
	                                                      HttpServletRequest request) {
	    return build(HttpStatus.BAD_REQUEST,
	            "Malformed JSON or invalid value (check enum fields such as role, "
	                    + "dates, which must look like 2027-01-18, and times, which must look like 09:00)",
	            request, null);
	}

	// Bad query/path parameter such as ?role=BOSS or /users/abc
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", request, null);
	}

	// Database unique constraint hit (e.g. two simultaneous requests, same email)
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return build(HttpStatus.CONFLICT, "Resource already exists or violates a data constraint", request, null);
	}

	// Safety net for anything unexpected
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.", request, null);
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
			Map<String, String> fieldErrors) {
		ErrorResponse body = ErrorResponse.builder().timestamp(Instant.now()).status(status.value())
				.error(status.getReasonPhrase()).message(message).path(request.getRequestURI()).fieldErrors(fieldErrors)
				.build();
		return ResponseEntity.status(status).body(body);
	}

}