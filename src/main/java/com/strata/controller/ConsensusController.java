package com.strata.controller;

import com.strata.dto.ComputeConsensusResponse;
import com.strata.service.ConsensusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint for Compute Consensus. */
@RestController
@RequestMapping("/analyses")
public class ConsensusController {

  private final ConsensusService service;

  public ConsensusController(ConsensusService service) {
    this.service = service;
  }

  /** Compute Consensus. TODO(M5): implement. */
  @PostMapping("/{analysisId}/consensus")
  public ResponseEntity<ComputeConsensusResponse> computeConsensus(
      @PathVariable String analysisId) {
    return ResponseEntity.ok(service.computeConsensus(analysisId));
  }
}
