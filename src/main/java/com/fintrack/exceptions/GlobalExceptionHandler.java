package com.fintrack.exceptions;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {

    logger.warn("Invalid credentials: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
      .body(Map.of("error", "Invalid credentials"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException e) {
    FieldError fieldError = e.getBindingResult().getFieldError();
    String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "Validation error";
    logger.warn("Validation error: {}", errorMessage);

    return ResponseEntity.badRequest()
      .body(Map.of("error", Objects.requireNonNull(errorMessage)));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException e) {
    logger.warn("Constraint Violation: {}", e.getMessage());

    return ResponseEntity.badRequest()
      .body(Map.of("error", "Invalid data"));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
    logger.warn("HTTP Message Not Readable: {}", e.getMessage());

    return ResponseEntity.badRequest()
      .body(Map.of("error", "Invalid JSON format"));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Map<String, String>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
    logger.warn("HTTP Media Type Not Supported: {}", e.getMessage());

    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
      .body(Map.of("error", "Unsupported media type"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
    logger.warn("Method Argument Type Mismatch: {}", e.getMessage());

    return ResponseEntity.badRequest()
      .body(Map.of("error", "Invalid parameter format"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
    logger.warn("Data Integrity Violation: {}", e.getMessage());

    String errorMessage = "Erro de integridade de dados";
    
    // Check if it's a duplicate email error
    if (e.getMessage() != null
        && e.getMessage().contains("Duplicate entry")
        && e.getMessage().contains("users.UK6dotkott2kjsp8vw4d0m25fb7")) {
      errorMessage = "Este e-mail já está cadastrado. Tente outro e-mail.";
    } else if (e.getMessage() != null
        && e.getMessage().contains("Duplicate entry")
        && e.getMessage().contains("email")) {
      errorMessage = "Este e-mail já está cadastrado. Tente outro e-mail.";
    }

    return ResponseEntity.badRequest()
      .body(Map.of("error", errorMessage));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
    logger.warn("Illegal argument: {}", e.getMessage());
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException e) {
    logger.warn("Response Status Exception: {} - {}", e.getStatusCode(), e.getReason());
    return ResponseEntity.status(e.getStatusCode())
      .body(Map.of("error", e.getReason() != null ? e.getReason() : "Request failed"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
    logger.error("Internal Server Error: ", e);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(Map.of("error", "Internal Server Error."));
  }
}