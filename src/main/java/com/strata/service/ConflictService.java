package com.strata.service;

import com.strata.dto.DetectConflictsResponse;
import com.strata.model.AcceptanceStatus;
import com.strata.model.Evidence;
import com.strata.store.InMemoryStore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * M4: compares accepted evidence without writing to the store or calling an external LLM.
 * M3 owns input validation: accepted records must have identifiers, a claim, and normalized
 * confidence in [0, 1]. Different normalized claims are treated as alternative categories;
 * this rule does not infer whether arbitrary natural-language sentences contradict one another.
 */
@Service
public class ConflictService {

  private static final double DEFAULT_THRESHOLD = 0.6;

  private final InMemoryStore store;

  public ConflictService(InMemoryStore store) {
    this.store = store;
  }

  /**
   * Returns a deterministic report for unchanged evidence. A null threshold means omitted.
   * READY and FINALIZED analyses can also be read. This operation must not change their status,
   * create records, consume IDs, or use the write-operation idempotency helper.
   */
  public DetectConflictsResponse detectConflicts(String analysisId, Double conflictThreshold) {
    double threshold = conflictThreshold == null ? DEFAULT_THRESHOLD : conflictThreshold;
    // NaN passes ordinary less-than/greater-than range checks, so check finiteness explicitly.
    if (!Double.isFinite(threshold) || threshold < 0 || threshold > 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "conflictThreshold must be a finite number in [0, 1]");
    }
    if (analysisId == null || analysisId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "analysisId is required");
    }
    if (store.findAnalysis(analysisId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis does not exist");
    }

    // ConcurrentHashMap iteration is unordered; sort for repeatable pair/response ordering.
    // Filter first so rejected evidence cannot satisfy the two-record prerequisite.
    List<Evidence> accepted = store.findEvidenceByAnalysis(analysisId).stream()
        .filter(item -> item.getAcceptanceStatus() == AcceptanceStatus.ACCEPTED)
        .sorted(Comparator.comparing(Evidence::getEvidenceId))
        .toList();
    if (accepted.size() < 2) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "At least two accepted evidence records are required");
    }

    List<List<String>> sources = new ArrayList<>();
    List<Double> scores = new ArrayList<>();
    List<List<String>> claims = new ArrayList<>();
    for (int i = 0; i < accepted.size(); i++) {
      Evidence first = accepted.get(i);
      // Starting at i + 1 visits each unordered pair once and avoids self-comparison.
      for (int j = i + 1; j < accepted.size(); j++) {
        Evidence second = accepted.get(j);
        // The API reports disagreement across sources, not within one source.
        if (first.getSourceId().equals(second.getSourceId())) {
          continue;
        }
        double severity = severity(first, second);
        // A zero threshold still excludes identical claims with identical confidence.
        if (severity > 0 && severity >= threshold) {
          // Append together: the three response arrays describe the same pair at each index.
          sources.add(List.of(first.getSourceId(), second.getSourceId()));
          scores.add(severity);
          claims.add(List.of(first.getClaim(), second.getClaim()));
        }
      }
    }
    return new DetectConflictsResponse(sources, scores, claims);
  }

  private static double severity(Evidence first, Evidence second) {
    // Use the decimal representation of stored scores so .7 and .6 average to exactly .65.
    // This preserves inclusive threshold boundaries without accepting scores via an epsilon.
    BigDecimal firstConfidence = BigDecimal.valueOf(first.getNormalizedConfidenceScore());
    BigDecimal secondConfidence = BigDecimal.valueOf(second.getNormalizedConfidenceScore());
    // Different recommendations score by average confidence. A large confidence gap can
    // also be a conflict, even when claims match. Both components remain in [0, 1].
    // Example: APPROVE/.9 versus REJECT/.7 gives max(.8, .2) = .8.
    BigDecimal disagreement = normalize(first.getClaim()).equals(normalize(second.getClaim()))
        ? BigDecimal.ZERO : firstConfidence.add(secondConfidence).divide(BigDecimal.valueOf(2));
    return disagreement.max(firstConfidence.subtract(secondConfidence).abs()).doubleValue();
  }

  private static String normalize(String claim) {
    // Normalize only for comparison; preserve original stored claims and response text.
    return claim.strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);
  }
}
