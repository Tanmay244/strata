package com.strata.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strata.common.GlobalExceptionHandler;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.service.EvidenceService;
import com.strata.store.InMemoryStore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP tests using the real M3 service and test analyses. AI-assisted implementation. */
class EvidenceControllerTest {

  private static final String ENDPOINT = "/analyses/ana-test/evidence";
  private final ObjectMapper mapper = new ObjectMapper();
  private InMemoryStore store;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    store = new InMemoryStore();
    Analysis analysis = new Analysis();
    analysis.setAnalysisId("ana-test");
    analysis.setStatus(AnalysisStatus.OPEN);
    store.saveAnalysis(analysis);
    mvc = MockMvcBuilders.standaloneSetup(
        new EvidenceController(new EvidenceService(store, mapper)))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
  }

  @Test
  void returnsReceiptAndReplaysIdenticalJson() throws Exception {
    String body = mapper.writeValueAsString(validBody());
    String receipt = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.evidenceId").isString())
        .andExpect(jsonPath("$.normalizedConfidenceScore").value(0.8))
        .andExpect(jsonPath("$.submittedAt").exists())
        .andExpect(jsonPath("$.acceptanceStatus").value("ACCEPTED"))
        .andReturn().getResponse().getContentAsString();
    String retry = mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertEquals(mapper.readTree(receipt), mapper.readTree(retry));
    assertEquals(1, store.findEvidenceByAnalysis("ana-test").size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"requestId", "sourceId", "claim", "confidenceScore", "evidenceType"})
  void missingFieldsReturn400(String field) throws Exception {
    Map<String, Object> body = validBody();
    body.remove(field);
    mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
    assertTrue(store.findEvidenceByAnalysis("ana-test").isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "null", "{", "[]",
      "{\"confidenceScore\":\"not-a-number\"}", "{\"metadata\":\"not-an-object\"}"})
  void invalidJsonOrBodyReturns400(String body) throws Exception {
    mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    assertTrue(store.findEvidenceByAnalysis("ana-test").isEmpty());
  }

  @Test
  void missingBodyReturns400() throws Exception {
    mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unknownAnalysisReturns404() throws Exception {
    mvc.perform(post("/analyses/missing/evidence").contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(validBody()))).andExpect(status().isNotFound());
  }

  @Test
  void closedAnalysisReturns409() throws Exception {
    store.findAnalysis("ana-test").orElseThrow().setStatus(AnalysisStatus.READY);
    mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(validBody()))).andExpect(status().isConflict());
    assertTrue(store.findEvidenceByAnalysis("ana-test").isEmpty());
  }

  @Test
  void duplicatesAndIncompatibleRetriesReturn409() throws Exception {
    Map<String, Object> body = validBody();
    mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isOk());
    body.put("requestId", "different-request");
    mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isConflict());
    body.put("requestId", "request-1");
    body.put("claim", "Reject the change");
    mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))).andExpect(status().isConflict());
    assertEquals(1, store.findEvidenceByAnalysis("ana-test").size());
  }

  private static Map<String, Object> validBody() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("requestId", "request-1");
    body.put("sourceId", "reviewer-1");
    body.put("claim", "Accept the change");
    body.put("confidenceScore", 0.8);
    body.put("evidenceType", "review");
    return body;
  }
}
