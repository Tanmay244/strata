package com.strata.common;

/** A referenced resource does not exist (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with a client-facing message. */
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
