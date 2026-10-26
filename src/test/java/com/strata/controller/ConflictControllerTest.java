package com.strata.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strata.ConflictFixtures;
import com.strata.store.InMemoryStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises real Spring request binding, service logic, and JSON serialization with fixed data. */
@SpringBootTest
@AutoConfigureMockMvc
class ConflictControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private InMemoryStore store;

  @Autowired
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    store.reset();
    ConflictFixtures.populate(store);
  }

  @Test
  void defaultThresholdProducesExpectedJsonAndRepeatedCallsDoNotModifyRecords() throws Exception {
    String before = snapshot("m4-conflicts");
    String first = mvc.perform(get("/analyses/m4-conflicts/conflicts"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {"conflictingSourcePairs":[["agent-a","agent-b"],["agent-b","agent-c"]],
             "conflictSeverityScores":[0.8,0.8],
             "conflictingClaims":[["APPROVE","REJECT"],["REJECT","approve"]]}
            """, true))
        .andReturn().getResponse().getContentAsString();
    mvc.perform(get("/analyses/m4-conflicts/conflicts").param("conflictThreshold", "0.6"))
        .andExpect(status().isOk())
        .andExpect(content().string(first));

    assertEquals(before, snapshot("m4-conflicts"));
    // Fixture IDs are fixed, so the first generated ID proves detection consumed none.
    assertEquals("check-1", store.newId("check"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.1", "1.1", "NaN", "Infinity", "-Infinity", "abc", "", " "})
  void invalidThresholdReturns400WithoutStateChange(String threshold) throws Exception {
    String before = snapshot("m4-conflicts");

    mvc.perform(get("/analyses/m4-conflicts/conflicts").param("conflictThreshold", threshold))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(
            "conflictThreshold must be a finite number in [0, 1]"));

    assertEquals(before, snapshot("m4-conflicts"));
  }

  @Test
  void missingAnalysisReturns404() throws Exception {
    mvc.perform(get("/analyses/m4-missing/conflicts"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Analysis does not exist"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"m4-empty", "m4-single"})
  void insufficientAcceptedEvidenceReturns409(String analysisId) throws Exception {
    String before = snapshot(analysisId);

    mvc.perform(get("/analyses/{id}/conflicts", analysisId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value(
            "At least two accepted evidence records are required"));

    assertEquals(before, snapshot(analysisId));
  }

  @Test
  void thresholdCanExcludeAllConflicts() throws Exception {
    mvc.perform(get("/analyses/m4-conflicts/conflicts").param("conflictThreshold", "1"))
        .andExpect(status().isOk())
        .andExpect(content().json("""
            {"conflictingSourcePairs":[],"conflictSeverityScores":[],"conflictingClaims":[]}
            """, true));
  }

  @Test
  void readyAnalysisWithCompatibleClaimsReturnsEmptyReportAtZero() throws Exception {
    mvc.perform(get("/analyses/m4-compatible/conflicts").param("conflictThreshold", "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conflictingSourcePairs").isEmpty());
  }

  @Test
  void finalizedAnalysisCanStillBeReadAndOtherAnalysesAreExcluded() throws Exception {
    String before = snapshot("m4-finalized");

    mvc.perform(get("/analyses/m4-finalized/conflicts").param("conflictThreshold", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conflictingSourcePairs.length()").value(1))
        .andExpect(jsonPath("$.conflictSeverityScores[0]").value(1.0));

    assertEquals(before, snapshot("m4-finalized"));
  }

  private String snapshot(String analysisId) throws Exception {
    // Serialize values rather than keep object references: the store returns mutable models,
    // so comparing the same references before/after a call could hide an in-place mutation.
    return mapper.writeValueAsString(List.of(store.findAnalysis(analysisId).orElseThrow(),
        store.findEvidenceByAnalysis(analysisId), store.findConsensusByAnalysis(analysisId),
        store.findDecisionByAnalysis(analysisId)));
  }
}
