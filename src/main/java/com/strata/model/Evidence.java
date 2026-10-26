package com.strata.model;

import java.time.Instant;
import java.util.Map;

/** A structured claim with a confidence score from one source. */
public class Evidence {

  private String evidenceId;
  private String analysisId;
  private String requestId;
  private String sourceId;
  private String claim;
  private String evidenceType;
  private double confidenceScore;
  private double normalizedConfidenceScore;
  private Map<String, Object> metadata;
  private Instant submittedAt;
  private AcceptanceStatus acceptanceStatus;

  /** Returns evidenceId. */
  public String getEvidenceId() {
    return evidenceId;
  }

  /** Sets evidenceId. */
  public void setEvidenceId(String evidenceId) {
    this.evidenceId = evidenceId;
  }

  /** Returns analysisId. */
  public String getAnalysisId() {
    return analysisId;
  }

  /** Sets analysisId. */
  public void setAnalysisId(String analysisId) {
    this.analysisId = analysisId;
  }

  /** Returns requestId. */
  public String getRequestId() {
    return requestId;
  }

  /** Sets requestId. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** Returns sourceId. */
  public String getSourceId() {
    return sourceId;
  }

  /** Sets sourceId. */
  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  /** Returns claim. */
  public String getClaim() {
    return claim;
  }

  /** Sets claim. */
  public void setClaim(String claim) {
    this.claim = claim;
  }

  /** Returns evidenceType. */
  public String getEvidenceType() {
    return evidenceType;
  }

  /** Sets evidenceType. */
  public void setEvidenceType(String evidenceType) {
    this.evidenceType = evidenceType;
  }

  /** Returns confidenceScore. */
  public double getConfidenceScore() {
    return confidenceScore;
  }

  /** Sets confidenceScore. */
  public void setConfidenceScore(double confidenceScore) {
    this.confidenceScore = confidenceScore;
  }

  /** Returns normalizedConfidenceScore. */
  public double getNormalizedConfidenceScore() {
    return normalizedConfidenceScore;
  }

  /** Sets normalizedConfidenceScore. */
  public void setNormalizedConfidenceScore(double normalizedConfidenceScore) {
    this.normalizedConfidenceScore = normalizedConfidenceScore;
  }

  /** Returns metadata. */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /** Sets metadata. */
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  /** Returns submittedAt. */
  public Instant getSubmittedAt() {
    return submittedAt;
  }

  /** Sets submittedAt. */
  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
  }

  /** Returns acceptanceStatus. */
  public AcceptanceStatus getAcceptanceStatus() {
    return acceptanceStatus;
  }

  /** Sets acceptanceStatus. */
  public void setAcceptanceStatus(AcceptanceStatus acceptanceStatus) {
    this.acceptanceStatus = acceptanceStatus;
  }
}
