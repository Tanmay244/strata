package com.strata.controller;

import com.strata.dto.CreateAnalysisRequest;
import com.strata.dto.CreateAnalysisResponse;
import com.strata.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint for Create Analysis. */
@RestController
@RequestMapping("/analyses")
public class AnalysisController {

  private final AnalysisService service;

  public AnalysisController(AnalysisService service) {
    this.service = service;
  }

  /** Create Analysis. TODO(M2): implement. */
  @PostMapping
  public ResponseEntity<CreateAnalysisResponse> createAnalysis(
      @RequestBody CreateAnalysisRequest request) {
    return ResponseEntity.ok(service.createAnalysis(request));
  }
}
