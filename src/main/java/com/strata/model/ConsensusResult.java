package com.strata.model;

import java.time.Instant;

/** The consensus claim computed from an analysis's evidence. */
public class ConsensusResult {

  private String consensusResultId;
  private String analysisId;
  private String consensusClaim;
  private double consensusConfidenceScore;
  private int evidenceCount;
  private Instant createdAt;

  /** Returns consensusResultId. */
  public String getConsensusResultId() {
    return consensusResultId;
  }

  /** Sets consensusResultId. */
  public void setConsensusResultId(String consensusResultId) {
    this.consensusResultId = consensusResultId;
  }

  /** Returns analysisId. */
  public String getAnalysisId() {
    return analysisId;
  }

  /** Sets analysisId. */
  public void setAnalysisId(String analysisId) {
    this.analysisId = analysisId;
  }

  /** Returns consensusClaim. */
  public String getConsensusClaim() {
    return consensusClaim;
  }

  /** Sets consensusClaim. */
  public void setConsensusClaim(String consensusClaim) {
    this.consensusClaim = consensusClaim;
  }

  /** Returns consensusConfidenceScore. */
  public double getConsensusConfidenceScore() {
    return consensusConfidenceScore;
  }

  /** Sets consensusConfidenceScore. */
  public void setConsensusConfidenceScore(double consensusConfidenceScore) {
    this.consensusConfidenceScore = consensusConfidenceScore;
  }

  /** Returns evidenceCount. */
  public int getEvidenceCount() {
    return evidenceCount;
  }

  /** Sets evidenceCount. */
  public void setEvidenceCount(int evidenceCount) {
    this.evidenceCount = evidenceCount;
  }

  /** Returns createdAt. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Sets createdAt. */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
