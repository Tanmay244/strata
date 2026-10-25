package com.strata.controller;

import com.strata.dto.DetectConflictsRequest;
import com.strata.dto.DetectConflictsResponse;
import com.strata.service.ConflictService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
  @PostMapping("/{analysisId}/conflicts")
  public ResponseEntity<DetectConflictsResponse> detectConflicts(
      @PathVariable String analysisId,
      @RequestBody DetectConflictsRequest request) {
    return ResponseEntity.ok(service.detectConflicts(analysisId, request));
  }
}
