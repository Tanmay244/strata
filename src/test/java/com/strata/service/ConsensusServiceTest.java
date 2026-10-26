package com.strata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.strata.dto.ComputeConsensusResponse;
import com.strata.model.AcceptanceStatus;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.model.Evidence;
import com.strata.store.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ConsensusServiceTest {

  @Test
  void groupsClaimsWeightsWinnerAndUpdatesAnalysis() {
    InMemoryStore store = new InMemoryStore();
    Analysis analysis = analysis("ana-1", 3);
    store.saveAnalysis(analysis);
    store.saveEvidence(evidence("ev-1", "ana-1", "Approve", 0.4, AcceptanceStatus.ACCEPTED));
    store.saveEvidence(evidence("ev-2", "ana-1", " approve ", 0.3, AcceptanceStatus.ACCEPTED));
    store.saveEvidence(evidence("ev-3", "ana-1", "Reject", 0.2, AcceptanceStatus.ACCEPTED));
    store.saveEvidence(evidence("ev-4", "ana-1", "Approve", 0.9, AcceptanceStatus.REJECTED));

    ComputeConsensusResponse response = new ConsensusService(store).computeConsensus("ana-1");

    assertEquals("Approve", response.getConsensusClaim());
    assertEquals(0.7 / 0.9, response.getConsensusConfidenceScore(), 0.000001);
    assertEquals(3, response.getEvidenceCount());
    assertEquals(AnalysisStatus.READY, analysis.getStatus());
    assertEquals(response.getConsensusResultId(),
        store.findConsensusByAnalysis("ana-1").orElseThrow().getConsensusResultId());
  }

  @Test
  void rejectsWhenMinimumEvidenceCountIsNotMet() {
    InMemoryStore store = new InMemoryStore();
    store.saveAnalysis(analysis("ana-1", 2));
    store.saveEvidence(evidence("ev-1", "ana-1", "Approve", 1.0, AcceptanceStatus.ACCEPTED));

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> new ConsensusService(store).computeConsensus("ana-1"));

    assertEquals(409, exception.getStatusCode().value());
    assertEquals(AnalysisStatus.OPEN, store.findAnalysis("ana-1").orElseThrow().getStatus());
  }

  @Test
  void rejectsInvalidAcceptedEvidenceWithoutChangingAnalysis() {
    InMemoryStore store = new InMemoryStore();
    Analysis analysis = analysis("ana-1", 1);
    store.saveAnalysis(analysis);
    store.saveEvidence(evidence("ev-1", "ana-1", "Approve", Double.NaN, AcceptanceStatus.ACCEPTED));

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> new ConsensusService(store).computeConsensus("ana-1"));

    assertEquals(409, exception.getStatusCode().value());
    assertEquals(AnalysisStatus.OPEN, analysis.getStatus());
    assertEquals(0, store.findConsensusByAnalysis("ana-1").stream().count());
  }

  @Test
  void rejectsUnknownAnalysis() {
    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> new ConsensusService(new InMemoryStore()).computeConsensus("missing"));

    assertEquals(404, exception.getStatusCode().value());
  }

  private static Analysis analysis(String id, int minimumEvidenceCount) {
    Analysis analysis = new Analysis();
    analysis.setAnalysisId(id);
    analysis.setMinimumEvidenceCount(minimumEvidenceCount);
    analysis.setStatus(AnalysisStatus.OPEN);
    return analysis;
  }

  private static Evidence evidence(
      String id, String analysisId, String claim, double confidence, AcceptanceStatus status) {
    Evidence evidence = new Evidence();
    evidence.setEvidenceId(id);
    evidence.setAnalysisId(analysisId);
    evidence.setClaim(claim);
    evidence.setNormalizedConfidenceScore(confidence);
    evidence.setAcceptanceStatus(status);
    return evidence;
  }
}
