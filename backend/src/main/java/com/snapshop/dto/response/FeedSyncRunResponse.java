package com.snapshop.dto.response;

import java.time.Instant;

/** One look-up in the feed. {@code outcome} is RUNNING, DONE, INCOMPLETE, RATE_LIMITED or FAILED. */
public record FeedSyncRunResponse(
		long runId,
		Long importBatchId,
		Instant startedAt,
		Instant finishedAt,
		int requestedCount,
		int foundCount,
		String outcome,
		String message) {
}
