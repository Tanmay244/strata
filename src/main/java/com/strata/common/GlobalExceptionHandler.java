package com.strata.common;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps exceptions to HTTP responses. Extend as operations add their own error cases. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Endpoints that are still stubs answer 501 Not Implemented. */
  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<String> handleNotImplemented(UnsupportedOperationException ex) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ex.getMessage());
  }

  /** Missing or invalid request fields answer 400 Bad Request. */
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<Map<String, String>> handleInvalidRequest(InvalidRequestException ex) {
    return error(HttpStatus.BAD_REQUEST, ex);
  }

  /** Unknown resources answer 404 Not Found. */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex);
  }

  /** State conflicts, such as an already finalized analysis, answer 409 Conflict. */
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Map<String, String>> handleConflict(ConflictException ex) {
    return error(HttpStatus.CONFLICT, ex);
  }

  /** Requests that cannot be processed, such as a bad consensus, answer 422. */
  @ExceptionHandler(UnprocessableException.class)
  public ResponseEntity<Map<String, String>> handleUnprocessable(UnprocessableException ex) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, ex);
  }

  private static ResponseEntity<Map<String, String>> error(HttpStatus status, Exception ex) {
    return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
  }
}
