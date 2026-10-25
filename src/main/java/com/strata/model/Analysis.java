package com.strata.model;

import java.time.Instant;

/** A decision-making topic that evidence is submitted against. */
public class Analysis {

  private String analysisId;
  private String requestId;
  private String topic;
  private String description;
  private int minimumEvidenceCount;
  private AnalysisStatus status;
  private Instant createdAt;

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

  /** Returns topic. */
  public String getTopic() {
    return topic;
  }

  /** Sets topic. */
  public void setTopic(String topic) {
    this.topic = topic;
  }

  /** Returns description. */
  public String getDescription() {
    return description;
  }

  /** Sets description. */
  public void setDescription(String description) {
    this.description = description;
  }

  /** Returns minimumEvidenceCount. */
  public int getMinimumEvidenceCount() {
    return minimumEvidenceCount;
  }

  /** Sets minimumEvidenceCount. */
  public void setMinimumEvidenceCount(int minimumEvidenceCount) {
    this.minimumEvidenceCount = minimumEvidenceCount;
  }

  /** Returns status. */
  public AnalysisStatus getStatus() {
    return status;
  }

  /** Sets status. */
  public void setStatus(AnalysisStatus status) {
    this.status = status;
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
