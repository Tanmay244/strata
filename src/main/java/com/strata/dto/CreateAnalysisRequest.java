package com.strata.dto;

/** Request for Create Analysis. */
public class CreateAnalysisRequest {

  private String requestId;
  private String topic;
  private String description;
  private Integer minimumEvidenceCount;

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

  /** Returns minimumEvidenceCount, or null when not supplied. */
  public Integer getMinimumEvidenceCount() {
    return minimumEvidenceCount;
  }

  /** Sets minimumEvidenceCount. */
  public void setMinimumEvidenceCount(Integer minimumEvidenceCount) {
    this.minimumEvidenceCount = minimumEvidenceCount;
  }
}
