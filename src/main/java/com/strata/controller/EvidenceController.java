package com.strata.controller;

import com.strata.dto.SubmitEvidenceRequest;
import com.strata.dto.SubmitEvidenceResponse;
import com.strata.service.EvidenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint for Submit Evidence. AI-assisted implementation. */
@RestController
@RequestMapping("/analyses")
public class EvidenceController {

  private final EvidenceService service;

  public EvidenceController(EvidenceService service) {
    this.service = service;
  }

  /** Submits evidence or returns its original receipt on retry. */
  @PostMapping("/{analysisId}/evidence")
  public ResponseEntity<SubmitEvidenceResponse> submitEvidence(
      @PathVariable String analysisId,
      @RequestBody SubmitEvidenceRequest request) {
    return ResponseEntity.ok(service.submitEvidence(analysisId, request));
  }
}
