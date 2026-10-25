package com.strata.controller;

import com.strata.dto.DetectConflictsResponse;
import com.strata.service.ConflictService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint for Detect Conflicts. */
@RestController
@RequestMapping("/analyses")
public class ConflictController {

  private final ConflictService service;

  public ConflictController(ConflictService service) {
    this.service = service;
  }

  /** Detect Conflicts. TODO(M4): implement. */
  @GetMapping("/{analysisId}/conflicts")
  public ResponseEntity<DetectConflictsResponse> detectConflicts(
      @PathVariable String analysisId,
      @RequestParam(required = false) Double conflictThreshold) {
    return ResponseEntity.ok(service.detectConflicts(analysisId, conflictThreshold));
  }
}
