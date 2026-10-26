package com.strata;

import com.strata.model.AcceptanceStatus;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.model.Evidence;
import com.strata.store.InMemoryStore;
import java.time.Instant;
import java.util.Map;

/** Fixed, test-only records shared by HTTP tests and the Postman fixture server. */
public final class ConflictFixtures {

  private ConflictFixtures() {}

  /** Adds isolated scenarios without depending on the M2/M3 endpoints. */
  public static void populate(InMemoryStore store) {
    // Postman asserts these IDs and values. Update the collection if a scenario changes.
    // Expected at threshold .6: a/b and b/c both score .8; a/c agree after normalization.
    analysis(store, "m4-conflicts", AnalysisStatus.OPEN);
    evidence(store, "ev-1", "m4-conflicts", "agent-a", "APPROVE", 0.9);
    evidence(store, "ev-2", "m4-conflicts", "agent-b", "REJECT", 0.7);
    evidence(store, "ev-3", "m4-conflicts", "agent-c", "approve", 0.9);
    Evidence rejected = evidence(store, "ev-4", "m4-conflicts", "agent-d", "DEFER", 1);
    rejected.setAcceptanceStatus(AcceptanceStatus.REJECTED);

    // The single scenario has two total records but only one accepted record: expect HTTP 409.
    analysis(store, "m4-empty", AnalysisStatus.OPEN);
    analysis(store, "m4-single", AnalysisStatus.OPEN);
    evidence(store, "ev-single", "m4-single", "agent-a", "APPROVE", 1);
    Evidence singleRejected = evidence(store, "ev-rejected", "m4-single", "agent-b", "REJECT", 1);
    singleRejected.setAcceptanceStatus(AcceptanceStatus.REJECTED);

    // Reading conflicts must also work after consensus/finalization without reopening an analysis.
    analysis(store, "m4-compatible", AnalysisStatus.READY);
    evidence(store, "ev-compatible-1", "m4-compatible", "agent-a", "APPROVE", 0.8);
    evidence(store, "ev-compatible-2", "m4-compatible", "agent-b", " approve ", 0.8);

    analysis(store, "m4-finalized", AnalysisStatus.FINALIZED);
    evidence(store, "ev-final-1", "m4-finalized", "agent-a", "APPROVE", 1);
    evidence(store, "ev-final-2", "m4-finalized", "agent-b", "REJECT", 1);
  }

  private static void analysis(InMemoryStore store, String id, AnalysisStatus status) {
    Analysis analysis = new Analysis();
    analysis.setAnalysisId(id);
    analysis.setRequestId("request-" + id);
    analysis.setTopic("Software change review");
    analysis.setMinimumEvidenceCount(2);
    analysis.setStatus(status);
    analysis.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
    store.saveAnalysis(analysis);
  }

  private static Evidence evidence(InMemoryStore store, String id, String analysisId,
      String sourceId, String claim, double confidence) {
    Evidence item = new Evidence();
    item.setEvidenceId(id);
    item.setAnalysisId(analysisId);
    item.setRequestId("request-" + id);
    item.setSourceId(sourceId);
    item.setClaim(claim);
    item.setEvidenceType("recommendation");
    item.setConfidenceScore(confidence);
    item.setNormalizedConfidenceScore(confidence);
    item.setMetadata(Map.of("fixture", true));
    item.setSubmittedAt(Instant.parse("2026-01-01T00:00:00Z"));
    item.setAcceptanceStatus(AcceptanceStatus.ACCEPTED);
    return store.saveEvidence(item);
  }
}
