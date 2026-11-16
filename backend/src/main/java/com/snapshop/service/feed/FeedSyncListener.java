package com.snapshop.service.feed;

import com.snapshop.config.FeedProperties;
import com.snapshop.repository.FeedSyncRunRepository;
import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Starts the feed look-up once an import has committed, and cleans up runs that a restart cut short. */
@Component
class FeedSyncListener {

	private final FeedProperties properties;
	private final FeedSyncService sync;
	private final FeedSyncRunRepository runs;

	FeedSyncListener(FeedProperties properties, FeedSyncService sync, FeedSyncRunRepository runs) {
		this.properties = properties;
		this.sync = sync;
		this.runs = runs;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void afterImport(FeedRequest request) {
		if (properties.enabled() && !request.itemIds().isEmpty()) {
			sync.syncAsync(request);
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	void markInterruptedRuns() {
		runs.failInterrupted(Instant.now());
	}

}
