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
  public synchronized ComputeConsensusResponse computeConsensus(String analysisId) {
    requireAnalysisId(analysisId);
    Analysis analysis = findAnalysis(analysisId);
    ConsensusResult existingResult = store.findConsensusByAnalysis(analysisId).orElse(null);
    if (existingResult != null) {
      return toResponse(existingResult);
    }
    if (analysis.getStatus() == AnalysisStatus.FINALIZED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Finalized analysis cannot compute consensus");
    }
    List<Evidence> acceptedEvidence = findAcceptedEvidence(analysisId);
    validateEvidenceCount(acceptedEvidence, analysis.getMinimumEvidenceCount());
    ConsensusSelection selection = selectConsensus(acceptedEvidence);
    ConsensusResult result = saveConsensus(analysisId, selection);
    markReady(analysis, result);
    return toResponse(result);
  }

  private static void requireAnalysisId(String analysisId) {
    if (analysisId == null || analysisId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "analysisId is required");
    }
  }

  private Analysis findAnalysis(String analysisId) {
    return store.findAnalysis(analysisId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Analysis does not exist"));
  }

  private List<Evidence> findAcceptedEvidence(String analysisId) {
    return store.findEvidenceByAnalysis(analysisId).stream()
        .filter(item -> item.getAcceptanceStatus() == AcceptanceStatus.ACCEPTED)
        .sorted(Comparator.comparing(
            Evidence::getEvidenceId, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private static void validateEvidenceCount(List<Evidence> evidence, int minimumEvidenceCount) {
    if (minimumEvidenceCount < 1) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Analysis minimum evidence count must be positive");
    }
    if (evidence.size() < minimumEvidenceCount) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Minimum evidence count has not been met");
    }
  }

  private static ConsensusSelection selectConsensus(List<Evidence> evidence) {
    Map<String, ClaimGroup> groups = new LinkedHashMap<>();
    double totalWeight = 0;
    for (Evidence item : evidence) {
      validateEvidence(item);
      double confidence = item.getNormalizedConfidenceScore();
      totalWeight += confidence;
      ClaimGroup group = groups.computeIfAbsent(
          normalize(item.getClaim()), key -> new ClaimGroup(item.getClaim()));
      group.add(confidence);
    }
    if (totalWeight <= 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Accepted evidence must have positive confidence");
    }

    ClaimGroup winner = groups.values().stream()
        .max(Comparator.comparingDouble(ClaimGroup::weight)
            .thenComparing(ClaimGroup::claim, Comparator.reverseOrder()))
        .orElseThrow();
    return new ConsensusSelection(winner.claim(), winner.weight() / totalWeight, evidence.size());
  }

  private static void validateEvidence(Evidence evidence) {
    if (evidence.getEvidenceId() == null || evidence.getEvidenceId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Accepted evidence has no id");
    }
    if (evidence.getClaim() == null || evidence.getClaim().isBlank()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Accepted evidence has no claim");
    }
    double confidence = evidence.getNormalizedConfidenceScore();
    if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Accepted evidence has an invalid confidence score");
    }
  }

  private ConsensusResult saveConsensus(String analysisId, ConsensusSelection selection) {
    ConsensusResult result = new ConsensusResult();
    result.setConsensusResultId(store.newId("con"));
    result.setAnalysisId(analysisId);
    result.setConsensusClaim(selection.claim());
    result.setConsensusConfidenceScore(selection.confidence());
    result.setEvidenceCount(selection.evidenceCount());
    result.setCreatedAt(Instant.now());
    return store.saveConsensus(result);
  }

  private void markReady(Analysis analysis, ConsensusResult result) {
    analysis.setStatus(AnalysisStatus.READY);
    store.saveAnalysis(analysis);
  }

  private static ComputeConsensusResponse toResponse(ConsensusResult result) {
    return new ComputeConsensusResponse(
        result.getConsensusResultId(),
        result.getConsensusClaim(),
        result.getConsensusConfidenceScore(),
        result.getEvidenceCount());
  }

  private static String normalize(String claim) {
    return claim.strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private record ConsensusSelection(String claim, double confidence, int evidenceCount) {}

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
