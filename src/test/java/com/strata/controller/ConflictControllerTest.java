package com.strata.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.strata.dto.DetectConflictsResponse;
import com.strata.service.ConflictService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Initial HTTP contract test; live service/API behavior is also exercised by Postman. */
@WebMvcTest(ConflictController.class)
class ConflictControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ConflictService service;

  @Test
  void omittedThresholdIsPassedToServiceAndConflictReportIsSerialized() throws Exception {
    // Isolate HTTP binding and serialization; the service tests own the scoring calculation.
    when(service.detectConflicts("ana-1", null)).thenReturn(new DetectConflictsResponse(
        List.of(List.of("agent-a", "agent-b")), List.of(0.8),
        List.of(List.of("APPROVE", "REJECT"))));

    mvc.perform(get("/analyses/ana-1/conflicts"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {"conflictingSourcePairs":[["agent-a","agent-b"]],
             "conflictSeverityScores":[0.8],
             "conflictingClaims":[["APPROVE","REJECT"]]}
            """, true));

    verify(service).detectConflicts("ana-1", null);
  }
}