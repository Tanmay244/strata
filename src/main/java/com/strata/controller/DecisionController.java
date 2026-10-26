package com.strata.controller;

import com.strata.dto.FinalizeDecisionRequest;
import com.strata.dto.FinalizeDecisionResponse;
import com.strata.service.DecisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint for Finalize Decision. */
@RestController
@RequestMapping("/analyses")
public class DecisionController {

  private final DecisionService service;

  public DecisionController(DecisionService service) {
    this.service = service;
  }

  /** Finalize Decision. */
  @PostMapping("/{analysisId}/decision")
  public ResponseEntity<FinalizeDecisionResponse> finalizeDecision(
      @PathVariable String analysisId,
      @RequestBody FinalizeDecisionRequest request) {
    return ResponseEntity.ok(service.finalizeDecision(analysisId, request));
  }
}
