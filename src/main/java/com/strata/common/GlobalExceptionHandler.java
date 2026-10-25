package com.strata.common;

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
}
