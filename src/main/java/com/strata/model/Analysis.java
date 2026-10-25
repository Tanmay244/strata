package com.strata.model;

import java.time.Instant;

/** A topic under analysis that evidence is submitted against. */
public class Analysis {

  private String analysisId;
  private String requestId;
  private String topic;
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
