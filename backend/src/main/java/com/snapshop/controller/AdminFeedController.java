package com.snapshop.controller;

import com.snapshop.config.FeedProperties;
import com.snapshop.dto.response.FeedSyncRunResponse;
import com.snapshop.exception.BusinessRuleException;
import com.snapshop.repository.FeedSyncRunRepository;
import com.snapshop.service.feed.FeedRequest;
import com.snapshop.service.feed.FeedSyncService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feed-syncs")
public class AdminFeedController {

	private final FeedProperties properties;
	private final FeedSyncService sync;
	private final FeedSyncRunRepository runs;

	public AdminFeedController(FeedProperties properties, FeedSyncService sync, FeedSyncRunRepository runs) {
		this.properties = properties;
		this.sync = sync;
		this.runs = runs;
	}

	@GetMapping
	public List<FeedSyncRunResponse> recent() {
		return runs.recent();
	}

	/** Looks up again the items still waiting for a price or never looked up; the answer comes later in the list. */
	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void retry() {
		if (!properties.enabled()) {
			throw new BusinessRuleException("ยังไม่ได้ตั้งค่า FEED_URL");
		}
		FeedRequest request = sync.pendingRequest();
		sync.syncAsync(request);
	}

}
