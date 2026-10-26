package com.strata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strata.common.ConflictException;
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
import java.util.List;
import java.util.Optional;
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
class DecisionServiceTest {

  private static final String ANALYSIS_ID = "ana-1";
  private static final String CONSENSUS_ID = "con-1";

  @Mock private InMemoryStore store;

  private DecisionService service;
  private Analysis analysis;

  @BeforeEach
  void setUp() {
    service = new DecisionService(store);
    analysis = new Analysis();
    analysis.setAnalysisId(ANALYSIS_ID);
    analysis.setStatus(AnalysisStatus.READY);

    ConsensusResult consensus = new ConsensusResult();
    consensus.setConsensusResultId(CONSENSUS_ID);
    consensus.setAnalysisId(ANALYSIS_ID);
    consensus.setConsensusClaim("adopt option A");
    consensus.setConsensusConfidenceScore(0.8);

    when(store.findAnalysis(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
    when(store.findConsensusByAnalysis(ANALYSIS_ID)).thenReturn(Optional.of(consensus));
    when(store.findEvidenceByAnalysis(ANALYSIS_ID))
        .thenReturn(
            List.of(
                evidence("ev-1", AcceptanceStatus.ACCEPTED),
                evidence("ev-2", AcceptanceStatus.REJECTED),
                evidence("ev-3", AcceptanceStatus.ACCEPTED)));
    when(store.newId("dec")).thenReturn("dec-1", "dec-2");
  }

  private static Evidence evidence(String id, AcceptanceStatus status) {
    Evidence item = new Evidence();
    item.setEvidenceId(id);
    item.setAnalysisId(ANALYSIS_ID);
    item.setAcceptanceStatus(status);
    return item;
  }

  private static FinalizeDecisionRequest request(
      String requestId, String consensusId, Double threshold) {
    FinalizeDecisionRequest request = new FinalizeDecisionRequest();
    request.setRequestId(requestId);
    request.setConsensusResultId(consensusId);
    request.setMinimumConfidenceThreshold(threshold);
    return request;
  }

  @Test
  void finalizesDecisionFromConsensus() {
    FinalizeDecisionResponse response =
        service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, 0.5));

    assertEquals("dec-1", response.getDecisionId());
    assertEquals("adopt option A", response.getFinalDecision());
    assertEquals(0.8, response.getConfidenceScore());
    assertEquals(AnalysisStatus.FINALIZED, response.getAnalysisStatus());
    assertEquals(List.of("ev-1", "ev-3"), response.getSupportingEvidenceIds());

    ArgumentCaptor<FinalDecision> saved = ArgumentCaptor.forClass(FinalDecision.class);
    verify(store).saveDecision(saved.capture());
    assertEquals(ANALYSIS_ID, saved.getValue().getAnalysisId());
    assertEquals(CONSENSUS_ID, saved.getValue().getConsensusResultId());
    assertEquals(AnalysisStatus.FINALIZED, analysis.getStatus());
    verify(store).saveAnalysis(analysis);
  }

  @Test
  void skipsThresholdCheckWhenNotSupplied() {
    FinalizeDecisionResponse response =
        service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, null));

    assertEquals(AnalysisStatus.FINALIZED, response.getAnalysisStatus());
  }

  @Test
  void acceptsScoreExactlyAtThreshold() {
    FinalizeDecisionResponse response =
        service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, 0.8));

    assertEquals("dec-1", response.getDecisionId());
  }

  @Test
  void repeatedRequestIdReturnsOriginalDecision() {
    FinalizeDecisionResponse first =
        service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, null));
    FinalizeDecisionResponse second =
        service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, null));

    assertEquals(first.getDecisionId(), second.getDecisionId());
    assertEquals(AnalysisStatus.FINALIZED, second.getAnalysisStatus());
    verify(store, times(1)).saveDecision(any(FinalDecision.class));
  }

  @Test
  void rejectsNullBody() {
    assertThrows(InvalidRequestException.class, () -> service.finalizeDecision(ANALYSIS_ID, null));
  }

  @Test
  void rejectsMissingRequestId() {
    assertThrows(
        InvalidRequestException.class,
        () -> service.finalizeDecision(ANALYSIS_ID, request(null, CONSENSUS_ID, null)));
  }

  @Test
  void rejectsMissingConsensusResultId() {
    assertThrows(
        InvalidRequestException.class,
        () -> service.finalizeDecision(ANALYSIS_ID, request("req-1", " ", null)));
  }

  @Test
  void rejectsThresholdOutOfRange() {
    assertThrows(
        InvalidRequestException.class,
        () -> service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, 1.5)));
    assertThrows(
        InvalidRequestException.class,
        () -> service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, -0.1)));
  }

  @Test
  void rejectsUnknownAnalysis() {
    when(store.findAnalysis("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.finalizeDecision("missing", request("req-1", CONSENSUS_ID, null)));
  }

  @Test
  void rejectsAlreadyFinalizedAnalysis() {
    analysis.setStatus(AnalysisStatus.FINALIZED);

    assertThrows(
        ConflictException.class,
        () -> service.finalizeDecision(ANALYSIS_ID, request("req-2", CONSENSUS_ID, null)));
    verify(store, never()).saveDecision(any(FinalDecision.class));
  }

  @Test
  void rejectsWhenNoConsensusExists() {
    when(store.findConsensusByAnalysis(ANALYSIS_ID)).thenReturn(Optional.empty());

    assertThrows(
        UnprocessableException.class,
        () -> service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, null)));
    verify(store, never()).saveDecision(any(FinalDecision.class));
  }

  @Test
  void rejectsMismatchedConsensusResultId() {
    UnprocessableException ex =
        assertThrows(
            UnprocessableException.class,
            () -> service.finalizeDecision(ANALYSIS_ID, request("req-1", "con-other", null)));

    assertTrue(ex.getMessage().contains("does not match"));
    verify(store, never()).saveDecision(any(FinalDecision.class));
  }

  @Test
  void rejectsConfidenceBelowThreshold() {
    assertThrows(
        UnprocessableException.class,
        () -> service.finalizeDecision(ANALYSIS_ID, request("req-1", CONSENSUS_ID, 0.9)));
    assertEquals(AnalysisStatus.READY, analysis.getStatus());
    verify(store, never()).saveDecision(any(FinalDecision.class));
  }
}
