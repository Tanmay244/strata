package com.strata.dto;

/** Request for Finalize Decision. */
public class FinalizeDecisionRequest {

  private String requestId;
  private String consensusResultId;
  private Double minimumConfidenceThreshold;

  /** Returns requestId. */
  public String getRequestId() {
    return requestId;
  }

  /** Sets requestId. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** Returns consensusResultId. */
  public String getConsensusResultId() {
    return consensusResultId;
  }

  /** Sets consensusResultId. */
  public void setConsensusResultId(String consensusResultId) {
    this.consensusResultId = consensusResultId;
  }

  /** Returns minimumConfidenceThreshold, or null when not supplied. */
  public Double getMinimumConfidenceThreshold() {
    return minimumConfidenceThreshold;
  }

  /** Sets minimumConfidenceThreshold. */
  public void setMinimumConfidenceThreshold(Double minimumConfidenceThreshold) {
    this.minimumConfidenceThreshold = minimumConfidenceThreshold;
  }
}
