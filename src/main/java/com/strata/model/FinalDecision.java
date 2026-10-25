package com.strata.model;

import java.time.Instant;
import java.util.List;

/** The finalized decision recorded for an analysis. */
public class FinalDecision {

  private String decisionId;
  private String requestId;
  private String analysisId;
  private String consensusResultId;
  private String finalDecision;
  private double confidenceScore;
  private List<String> supportingEvidenceIds;
  private Instant finalizedAt;

  /** Returns decisionId. */
  public String getDecisionId() {
    return decisionId;
  }

  /** Sets decisionId. */
  public void setDecisionId(String decisionId) {
    this.decisionId = decisionId;
  }

  /** Returns requestId. */
  public String getRequestId() {
    return requestId;
  }

  /** Sets requestId. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** Returns analysisId. */
  public String getAnalysisId() {
    return analysisId;
  }

  /** Sets analysisId. */
  public void setAnalysisId(String analysisId) {
    this.analysisId = analysisId;
  }

  /** Returns consensusResultId. */
  public String getConsensusResultId() {
    return consensusResultId;
  }

  /** Sets consensusResultId. */
  public void setConsensusResultId(String consensusResultId) {
    this.consensusResultId = consensusResultId;
  }

  /** Returns finalDecision. */
  public String getFinalDecision() {
    return finalDecision;
  }

  /** Sets finalDecision. */
  public void setFinalDecision(String finalDecision) {
    this.finalDecision = finalDecision;
  }

  /** Returns confidenceScore. */
  public double getConfidenceScore() {
    return confidenceScore;
  }

  /** Sets confidenceScore. */
  public void setConfidenceScore(double confidenceScore) {
    this.confidenceScore = confidenceScore;
  }

  /** Returns supportingEvidenceIds. */
  public List<String> getSupportingEvidenceIds() {
    return supportingEvidenceIds;
  }

  /** Sets supportingEvidenceIds. */
  public void setSupportingEvidenceIds(List<String> supportingEvidenceIds) {
    this.supportingEvidenceIds = supportingEvidenceIds;
  }

  /** Returns finalizedAt. */
  public Instant getFinalizedAt() {
    return finalizedAt;
  }

  /** Sets finalizedAt. */
  public void setFinalizedAt(Instant finalizedAt) {
    this.finalizedAt = finalizedAt;
  }
}
