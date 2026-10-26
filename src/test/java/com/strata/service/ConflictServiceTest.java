package com.strata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.strata.dto.DetectConflictsResponse;
import com.strata.model.AcceptanceStatus;
import com.strata.model.Analysis;
import com.strata.model.Evidence;
import com.strata.store.InMemoryStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Basic service test for default-threshold conflict detection and read-only store access. */
class ConflictServiceTest {

  private InMemoryStore store;
  private ConflictService service;

  @BeforeEach
  void setUp() {
    store = mock(InMemoryStore.class);
    service = new ConflictService(store);
  }

  @Test
  void defaultThresholdReturnsAlignedConflictWithoutChangingEvidence() {
    Evidence first = evidence("ev-1", "agent-a", "APPROVE", 0.9);
    Evidence second = evidence("ev-2", "agent-b", "REJECT", 0.7);
    givenEvidence(List.of(second, first));

    final DetectConflictsResponse response = service.detectConflicts("ana-1", null);

    assertEquals(List.of(List.of("agent-a", "agent-b")), response.conflictingSourcePairs());
    assertEquals(List.of(0.8), response.conflictSeverityScores());
    assertEquals(List.of(List.of("APPROVE", "REJECT")), response.conflictingClaims());
    assertEquals("APPROVE", first.getClaim());
    assertEquals(0.9, first.getNormalizedConfidenceScore());
    // Catch accidental store writes or ID allocation on the successful path.
    verify(store).findAnalysis("ana-1");
    verify(store).findEvidenceByAnalysis("ana-1");
    verifyNoMoreInteractions(store);
  }

  private void givenEvidence(List<Evidence> items) {
    when(store.findAnalysis("ana-1")).thenReturn(Optional.of(new Analysis()));
    when(store.findEvidenceByAnalysis("ana-1")).thenReturn(items);
  }

  private static Evidence evidence(String id, String source, String claim, double confidence) {
    Evidence item = new Evidence();
    item.setEvidenceId(id);
    item.setAnalysisId("ana-1");
    item.setSourceId(source);
    item.setClaim(claim);
    item.setNormalizedConfidenceScore(confidence);
    item.setAcceptanceStatus(AcceptanceStatus.ACCEPTED);
    return item;
  }
}
