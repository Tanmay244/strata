package com.strata.fixture;

import com.strata.StrataApplication;
import com.strata.model.Analysis;
import com.strata.model.AnalysisStatus;
import com.strata.store.InMemoryStore;
import org.springframework.boot.SpringApplication;

/**
 * Test-only launcher for Postman while Create Analysis is a stub. Never packaged in the app jar.
 * AI-assisted implementation.
 */
public final class M3ApiTestApplication {

  private M3ApiTestApplication() {}

  /** Starts the real HTTP service, then seeds analyses for the API collection. */
  public static void main(String[] args) {
    var context = SpringApplication.run(StrataApplication.class, args);
    InMemoryStore store = context.getBean(InMemoryStore.class);
    for (AnalysisStatus status : AnalysisStatus.values()) {
      Analysis analysis = new Analysis();
      analysis.setAnalysisId("m3-" + status.name().toLowerCase(java.util.Locale.ROOT));
      analysis.setStatus(status);
      analysis.setTopic("M3 API test fixture");
      store.saveAnalysis(analysis);
    }
    System.out.println("M3 fixtures ready: m3-open, m3-ready, m3-finalized");
  }
}
