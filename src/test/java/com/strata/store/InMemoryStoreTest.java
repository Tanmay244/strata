package com.strata.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.strata.model.Analysis;
import com.strata.model.Evidence;
import org.junit.jupiter.api.Test;

class InMemoryStoreTest {

  @Test
  void newIdIsUniqueAndPrefixed() {
    InMemoryStore store = new InMemoryStore();

    assertEquals("ana-1", store.newId("ana"));
    assertEquals("ana-2", store.newId("ana"));
  }

  @Test
  void savedAnalysisCanBeFound() {
    InMemoryStore store = new InMemoryStore();
    Analysis analysis = new Analysis();
    analysis.setAnalysisId("ana-1");

    store.saveAnalysis(analysis);

    assertEquals(analysis, store.findAnalysis("ana-1").orElseThrow());
    assertTrue(store.findAnalysis("nope").isEmpty());
  }

  @Test
  void evidenceIsGroupedByAnalysis() {
    InMemoryStore store = new InMemoryStore();
    store.saveEvidence(evidence("ev-1", "ana-1"));
    store.saveEvidence(evidence("ev-2", "ana-1"));
    store.saveEvidence(evidence("ev-3", "ana-2"));

    assertEquals(2, store.findEvidenceByAnalysis("ana-1").size());
    assertEquals(1, store.findEvidenceByAnalysis("ana-2").size());
  }

  @Test
  void resetClearsEverything() {
    InMemoryStore store = new InMemoryStore();
    store.saveEvidence(evidence("ev-1", "ana-1"));

    store.reset();

    assertTrue(store.findEvidenceByAnalysis("ana-1").isEmpty());
    assertEquals("ana-1", store.newId("ana"));
  }

  private static Evidence evidence(String evidenceId, String analysisId) {
    Evidence item = new Evidence();
    item.setEvidenceId(evidenceId);
    item.setAnalysisId(analysisId);
    return item;
  }
}
