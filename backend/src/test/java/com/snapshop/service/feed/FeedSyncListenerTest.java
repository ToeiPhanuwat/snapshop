package com.snapshop.service.feed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.snapshop.config.FeedProperties;
import com.snapshop.repository.FeedSyncRunRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FeedSyncListenerTest {

	final FeedSyncService sync = mock(FeedSyncService.class);
	final FeedSyncRunRepository runs = mock(FeedSyncRunRepository.class);
	final FeedRequest request = new FeedRequest(1L, Set.of(5L), Set.of());

	@Test
	void startsTheLookupAfterAnImportWhenTheFeedIsConfigured() {
		new FeedSyncListener(new FeedProperties("https://feed.example/x", null, null), sync, runs).afterImport(request);

		verify(sync).syncAsync(request);
	}

	@Test
	void doesNothingWithoutFeedLinkOrWithoutItems() {
		new FeedSyncListener(new FeedProperties("", null, null), sync, runs).afterImport(request);
		new FeedSyncListener(new FeedProperties("https://feed.example/x", null, null), sync, runs)
				.afterImport(new FeedRequest(1L, Set.of(), Set.of()));

		verify(sync, never()).syncAsync(any());
	}

	@Test
	void marksRunsThatARestartCutShort() {
		new FeedSyncListener(new FeedProperties("", null, null), sync, runs).markInterruptedRuns();

		verify(runs).failInterrupted(any(Instant.class));
	}

}
