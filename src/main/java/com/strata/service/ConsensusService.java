package com.strata.service;

import com.strata.dto.ComputeConsensusResponse;
import com.strata.model.AcceptanceStatus;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.model.ConsensusResult;
import com.strata.model.Evidence;
import com.strata.store.InMemoryStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Business logic for Compute Consensus. */
@Service
public class ConsensusService {

  private final InMemoryStore store;

  public ConsensusService(InMemoryStore store) {
    this.store = store;
  }

  /** Computes and stores the highest-confidence compatible claim. */
  public ComputeConsensusResponse computeConsensus(String analysisId) {
    if (analysisId == null || analysisId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "analysisId is required");
    }

    Analysis analysis = store.findAnalysis(analysisId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Analysis does not exist"));

    List<Evidence> acceptedEvidence = store.findEvidenceByAnalysis(analysisId).stream()
        .filter(item -> item.getAcceptanceStatus() == AcceptanceStatus.ACCEPTED)
        .sorted(Comparator.comparing(Evidence::getEvidenceId))
        .toList();
    if (acceptedEvidence.size() < analysis.getMinimumEvidenceCount()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Minimum evidence count has not been met");
    }
    if (acceptedEvidence.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "At least one accepted evidence record is required");
    }

    double totalWeight = acceptedEvidence.stream()
        .mapToDouble(Evidence::getNormalizedConfidenceScore)
        .sum();
    if (totalWeight <= 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Accepted evidence must have positive confidence");
    }

    Map<String, ClaimGroup> groups = new LinkedHashMap<>();
    for (Evidence evidence : acceptedEvidence) {
      String normalizedClaim = normalize(evidence.getClaim());
      ClaimGroup group = groups.computeIfAbsent(
          normalizedClaim, key -> new ClaimGroup(evidence.getClaim()));
      group.add(evidence.getNormalizedConfidenceScore());
    }

    ClaimGroup winningGroup = groups.values().stream()
        .max(Comparator.comparingDouble(ClaimGroup::weight)
            .thenComparing(ClaimGroup::claim, Comparator.reverseOrder()))
        .orElseThrow();
    double normalizedScore = winningGroup.weight() / totalWeight;

    ConsensusResult result = new ConsensusResult();
    result.setConsensusResultId(store.newId("con"));
    result.setAnalysisId(analysisId);
    result.setConsensusClaim(winningGroup.claim());
    result.setConsensusConfidenceScore(normalizedScore);
    result.setEvidenceCount(acceptedEvidence.size());
    result.setCreatedAt(Instant.now());
    store.saveConsensus(result);

    analysis.setStatus(AnalysisStatus.READY);
    store.saveAnalysis(analysis);

    return new ComputeConsensusResponse(
        result.getConsensusResultId(),
        result.getConsensusClaim(),
        result.getConsensusConfidenceScore(),
        result.getEvidenceCount());
  }

  private static String normalize(String claim) {
    return claim == null
        ? ""
        : claim.strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private static final class ClaimGroup {

    private final String claim;
    private double weight;

    private ClaimGroup(String claim) {
      this.claim = claim;
    }

    private void add(double confidence) {
      weight += confidence;
    }

    private String claim() {
      return claim;
    }

    private double weight() {
      return weight;
    }
  }
}
