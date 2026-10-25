package com.strata.common;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Remembers the result of an operation per requestId, so a repeated request returns the original
 * record instead of creating a new one. Use one instance per operation.
 *
 * @param <T> the type of record created by the operation
 */
public class IdempotencyHelper<T> {

  private final ConcurrentHashMap<String, T> results = new ConcurrentHashMap<>();

  /** Returns the record already stored for the requestId, if any. */
  public Optional<T> find(String requestId) {
    return Optional.ofNullable(results.get(requestId));
  }

  /**
   * Returns the record stored for the requestId, creating it with the supplier on first use. The
   * supplier runs at most once per requestId.
   */
  public T getOrCreate(String requestId, Supplier<T> creator) {
    return results.computeIfAbsent(requestId, id -> creator.get());
  }

  /** Removes all remembered requests. */
  public void clear() {
    results.clear();
  }
}
