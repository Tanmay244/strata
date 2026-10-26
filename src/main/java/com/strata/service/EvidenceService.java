package com.strata.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strata.common.IdempotencyHelper;
import com.strata.dto.SubmitEvidenceRequest;
import com.strata.dto.SubmitEvidenceResponse;
import com.strata.model.AcceptanceStatus;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.model.Evidence;
import com.strata.store.InMemoryStore;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Business logic for Submit Evidence. AI-assisted implementation. */
@Service
public class EvidenceService {

  private final InMemoryStore store;
  private final ObjectMapper objectMapper;
  private final IdempotencyHelper<Evidence> requests = new IdempotencyHelper<>();

  public EvidenceService(InMemoryStore store, ObjectMapper objectMapper) {
    this.store = store;
    this.objectMapper = objectMapper;
  }

  /**
   * Validates and stores evidence. Serializes submissions so different request IDs cannot
   * simultaneously insert identical evidence. Failed requests never reserve a request ID.
   */
  public synchronized SubmitEvidenceResponse submitEvidence(
      String analysisId, SubmitEvidenceRequest request) {
    String targetId = requiredText(analysisId, "analysisId");
    if (request == null) {
      throw badRequest("Request body is required");
    }
    Evidence candidate = normalize(targetId, request);
    Evidence existing = requests.find(candidate.getRequestId()).orElse(null);
    if (existing != null) {
      if (!sameSubmission(existing, candidate)) {
        throw conflict("requestId has already been used with incompatible parameters");
      }
      // A successful retry remains valid after the analysis stops accepting new evidence.
      return receipt(existing);
    }

    Analysis analysis = store.findAnalysis(targetId).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis does not exist"));
    if (analysis.getStatus() != AnalysisStatus.OPEN) {
      throw conflict("Analysis is no longer accepting evidence");
    }
    for (Evidence item : store.findEvidenceByAnalysis(targetId)) {
      if (sameSubmission(item, candidate)) {
        throw conflict("Exact evidence submission already exists; retry its original requestId");
      }
    }
    Evidence saved = requests.getOrCreate(candidate.getRequestId(), () -> {
      candidate.setEvidenceId(store.newId("ev"));
      candidate.setSubmittedAt(Instant.now());
      candidate.setAcceptanceStatus(AcceptanceStatus.ACCEPTED);
      return store.saveEvidence(candidate);
    });
    return receipt(saved);
  }

  private Evidence normalize(String analysisId, SubmitEvidenceRequest request) {
    Evidence candidate = new Evidence();
    candidate.setAnalysisId(analysisId);
    candidate.setRequestId(requiredText(request.getRequestId(), "requestId"));
    candidate.setSourceId(requiredText(request.getSourceId(), "sourceId"));
    candidate.setClaim(requiredText(request.getClaim(), "claim").replaceAll("\\s+", " "));
    candidate.setEvidenceType(requiredText(request.getEvidenceType(), "evidenceType"));
    Double confidence = request.getConfidenceScore();
    if (confidence == null || !Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
      throw badRequest("confidenceScore must be a finite number in [0, 1]");
    }
    candidate.setConfidenceScore(confidence);
    // The input already uses the internal [0, 1] scale; canonicalize negative zero only.
    candidate.setNormalizedConfidenceScore(confidence == 0 ? 0 : confidence);
    Map<String, Object> metadata = request.getMetadata() == null ? Map.of()
        : objectMapper.convertValue(request.getMetadata(), new TypeReference<>() {});
    candidate.setMetadata(metadata);
    return candidate;
  }

  private static String requiredText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw badRequest(field + " is required and must not be blank");
    }
    return value.strip();
  }

  private static boolean sameSubmission(Evidence first, Evidence second) {
    return Objects.equals(first.getAnalysisId(), second.getAnalysisId())
        && Objects.equals(first.getSourceId(), second.getSourceId())
        && Objects.equals(first.getClaim(), second.getClaim())
        && Objects.equals(first.getEvidenceType(), second.getEvidenceType())
        && Double.compare(first.getNormalizedConfidenceScore(),
            second.getNormalizedConfidenceScore()) == 0
        && Objects.equals(first.getMetadata(), second.getMetadata());
  }

  private static SubmitEvidenceResponse receipt(Evidence item) {
    return new SubmitEvidenceResponse(item.getEvidenceId(), item.getNormalizedConfidenceScore(),
        item.getSubmittedAt(), item.getAcceptanceStatus());
  }

  private static ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
