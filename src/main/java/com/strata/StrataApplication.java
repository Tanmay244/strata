package com.strata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the Strata service. */
@SpringBootApplication
public class StrataApplication {

  /** Starts the application. */
  public static void main(String[] args) {
    SpringApplication.run(StrataApplication.class, args);
  }
}
