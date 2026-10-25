package com.strata.service;

import com.strata.dto.ComputeConsensusRequest;
import com.strata.dto.ComputeConsensusResponse;
import org.springframework.stereotype.Service;

/** Business logic for Compute Consensus. */
@Service
public class ConsensusService {

  /** Compute Consensus. TODO(M5): implement. */
  public ComputeConsensusResponse computeConsensus(
      String analysisId,
      ComputeConsensusRequest request) {
    throw new UnsupportedOperationException("Compute Consensus is not implemented yet");
  }
}
