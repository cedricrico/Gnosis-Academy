package com.example.Gnosis.admin.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class AdminApiExceptionHandler {
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<Map<String, String>> handleValidation(Exception exception) {
		ObjectError error = null;
		if (exception instanceof MethodArgumentNotValidException manv) {
			error = manv.getBindingResult().getAllErrors().stream().findFirst().orElse(null);
		} else if (exception instanceof BindException be) {
			error = be.getBindingResult().getAllErrors().stream().findFirst().orElse(null);
		}

		String message = error != null ? error.getDefaultMessage() : "Validation failed.";
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException exception) {
		String message = exception.getConstraintViolations().stream()
				.findFirst()
				.map(violation -> violation.getMessage())
				.orElse("Validation failed.");
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException exception) {
		String detail = exception.getMostSpecificCause() != null
				? exception.getMostSpecificCause().getMessage()
				: exception.getMessage();
		String message = "Database constraint violation.";
		if (detail != null) {
			String lower = detail.toLowerCase();
			if (lower.contains("duplicate entry")) {
				message = "Duplicate entry violates a unique constraint.";
			} else if (lower.contains("data too long") && lower.contains("column")) {
				message = "One of the fields is too long for the database column.";
			} else if (lower.contains("cannot be null") || lower.contains("null") && lower.contains("column")) {
				message = "A required field is missing.";
			} else if (lower.contains("foreign key")) {
				message = "Invalid reference for a related record.";
			}
		}
		if (detail != null && !detail.isBlank()) {
			String trimmed = detail.replaceAll("\\s+", " ").trim();
			if (trimmed.length() > 200) {
				trimmed = trimmed.substring(0, 200) + "...";
			}
			message = message + " (" + trimmed + ")";
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("message", message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = "Unexpected server error.";
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("message", message));
	}
}
