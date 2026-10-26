package com.strata.service;

import com.strata.dto.FinalizeDecisionRequest;
import com.strata.dto.FinalizeDecisionResponse;
import org.springframework.stereotype.Service;

/** Business logic for Finalize Decision. */
@Service
public class DecisionService {

  /** Finalize Decision. TODO(M2): implement. */
  public FinalizeDecisionResponse finalizeDecision(
      String analysisId,
      FinalizeDecisionRequest request) {
    throw new UnsupportedOperationException(
        "Finalize Decision is not implemented yet");
  }
}
