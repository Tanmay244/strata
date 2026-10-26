package com.strata.controller;

import com.strata.dto.CreateAnalysisRequest;
import com.strata.dto.CreateAnalysisResponse;
import com.strata.service.AnalysisService;
import org.springframework.http.HttpStatus;
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

  /** Create Analysis. Answers 201 when created and 200 when a repeated requestId is replayed. */
  @PostMapping
  public ResponseEntity<CreateAnalysisResponse> createAnalysis(
      @RequestBody CreateAnalysisRequest request) {
    CreateAnalysisResponse response = service.createAnalysis(request);
    HttpStatus status = response.isReplayed() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status).body(response);
  }
}
