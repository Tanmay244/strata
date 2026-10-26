package com.strata.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.strata.dto.ComputeConsensusResponse;
import com.strata.service.ConsensusService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class ConsensusControllerTest {

  @Test
  void returnsConsensusResponse() throws Exception {
    StubConsensusService service = new StubConsensusService();
    service.response = new ComputeConsensusResponse("con-1", "Approve", 0.75, 2);

    mockMvc(service).perform(post("/analyses/ana-1/consensus"))
        .andExpect(status().isOk())
        .andExpect(content().json("""
            {
              "consensusResultId": "con-1",
              "consensusClaim": "Approve",
              "consensusConfidenceScore": 0.75,
              "evidenceCount": 2
            }
            """));
  }

  @Test
  void mapsServiceErrorsToHttpStatus() throws Exception {
    StubConsensusService service = new StubConsensusService();
    service.failure = new ResponseStatusException(
        org.springframework.http.HttpStatus.NOT_FOUND, "Analysis does not exist");

    mockMvc(service).perform(post("/analyses/missing/consensus"))
        .andExpect(status().isNotFound());
  }

  @Test
  void exposesJsonContentType() throws Exception {
    StubConsensusService service = new StubConsensusService();
    service.response = new ComputeConsensusResponse("con-1", "Approve", 1.0, 1);

    mockMvc(service).perform(post("/analyses/ana-1/consensus")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  private static MockMvc mockMvc(ConsensusService service) {
    return MockMvcBuilders.standaloneSetup(new ConsensusController(service)).build();
  }

  private static final class StubConsensusService extends ConsensusService {

    private ComputeConsensusResponse response;
    private ResponseStatusException failure;

    private StubConsensusService() {
      super(null);
    }

    @Override
    public synchronized ComputeConsensusResponse computeConsensus(String analysisId) {
      if (failure != null) {
        throw failure;
      }
      return response;
    }
  }
}
