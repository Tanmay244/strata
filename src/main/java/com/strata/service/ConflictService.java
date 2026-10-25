package com.strata.service;

import com.strata.dto.DetectConflictsRequest;
import com.strata.dto.DetectConflictsResponse;
import org.springframework.stereotype.Service;

/** Business logic for Detect Conflicts. */
@Service
public class ConflictService {

  /** Detect Conflicts. TODO(M4): implement. */
  public DetectConflictsResponse detectConflicts(
      String analysisId,
      DetectConflictsRequest request) {
    throw new UnsupportedOperationException("Detect Conflicts is not implemented yet");
  }
}
