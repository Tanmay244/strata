package com.strata.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.strata.model.AnalysisStatus;
import java.time.Instant;

/** Response for Create Analysis. */
public class CreateAnalysisResponse {

  private String analysisId;
  private AnalysisStatus status;
  private Instant createdAt;
  private int minimumEvidenceCount;
  private boolean replayed;

  /** Returns analysisId. */
  public String getAnalysisId() {
    return analysisId;
  }

  /** Sets analysisId. */
  public void setAnalysisId(String analysisId) {
    this.analysisId = analysisId;
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

  /** Returns minimumEvidenceCount. */
  public int getMinimumEvidenceCount() {
    return minimumEvidenceCount;
  }

  /** Sets minimumEvidenceCount. */
  public void setMinimumEvidenceCount(int minimumEvidenceCount) {
    this.minimumEvidenceCount = minimumEvidenceCount;
  }

  /** True when this response replays an earlier request with the same requestId. */
  @JsonIgnore
  public boolean isReplayed() {
    return replayed;
  }

  /** Marks this response as a replay. Not part of the JSON body. */
  public void setReplayed(boolean replayed) {
    this.replayed = replayed;
  }
}
