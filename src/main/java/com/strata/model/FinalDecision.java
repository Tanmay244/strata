package com.strata.model;

import java.time.Instant;

/** The finalized decision recorded for an analysis. */
public class FinalDecision {

  private String decisionId;
  private String requestId;
  private String analysisId;
  private String consensusResultId;
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

  /** Returns finalizedAt. */
  public Instant getFinalizedAt() {
    return finalizedAt;
  }

  /** Sets finalizedAt. */
  public void setFinalizedAt(Instant finalizedAt) {
    this.finalizedAt = finalizedAt;
  }
}
