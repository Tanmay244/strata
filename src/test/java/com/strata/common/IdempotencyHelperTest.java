package com.strata.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class IdempotencyHelperTest {

  @Test
  void sameRequestIdCreatesOnlyOnce() {
    IdempotencyHelper<String> helper = new IdempotencyHelper<>();
    AtomicInteger calls = new AtomicInteger();

    String first = helper.getOrCreate("req-1", () -> "record-" + calls.incrementAndGet());
    String second = helper.getOrCreate("req-1", () -> "record-" + calls.incrementAndGet());

    assertEquals("record-1", first);
    assertEquals(first, second);
    assertEquals(1, calls.get());
  }

  @Test
  void differentRequestIdsCreateSeparateRecords() {
    IdempotencyHelper<String> helper = new IdempotencyHelper<>();

    assertEquals("a", helper.getOrCreate("req-1", () -> "a"));
    assertEquals("b", helper.getOrCreate("req-2", () -> "b"));
  }

  @Test
  void findReturnsEmptyForUnknownRequestId() {
    IdempotencyHelper<String> helper = new IdempotencyHelper<>();

    assertTrue(helper.find("missing").isEmpty());
  }
}
