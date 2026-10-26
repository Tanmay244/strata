package com.strata.dto;

import com.strata.model.AcceptanceStatus;
import java.time.Instant;

/**
 * The immutable receipt returned for both a new submission and its retries.
 * AI-assisted implementation.
 *
 * @param evidenceId the stored evidence identifier
 * @param normalizedConfidenceScore confidence in the inclusive range [0, 1]
 * @param submittedAt the original submission time
 * @param acceptanceStatus whether the evidence was accepted
 */
public record SubmitEvidenceResponse(
    String evidenceId,
    double normalizedConfidenceScore,
    Instant submittedAt,
    AcceptanceStatus acceptanceStatus) {}
