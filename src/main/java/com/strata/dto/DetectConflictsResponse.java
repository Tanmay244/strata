package com.strata.dto;

import java.util.List;

/**
 * Read-only conflict report. The same index in each list describes one evidence pair.
 * For example, conflictingSourcePairs[0], conflictSeverityScores[0], and conflictingClaims[0]
 * describe the first conflict, with the two claims in the same order as their sources.
 *
 * @param conflictingSourcePairs source identifiers, two per pair
 * @param conflictSeverityScores severity in [0, 1] for each pair
 * @param conflictingClaims original claims, in source-pair order
 */
public record DetectConflictsResponse(
    List<List<String>> conflictingSourcePairs,
    List<Double> conflictSeverityScores,
    List<List<String>> conflictingClaims) {

  /** Copies both list levels so callers cannot accidentally change a returned report. */
  public DetectConflictsResponse {
    conflictingSourcePairs = conflictingSourcePairs.stream().map(List::copyOf).toList();
    conflictSeverityScores = List.copyOf(conflictSeverityScores);
    conflictingClaims = conflictingClaims.stream().map(List::copyOf).toList();
  }
}
