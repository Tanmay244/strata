package com.strata.controller;

import com.strata.dto.DetectConflictsResponse;
import com.strata.service.ConflictService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** HTTP endpoint for Detect Conflicts. */
@RestController
@RequestMapping("/analyses")
public class ConflictController {

  private final ConflictService service;

  public ConflictController(ConflictService service) {
    this.service = service;
  }

  /** Returns conflicts at the requested threshold, or 0.6 when it is omitted. */
  @GetMapping("/{analysisId}/conflicts")
  public ResponseEntity<DetectConflictsResponse> detectConflicts(
      @PathVariable String analysisId,
      @RequestParam(required = false) String conflictThreshold) {
    // Bind as text to distinguish an omitted parameter (default) from an empty value (400).
    // Spring's Double binding would otherwise turn an empty query value into null.
    Double threshold = null;
    if (conflictThreshold != null) {
      try {
        threshold = Double.valueOf(conflictThreshold);
      } catch (NumberFormatException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "conflictThreshold must be a finite number in [0, 1]", ex);
      }
    }
    return ResponseEntity.ok(service.detectConflicts(analysisId, threshold));
  }

  /** Returns a structured error for M4 without changing other milestones' error handling. */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> handleConflictError(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason()));
  }
}
