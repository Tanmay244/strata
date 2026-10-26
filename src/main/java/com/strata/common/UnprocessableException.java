package com.strata.common;

/** The request is well formed but cannot be processed (HTTP 422). */
public class UnprocessableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with a client-facing message. */
  public UnprocessableException(String message) {
    super(message);
  }
}
