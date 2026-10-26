package com.strata.service;

import com.strata.common.IdempotencyHelper;
import com.strata.common.InvalidRequestException;
import com.strata.dto.CreateAnalysisRequest;
import com.strata.dto.CreateAnalysisResponse;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.store.InMemoryStore;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

/** Business logic for Create Analysis. */
@Service
public class AnalysisService {

  static final int DEFAULT_MINIMUM_EVIDENCE_COUNT = 1;

  private final InMemoryStore store;
  private final IdempotencyHelper<Analysis> idempotency = new IdempotencyHelper<>();

  public AnalysisService(InMemoryStore store) {
    this.store = store;
  }

  /**
   * Creates an analysis in OPEN status. A repeated requestId returns the original analysis, with
   * the response flagged as replayed.
   */
  public CreateAnalysisResponse createAnalysis(CreateAnalysisRequest request) {
    if (request == null) {
      throw new InvalidRequestException("Request body is required");
    }
    if (isBlank(request.getRequestId())) {
      throw new InvalidRequestException("requestId is required");
    }
    if (isBlank(request.getTopic())) {
      throw new InvalidRequestException("topic is required");
    }
    Integer requested = request.getMinimumEvidenceCount();
    if (requested != null && requested < 0) {
      throw new InvalidRequestException("minimumEvidenceCount must not be negative");
    }
    int minimumEvidenceCount = requested == null ? DEFAULT_MINIMUM_EVIDENCE_COUNT : requested;

    AtomicBoolean created = new AtomicBoolean(false);
    Analysis analysis =
        idempotency.getOrCreate(
            request.getRequestId(),
            () -> {
              created.set(true);
              Analysis fresh = new Analysis();
              fresh.setAnalysisId(store.newId("ana"));
              fresh.setRequestId(request.getRequestId());
              fresh.setTopic(request.getTopic());
              fresh.setDescription(request.getDescription());
              fresh.setMinimumEvidenceCount(minimumEvidenceCount);
              fresh.setStatus(AnalysisStatus.OPEN);
              fresh.setCreatedAt(Instant.now());
              return store.saveAnalysis(fresh);
            });

    CreateAnalysisResponse response = new CreateAnalysisResponse();
    response.setAnalysisId(analysis.getAnalysisId());
    response.setStatus(analysis.getStatus());
    response.setCreatedAt(analysis.getCreatedAt());
    response.setMinimumEvidenceCount(analysis.getMinimumEvidenceCount());
    response.setReplayed(!created.get());
    return response;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
