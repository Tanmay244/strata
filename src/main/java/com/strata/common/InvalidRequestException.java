package com.strata.common;

/** A request field is missing or invalid (HTTP 400). */
public class InvalidRequestException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with a client-facing message. */
  public InvalidRequestException(String message) {
    super(message);
  }
}
