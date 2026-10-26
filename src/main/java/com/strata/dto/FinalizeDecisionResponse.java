package com.strata.dto;

import com.strata.model.AnalysisStatus;
import java.util.List;

/** Response for Finalize Decision. */
public class FinalizeDecisionResponse {

  private String decisionId;
  private String finalDecision;
  private double confidenceScore;
  private AnalysisStatus analysisStatus;
  private List<String> supportingEvidenceIds;

  /** Returns decisionId. */
  public String getDecisionId() {
    return decisionId;
  }

  /** Sets decisionId. */
  public void setDecisionId(String decisionId) {
    this.decisionId = decisionId;
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

  /** Returns analysisStatus. */
  public AnalysisStatus getAnalysisStatus() {
    return analysisStatus;
  }

  /** Sets analysisStatus. */
  public void setAnalysisStatus(AnalysisStatus analysisStatus) {
    this.analysisStatus = analysisStatus;
  }

  /** Returns supportingEvidenceIds. */
  public List<String> getSupportingEvidenceIds() {
    return supportingEvidenceIds;
  }

  /** Sets supportingEvidenceIds. */
  public void setSupportingEvidenceIds(List<String> supportingEvidenceIds) {
    this.supportingEvidenceIds = supportingEvidenceIds;
  }
}
