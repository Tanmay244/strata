package com.strata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strata.common.InvalidRequestException;
import com.strata.dto.CreateAnalysisRequest;
import com.strata.dto.CreateAnalysisResponse;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.store.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalysisServiceTest {

  @Mock private InMemoryStore store;

  private AnalysisService service;

  @BeforeEach
  void setUp() {
    service = new AnalysisService(store);
    when(store.newId("ana")).thenReturn("ana-1", "ana-2");
    when(store.saveAnalysis(any(Analysis.class))).thenAnswer(call -> call.getArgument(0));
  }

  private static CreateAnalysisRequest request(String requestId, String topic, Integer min) {
    CreateAnalysisRequest request = new CreateAnalysisRequest();
    request.setRequestId(requestId);
    request.setTopic(topic);
    request.setDescription("desc");
    request.setMinimumEvidenceCount(min);
    return request;
  }

  @Test
  void createsOpenAnalysis() {
    CreateAnalysisResponse response = service.createAnalysis(request("req-1", "topic", 3));

    assertEquals("ana-1", response.getAnalysisId());
    assertEquals(AnalysisStatus.OPEN, response.getStatus());
    assertEquals(3, response.getMinimumEvidenceCount());
    assertNotNull(response.getCreatedAt());
    assertFalse(response.isReplayed());

    ArgumentCaptor<Analysis> saved = ArgumentCaptor.forClass(Analysis.class);
    verify(store).saveAnalysis(saved.capture());
    assertEquals("topic", saved.getValue().getTopic());
    assertEquals("desc", saved.getValue().getDescription());
    assertEquals("req-1", saved.getValue().getRequestId());
  }

  @Test
  void defaultsMinimumEvidenceCountToOne() {
    CreateAnalysisResponse response = service.createAnalysis(request("req-1", "topic", null));

    assertEquals(1, response.getMinimumEvidenceCount());
  }

  @Test
  void acceptsZeroMinimumEvidenceCount() {
    CreateAnalysisResponse response = service.createAnalysis(request("req-1", "topic", 0));

    assertEquals(0, response.getMinimumEvidenceCount());
  }

  @Test
  void repeatedRequestIdReturnsOriginalWithoutCreating() {
    CreateAnalysisResponse first = service.createAnalysis(request("req-1", "topic", 2));
    CreateAnalysisResponse second = service.createAnalysis(request("req-1", "other", 5));

    assertEquals(first.getAnalysisId(), second.getAnalysisId());
    assertEquals(first.getCreatedAt(), second.getCreatedAt());
    assertEquals(2, second.getMinimumEvidenceCount());
    assertTrue(second.isReplayed());
    verify(store, times(1)).saveAnalysis(any(Analysis.class));
  }

  @Test
  void differentRequestIdsCreateSeparateAnalyses() {
    CreateAnalysisResponse first = service.createAnalysis(request("req-1", "topic", 1));
    CreateAnalysisResponse second = service.createAnalysis(request("req-2", "topic", 1));

    assertEquals("ana-1", first.getAnalysisId());
    assertEquals("ana-2", second.getAnalysisId());
  }

  @Test
  void rejectsNullBody() {
    assertThrows(InvalidRequestException.class, () -> service.createAnalysis(null));
  }

  @Test
  void rejectsMissingRequestId() {
    assertThrows(
        InvalidRequestException.class, () -> service.createAnalysis(request(null, "topic", 1)));
    assertThrows(
        InvalidRequestException.class, () -> service.createAnalysis(request(" ", "topic", 1)));
  }

  @Test
  void rejectsMissingTopic() {
    assertThrows(
        InvalidRequestException.class, () -> service.createAnalysis(request("req-1", null, 1)));
    assertThrows(
        InvalidRequestException.class, () -> service.createAnalysis(request("req-1", "", 1)));
  }

  @Test
  void rejectsNegativeMinimumEvidenceCount() {
    assertThrows(
        InvalidRequestException.class, () -> service.createAnalysis(request("req-1", "t", -1)));
    verify(store, never()).saveAnalysis(any(Analysis.class));
  }

  @Test
  void invalidRequestDoesNotConsumeRequestId() {
    assertThrows(
        InvalidRequestException.class, () -> service.createAnalysis(request("req-1", null, 1)));

    CreateAnalysisResponse response = service.createAnalysis(request("req-1", "topic", 1));

    assertFalse(response.isReplayed());
  }
}
