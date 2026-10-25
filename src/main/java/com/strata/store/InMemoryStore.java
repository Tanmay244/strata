package com.strata.store;

import com.strata.model.Analysis;
import com.strata.model.ConsensusResult;
import com.strata.model.Evidence;
import com.strata.model.FinalDecision;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** In-memory datastore shared by all operations. Data is lost when the application stops. */
@Component
public class InMemoryStore {

  private final Map<String, Analysis> analyses = new ConcurrentHashMap<>();
  private final Map<String, Evidence> evidence = new ConcurrentHashMap<>();
  private final Map<String, ConsensusResult> consensusResults = new ConcurrentHashMap<>();
  private final Map<String, FinalDecision> decisions = new ConcurrentHashMap<>();
  private final AtomicLong idCounter = new AtomicLong();

  /** Returns a new unique id such as {@code ana-1}. */
  public String newId(String prefix) {
    return prefix + "-" + idCounter.incrementAndGet();
  }

  /** Saves or replaces an analysis. */
  public Analysis saveAnalysis(Analysis analysis) {
    analyses.put(analysis.getAnalysisId(), analysis);
    return analysis;
  }

  /** Finds an analysis by id. */
  public Optional<Analysis> findAnalysis(String analysisId) {
    return Optional.ofNullable(analyses.get(analysisId));
  }

  /** Saves or replaces a piece of evidence. */
  public Evidence saveEvidence(Evidence item) {
    evidence.put(item.getEvidenceId(), item);
    return item;
  }

  /** Returns all evidence submitted to an analysis. */
  public List<Evidence> findEvidenceByAnalysis(String analysisId) {
    List<Evidence> matches = new ArrayList<>();
    for (Evidence item : evidence.values()) {
      if (analysisId.equals(item.getAnalysisId())) {
        matches.add(item);
      }
    }
    return matches;
  }

  /** Saves or replaces a consensus result. */
  public ConsensusResult saveConsensus(ConsensusResult result) {
    consensusResults.put(result.getAnalysisId(), result);
    return result;
  }

  /** Finds the consensus result computed for an analysis. */
  public Optional<ConsensusResult> findConsensusByAnalysis(String analysisId) {
    return Optional.ofNullable(consensusResults.get(analysisId));
  }

  /** Saves or replaces a final decision. */
  public FinalDecision saveDecision(FinalDecision decision) {
    decisions.put(decision.getAnalysisId(), decision);
    return decision;
  }

  /** Finds the final decision recorded for an analysis. */
  public Optional<FinalDecision> findDecisionByAnalysis(String analysisId) {
    return Optional.ofNullable(decisions.get(analysisId));
  }

  /** Removes all data. Intended for tests. */
  public void reset() {
    analyses.clear();
    evidence.clear();
    consensusResults.clear();
    decisions.clear();
    idCounter.set(0);
  }
}
