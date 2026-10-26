package com.strata.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void invalidRequestMapsTo400() {
    ResponseEntity<Map<String, String>> response =
        handler.handleInvalidRequest(new InvalidRequestException("bad"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("bad", response.getBody().get("error"));
  }

  @Test
  void notFoundMapsTo404() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        handler.handleNotFound(new ResourceNotFoundException("x")).getStatusCode());
  }

  @Test
  void conflictMapsTo409() {
    assertEquals(
        HttpStatus.CONFLICT, handler.handleConflict(new ConflictException("x")).getStatusCode());
  }

  @Test
  void unprocessableMapsTo422() {
    assertEquals(
        HttpStatus.UNPROCESSABLE_ENTITY,
        handler.handleUnprocessable(new UnprocessableException("x")).getStatusCode());
  }
}
