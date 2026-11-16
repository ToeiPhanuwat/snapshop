package com.snapshop.service.feed;

import java.util.Map;

/**
 * What one look-up in the feed brought back.
 *
 * @param found   the wanted items the feed has, by item id
 * @param message what an admin should know: how many were found, or why the look-up failed
 */
public record FeedResult(Map<Long, FeedRecord> found, Outcome outcome, String message) {

	/** Same names as the {@code outcome} column of feed_sync_run (RUNNING is only ever stored). */
	public enum Outcome {
		/** Every wanted item was found, or the whole feed was read. */
		DONE,
		/** The connection broke part-way; the items found so far are kept. */
		INCOMPLETE,
		/** Shopee answered 429 Too Many Requests. */
		RATE_LIMITED,
		FAILED
	}

	public FeedResult {
		found = Map.copyOf(found);
	}

	static FeedResult failed(Outcome outcome, String message) {
		return new FeedResult(Map.of(), outcome, message);
	}

}
