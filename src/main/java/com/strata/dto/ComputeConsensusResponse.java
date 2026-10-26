package com.strata.dto;

/** Response for Compute Consensus. */
public class ComputeConsensusResponse {

  private final String consensusResultId;
  private final String consensusClaim;
  private final double consensusConfidenceScore;
  private final int evidenceCount;

  /** Creates a consensus response. */
  public ComputeConsensusResponse(
      String consensusResultId,
      String consensusClaim,
      double consensusConfidenceScore,
      int evidenceCount) {
    this.consensusResultId = consensusResultId;
    this.consensusClaim = consensusClaim;
    this.consensusConfidenceScore = consensusConfidenceScore;
    this.evidenceCount = evidenceCount;
  }

  /** Returns consensusResultId. */
  public String getConsensusResultId() {
    return consensusResultId;
  }

  /** Returns consensusClaim. */
  public String getConsensusClaim() {
    return consensusClaim;
  }

  /** Returns consensusConfidenceScore. */
  public double getConsensusConfidenceScore() {
    return consensusConfidenceScore;
  }

  /** Returns evidenceCount. */
  public int getEvidenceCount() {
    return evidenceCount;
  }
}
