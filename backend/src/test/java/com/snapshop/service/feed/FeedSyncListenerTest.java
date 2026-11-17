package com.snapshop.service.feed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		when(sync.start(request)).thenReturn(42L);

		new FeedSyncListener(new FeedProperties("https://feed.example/x", null, null), sync, runs).afterImport(request);

		verify(sync).runAsync(42L, request);
	}

	@Test
	void doesNothingWithoutFeedLinkOrWithoutItems() {
		new FeedSyncListener(new FeedProperties("", null, null), sync, runs).afterImport(request);
		new FeedSyncListener(new FeedProperties("https://feed.example/x", null, null), sync, runs)
				.afterImport(new FeedRequest(1L, Set.of(), Set.of()));

		verify(sync, never()).start(any());
		verify(sync, never()).runAsync(anyLong(), any());
	}

	@Test
	void marksRunsThatARestartCutShort() {
		new FeedSyncListener(new FeedProperties("", null, null), sync, runs).markInterruptedRuns();

		verify(runs).failInterrupted(any(Instant.class));
	}

}
