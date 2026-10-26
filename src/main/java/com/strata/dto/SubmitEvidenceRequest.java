package com.strata.dto;

import java.util.Map;

/** Request for Submit Evidence. AI-assisted implementation. */
public class SubmitEvidenceRequest {

  private String requestId;

  private String sourceId;

  private String claim;

  private Double confidenceScore;

  private String evidenceType;

  private Map<String, Object> metadata;

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

  /** Returns confidenceScore. */
  public Double getConfidenceScore() {
    return confidenceScore;
  }

  /** Sets confidenceScore. */
  public void setConfidenceScore(Double confidenceScore) {
    this.confidenceScore = confidenceScore;
  }

  /** Returns evidenceType. */
  public String getEvidenceType() {
    return evidenceType;
  }

  /** Sets evidenceType. */
  public void setEvidenceType(String evidenceType) {
    this.evidenceType = evidenceType;
  }

  /** Returns metadata. */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /** Sets metadata. */
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }
}
