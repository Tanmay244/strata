package com.strata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strata.dto.SubmitEvidenceRequest;
import com.strata.dto.SubmitEvidenceResponse;
import com.strata.model.AcceptanceStatus;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.model.Evidence;
import com.strata.store.InMemoryStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests with analysis fixtures, independent of M2. AI-assisted implementation. */
class EvidenceServiceTest {

  private InMemoryStore store;
  private EvidenceService service;
  private Analysis analysis;

  @BeforeEach
  void setUp() {
    store = new InMemoryStore();
    service = new EvidenceService(store, new ObjectMapper());
    analysis = new Analysis();
    analysis.setAnalysisId("ana-test");
    analysis.setStatus(AnalysisStatus.OPEN);
    store.saveAnalysis(analysis);
  }

  @Test
  void persistsNormalizedEvidenceAndReturnsReceipt() {
    SubmitEvidenceRequest request = request("req-1");
    request.setSourceId(" source-1 ");
    request.setClaim("  Accept\t the\n change  ");
    request.setEvidenceType(" review ");
    Map<String, Object> nested = new HashMap<>(Map.of("url", "https://example.com"));
    request.setMetadata(Map.of("support", nested));

    SubmitEvidenceResponse response = service.submitEvidence("ana-test", request);
    nested.put("url", "changed");

    Evidence saved = store.findEvidenceByAnalysis("ana-test").get(0);
    assertEquals(saved.getEvidenceId(), response.evidenceId());
    assertTrue(response.evidenceId().startsWith("ev-"));
    assertEquals(0.8, response.normalizedConfidenceScore());
    assertEquals(AcceptanceStatus.ACCEPTED, response.acceptanceStatus());
    assertNotNull(response.submittedAt());
    assertEquals(response.submittedAt(), saved.getSubmittedAt());
    assertEquals("req-1", saved.getRequestId());
    assertEquals("source-1", saved.getSourceId());
    assertEquals("Accept the change", saved.getClaim());
    assertEquals("review", saved.getEvidenceType());
    assertEquals(0.8, saved.getConfidenceScore());
    assertEquals(Map.of("support", Map.of("url", "https://example.com")), saved.getMetadata());
    assertEquals(AnalysisStatus.OPEN, analysis.getStatus());
  }

  @ParameterizedTest
  @ValueSource(doubles = {0, -0.0, 1})
  void acceptsConfidenceBoundaries(double score) {
    SubmitEvidenceRequest request = request("req-1");
    request.setConfidenceScore(score);
    assertEquals(score == 0 ? 0 : score,
        service.submitEvidence("ana-test", request).normalizedConfidenceScore());
  }

  @Test
  void retryReturnsOriginalEvenAfterAnalysisCloses() {
    final SubmitEvidenceResponse first = service.submitEvidence("ana-test", request("req-1"));
    analysis.setStatus(AnalysisStatus.FINALIZED);
    assertEquals(first, service.submitEvidence("ana-test", request("req-1")));
    assertEquals(1, store.findEvidenceByAnalysis("ana-test").size());
  }

  @Test
  void normalizedEquivalentRetryReturnsOriginal() {
    final SubmitEvidenceResponse first = service.submitEvidence("ana-test", request("req-1"));
    SubmitEvidenceRequest retry = request("req-1");
    retry.setSourceId(" source-1 ");
    retry.setClaim("Accept\t the change");
    retry.setMetadata(Map.of());
    assertEquals(first, service.submitEvidence("ana-test", retry));
  }

  @ParameterizedTest
  @ValueSource(strings = {"sourceId", "claim", "confidenceScore", "evidenceType", "metadata"})
  void incompatibleRetryIsConflict(String field) {
    service.submitEvidence("ana-test", request("req-1"));
    SubmitEvidenceRequest retry = request("req-1");
    switch (field) {
      case "sourceId" -> retry.setSourceId("different-source");
      case "claim" -> retry.setClaim("Reject the change");
      case "confidenceScore" -> retry.setConfidenceScore(0.9);
      case "evidenceType" -> retry.setEvidenceType("different-type");
      default -> retry.setMetadata(Map.of("new", "value"));
    }
    assertStatus(HttpStatus.CONFLICT, () -> service.submitEvidence("ana-test", retry));
    assertEquals(1, store.findEvidenceByAnalysis("ana-test").size());
  }

  @Test
  void requestIdCannotReplayEvidenceFromAnotherAnalysis() {
    service.submitEvidence("ana-test", request("req-1"));
    assertStatus(HttpStatus.CONFLICT,
        () -> service.submitEvidence("ana-other", request("req-1")));
    assertTrue(store.findEvidenceByAnalysis("ana-other").isEmpty());
  }

  @Test
  void identicalEvidenceWithNewRequestIdIsRejectedWithoutReservingId() {
    service.submitEvidence("ana-test", request("req-1"));
    SubmitEvidenceRequest duplicate = request("req-2");
    assertStatus(HttpStatus.CONFLICT, () -> service.submitEvidence("ana-test", duplicate));
    duplicate.setSourceId("source-2");
    SubmitEvidenceResponse next = service.submitEvidence("ana-test", duplicate);
    assertNotNull(next.evidenceId());
    assertEquals(2, store.findEvidenceByAnalysis("ana-test").size());
  }

  @Test
  void differentEvidenceCreatesDistinctRecords() {
    final SubmitEvidenceResponse first = service.submitEvidence("ana-test", request("req-1"));
    SubmitEvidenceRequest next = request("req-2");
    next.setClaim("Reject the change");
    assertNotEquals(first.evidenceId(), service.submitEvidence("ana-test", next).evidenceId());
  }

  @Test
  void missingAnalysisDoesNotReserveRequestId() {
    assertStatus(HttpStatus.NOT_FOUND,
        () -> service.submitEvidence("missing", request("req-1")));
    assertNotNull(service.submitEvidence("ana-test", request("req-1")).evidenceId());
  }

  @ParameterizedTest
  @EnumSource(value = AnalysisStatus.class, names = {"READY", "FINALIZED"})
  void rejectsNewEvidenceForClosedAnalysis(AnalysisStatus status) {
    analysis.setStatus(status);
    assertStatus(HttpStatus.CONFLICT,
        () -> service.submitEvidence("ana-test", request("req-1")));
    assertTrue(store.findEvidenceByAnalysis("ana-test").isEmpty());
    assertEquals(status, analysis.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {"requestId", "sourceId", "claim", "evidenceType"})
  void rejectsMissingAndBlankRequiredFields(String field) {
    for (String value : new String[] {null, "", " \t\n"}) {
      SubmitEvidenceRequest invalid = request("req-1");
      switch (field) {
        case "requestId" -> invalid.setRequestId(value);
        case "sourceId" -> invalid.setSourceId(value);
        case "claim" -> invalid.setClaim(value);
        default -> invalid.setEvidenceType(value);
      }
      assertStatus(HttpStatus.BAD_REQUEST, () -> service.submitEvidence("ana-test", invalid));
    }
    assertTrue(store.findEvidenceByAnalysis("ana-test").isEmpty());
    assertNotNull(service.submitEvidence("ana-test", request("req-1")).evidenceId());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" \t"})
  void rejectsMissingAnalysisId(String id) {
    assertStatus(HttpStatus.BAD_REQUEST, () -> service.submitEvidence(id, request("req-1")));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY,
      Double.NEGATIVE_INFINITY})
  void rejectsInvalidConfidence(double score) {
    SubmitEvidenceRequest invalid = request("req-1");
    invalid.setConfidenceScore(score);
    assertStatus(HttpStatus.BAD_REQUEST, () -> service.submitEvidence("ana-test", invalid));
    assertTrue(store.findEvidenceByAnalysis("ana-test").isEmpty());
  }

  @Test
  void rejectsMissingBodyAndMissingConfidence() {
    assertStatus(HttpStatus.BAD_REQUEST, () -> service.submitEvidence("ana-test", null));
    SubmitEvidenceRequest invalid = request("req-1");
    invalid.setConfidenceScore(null);
    assertStatus(HttpStatus.BAD_REQUEST, () -> service.submitEvidence("ana-test", invalid));
    assertTrue(store.findEvidenceByAnalysis("ana-test").isEmpty());
  }

  @Test
  void concurrentRetriesCreateOneRecord() throws Exception {
    var executor = Executors.newFixedThreadPool(8);
    try {
      List<Callable<SubmitEvidenceResponse>> tasks = new ArrayList<>();
      for (int i = 0; i < 24; i++) {
        tasks.add(() -> service.submitEvidence("ana-test", request("req-1")));
      }
      var results = executor.invokeAll(tasks, 5, TimeUnit.SECONDS);
      var expected = results.get(0).get();
      for (var result : results) {
        assertFalse(result.isCancelled());
        assertEquals(expected, result.get());
      }
      assertEquals(1, store.findEvidenceByAnalysis("ana-test").size());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentExactDuplicatesWithDifferentIdsCreateOneRecord() throws Exception {
    var executor = Executors.newFixedThreadPool(8);
    try {
      List<Callable<Boolean>> tasks = new ArrayList<>();
      for (int i = 0; i < 24; i++) {
        String requestId = "req-" + i;
        tasks.add(() -> {
          try {
            service.submitEvidence("ana-test", request(requestId));
            return true;
          } catch (ResponseStatusException ex) {
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            return false;
          }
        });
      }
      int accepted = 0;
      for (var result : executor.invokeAll(tasks, 5, TimeUnit.SECONDS)) {
        if (result.get()) {
          accepted++;
        }
      }
      assertEquals(1, accepted);
      assertEquals(1, store.findEvidenceByAnalysis("ana-test").size());
    } finally {
      executor.shutdownNow();
    }
  }

  private static SubmitEvidenceRequest request(String id) {
    SubmitEvidenceRequest request = new SubmitEvidenceRequest();
    request.setRequestId(id);
    request.setSourceId("source-1");
    request.setClaim("Accept the change");
    request.setConfidenceScore(0.8);
    request.setEvidenceType("review");
    return request;
  }

  private static void assertStatus(HttpStatus status, Runnable action) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
    assertEquals(status, ex.getStatusCode());
  }
}
