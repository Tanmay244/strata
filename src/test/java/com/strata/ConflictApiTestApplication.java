package com.strata;

import com.strata.store.InMemoryStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Local Postman server; this class and its fixtures are excluded from the production JAR. */
public class ConflictApiTestApplication {

  /** Starts the normal API on loopback port 8081 with test evidence already loaded. */
  public static void main(String[] args) {
    // Run the real controllers/services with seed data, so Postman can test M4 before M2/M3 land.
    SpringApplication application = new SpringApplication(
        StrataApplication.class, FixtureConfiguration.class);
    application.setDefaultProperties(java.util.Map.of(
        "server.port", "8081", "server.address", "127.0.0.1"));
    application.run(args);
  }

  @TestConfiguration
  static class FixtureConfiguration {

    @Bean
    ApplicationRunner conflictFixtures(InMemoryStore store) {
      // Explicitly registered above; ordinary application startup does not load this configuration.
      return args -> ConflictFixtures.populate(store);
    }
  }
}
