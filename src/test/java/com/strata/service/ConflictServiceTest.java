package com.strata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Tests scoring in isolation; mocked store interactions also enforce the read-only contract. */
class ConflictServiceTest {

  private InMemoryStore store;
  private ConflictService service;

  @BeforeEach
  void setUp() {
    store = mock(InMemoryStore.class);
    service = new ConflictService(store);
  }

  @Test
  void defaultThresholdFiltersPairsAndKeepsAllThreeListsAligned() {
    Evidence first = evidence("ev-1", "agent-a", "APPROVE", 0.9);
    Evidence second = evidence("ev-2", "agent-b", "REJECT", 0.7);
    Evidence third = evidence("ev-3", "agent-c", "DEFER", 0.2);
    // Deliberately reverse datastore order to verify that the API sorts by evidence ID.
    givenEvidence(List.of(third, second, first));

    DetectConflictsResponse response = service.detectConflicts("ana-1", null);

    assertEquals(List.of(List.of("agent-a", "agent-b"), List.of("agent-a", "agent-c")),
        response.conflictingSourcePairs());
    assertEquals(List.of(0.8, 0.7), response.conflictSeverityScores());
    assertEquals(List.of(List.of("APPROVE", "REJECT"), List.of("APPROVE", "DEFER")),
        response.conflictingClaims());
    assertEquals(response, service.detectConflicts("ana-1", 0.6));
    assertEquals("APPROVE", first.getClaim());
    assertEquals(0.9, first.getNormalizedConfidenceScore());
  }

  @Test
  void comparesEveryUnorderedPairExactlyOnce() {
    givenEvidence(List.of(evidence("ev-3", "c", "DEFER", 1),
        evidence("ev-1", "a", "APPROVE", 1), evidence("ev-2", "b", "REJECT", 1)));

    DetectConflictsResponse response = service.detectConflicts("ana-1", 1.0);

    assertEquals(List.of(List.of("a", "b"), List.of("a", "c"), List.of("b", "c")),
        response.conflictingSourcePairs());
    assertEquals(List.of(1.0, 1.0, 1.0), response.conflictSeverityScores());
  }

  @Test
  void thresholdIsInclusiveAndUsesNormalizedConfidence() {
    Evidence first = evidence("ev-1", "a", "APPROVE", 0.8);
    // Different raw/normalized values catch accidentally reading the pre-normalization score.
    first.setConfidenceScore(0.1);
    givenEvidence(List.of(first, evidence("ev-2", "b", "REJECT", 0.4)));

    assertEquals(1, service.detectConflicts("ana-1", 0.6).conflictingSourcePairs().size());
    assertTrue(service.detectConflicts("ana-1", 0.61).conflictingSourcePairs().isEmpty());
  }

  @Test
  void equivalentClaimsAndEqualConfidenceDoNotConflictEvenAtZeroThreshold() {
    givenEvidence(List.of(evidence("ev-1", "a", "  APPROVE\t CHANGE  ", 0.9),
        evidence("ev-2", "b", "approve change", 0.9)));

    DetectConflictsResponse response = service.detectConflicts("ana-1", 0.0);

    assertTrue(response.conflictingSourcePairs().isEmpty());
    assertTrue(response.conflictSeverityScores().isEmpty());
    assertTrue(response.conflictingClaims().isEmpty());
  }

  @Test
  void confidenceGapCanConflictEvenWhenClaimsMatch() {
    givenEvidence(List.of(evidence("ev-1", "a", "APPROVE", 0.9),
        evidence("ev-2", "b", "approve", 0.1)));

    assertEquals(List.of(0.8), service.detectConflicts("ana-1", null).conflictSeverityScores());
  }

  @Test
  void ignoresRejectedEvidenceAndDoesNotCompareSourceWithItself() {
    Evidence rejected = evidence("ev-3", "b", "DEFER", 1);
    rejected.setAcceptanceStatus(AcceptanceStatus.REJECTED);
    givenEvidence(List.of(evidence("ev-1", "a", "APPROVE", 1),
        evidence("ev-2", "a", "REJECT", 1), rejected));

    assertTrue(service.detectConflicts("ana-1", null).conflictingSourcePairs().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY,
      Double.NEGATIVE_INFINITY})
  void rejectsInvalidThresholdBeforeReadingStore(double threshold) {
    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> service.detectConflicts("ana-1", threshold));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(store);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void rejectsMissingAnalysisId(String analysisId) {
    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> service.detectConflicts(analysisId, null));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(store);
  }

  @Test
  void missingAnalysisIsNotFound() {
    when(store.findAnalysis("missing")).thenReturn(Optional.empty());

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> service.detectConflicts("missing", null));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    verify(store).findAnalysis("missing");
    verifyNoMoreInteractions(store);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void requiresTwoAcceptedRecords(int count) {
    Evidence rejected = evidence("ev-rejected", "c", "REJECT", 1);
    rejected.setAcceptanceStatus(AcceptanceStatus.REJECTED);
    givenEvidence(count == 0 ? List.of(rejected)
        : List.of(rejected, evidence("ev-1", "a", "APPROVE", 1)));

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> service.detectConflicts("ana-1", null));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verify(store).findAnalysis("ana-1");
    verify(store).findEvidenceByAnalysis("ana-1");
    verifyNoMoreInteractions(store);
  }

  @Test
  void successOnlyReadsStoreAndReturnsImmutableLists() {
    givenEvidence(List.of(evidence("ev-1", "a", "APPROVE", 1),
        evidence("ev-2", "b", "REJECT", 1)));

    final DetectConflictsResponse response = service.detectConflicts("ana-1", null);

    // No extra store calls are allowed: this catches accidental writes or ID allocation.
    verify(store).findAnalysis("ana-1");
    verify(store).findEvidenceByAnalysis("ana-1");
    verifyNoMoreInteractions(store);
    assertThrows(UnsupportedOperationException.class,
        () -> response.conflictingSourcePairs().get(0).add("c"));
    assertThrows(UnsupportedOperationException.class,
        () -> response.conflictSeverityScores().clear());
    assertThrows(UnsupportedOperationException.class,
        () -> response.conflictingClaims().clear());
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
