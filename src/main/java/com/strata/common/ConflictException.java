package com.strata.common;

/** The request conflicts with the current state of a resource (HTTP 409). */
public class ConflictException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with a client-facing message. */
  public ConflictException(String message) {
    super(message);
  }
}
