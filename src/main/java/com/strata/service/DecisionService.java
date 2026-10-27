package com.strata.service;

import com.strata.common.ConflictException;
import com.strata.common.IdempotencyHelper;
import com.strata.common.InvalidRequestException;
import com.strata.common.ResourceNotFoundException;
import com.strata.common.UnprocessableException;
import com.strata.dto.FinalizeDecisionRequest;
import com.strata.dto.FinalizeDecisionResponse;
import com.strata.model.AcceptanceStatus;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.model.ConsensusResult;
import com.strata.model.Evidence;
import com.strata.model.FinalDecision;
import com.strata.store.InMemoryStore;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business logic for Finalize Decision.
 *
 * <p>AI-assisted; see docs/m2-ai-usage.md.
 */
@Service
public class DecisionService {

  private final InMemoryStore store;
  private final IdempotencyHelper<FinalDecision> idempotency = new IdempotencyHelper<>();

  public DecisionService(InMemoryStore store) {
    this.store = store;
  }

  /**
   * Records the final decision for an analysis from its consensus result and marks the analysis
   * FINALIZED. A repeated requestId returns the original decision.
   */
  public FinalizeDecisionResponse finalizeDecision(
      String analysisId,
      FinalizeDecisionRequest request) {
    if (request == null) {
      throw new InvalidRequestException("Request body is required");
    }
    if (isBlank(request.getRequestId())) {
      throw new InvalidRequestException("requestId is required");
    }
    if (isBlank(request.getConsensusResultId())) {
      throw new InvalidRequestException("consensusResultId is required");
    }
    Double threshold = request.getMinimumConfidenceThreshold();
    if (threshold != null && (threshold.isNaN() || threshold < 0.0 || threshold > 1.0)) {
      throw new InvalidRequestException("minimumConfidenceThreshold must be between 0 and 1");
    }

    // A replay must be answered before the FINALIZED check, which the first call itself triggers.
    FinalDecision existing = idempotency.find(request.getRequestId()).orElse(null);
    if (existing != null) {
      return toResponse(existing, AnalysisStatus.FINALIZED);
    }

    Analysis analysis =
        store
            .findAnalysis(analysisId)
            .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));
    if (analysis.getStatus() == AnalysisStatus.FINALIZED) {
      throw new ConflictException("Analysis is already finalized: " + analysisId);
    }
    ConsensusResult consensus =
        store
            .findConsensusByAnalysis(analysisId)
            .orElseThrow(
                () ->
                    new UnprocessableException(
                        "No consensus result for analysis: " + analysisId));
    if (!consensus.getConsensusResultId().equals(request.getConsensusResultId())) {
      throw new UnprocessableException(
          "consensusResultId does not match the consensus of analysis: " + analysisId);
    }
    if (threshold != null && consensus.getConsensusConfidenceScore() < threshold) {
      throw new UnprocessableException(
          "Consensus confidence "
              + consensus.getConsensusConfidenceScore()
              + " is below the minimum threshold "
              + threshold);
    }

    FinalDecision decision =
        idempotency.getOrCreate(
            request.getRequestId(), () -> createDecision(analysis, consensus, request));
    return toResponse(decision, analysis.getStatus());
  }

  private FinalDecision createDecision(
      Analysis analysis, ConsensusResult consensus, FinalizeDecisionRequest request) {
    FinalDecision decision = new FinalDecision();
    decision.setDecisionId(store.newId("dec"));
    decision.setRequestId(request.getRequestId());
    decision.setAnalysisId(analysis.getAnalysisId());
    decision.setConsensusResultId(consensus.getConsensusResultId());
    decision.setFinalDecision(consensus.getConsensusClaim());
    decision.setConfidenceScore(consensus.getConsensusConfidenceScore());
    decision.setSupportingEvidenceIds(acceptedEvidenceIds(analysis.getAnalysisId()));
    decision.setFinalizedAt(Instant.now());
    store.saveDecision(decision);

    analysis.setStatus(AnalysisStatus.FINALIZED);
    store.saveAnalysis(analysis);
    return decision;
  }

  private List<String> acceptedEvidenceIds(String analysisId) {
    return store.findEvidenceByAnalysis(analysisId).stream()
        .filter(item -> item.getAcceptanceStatus() == AcceptanceStatus.ACCEPTED)
        .map(Evidence::getEvidenceId)
        .toList();
  }

  private static FinalizeDecisionResponse toResponse(
      FinalDecision decision, AnalysisStatus status) {
    FinalizeDecisionResponse response = new FinalizeDecisionResponse();
    response.setDecisionId(decision.getDecisionId());
    response.setFinalDecision(decision.getFinalDecision());
    response.setConfidenceScore(decision.getConfidenceScore());
    response.setAnalysisStatus(status);
    response.setSupportingEvidenceIds(decision.getSupportingEvidenceIds());
    return response;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
