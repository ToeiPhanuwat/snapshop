package com.snapshop.service.feed;

import com.snapshop.config.FeedProperties;
import com.snapshop.service.feed.FeedResult.Outcome;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reads the feed straight from its link, without saving the multi-gigabyte file. It asks once: a 429 answer is
 * reported with Shopee's Retry-After, never retried here.
 */
@Component
public class FeedClient {

	private final FeedProperties properties;
	private final HttpClient http;

	public FeedClient(FeedProperties properties) {
		this.properties = properties;
		// The feed link answers 302 to where the file really is; NORMAL follows it, but never from https to http.
		this.http = HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	public FeedResult fetch(Set<Long> itemIds) {
		if (!properties.enabled()) {
			return FeedResult.failed(Outcome.FAILED, "ยังไม่ได้ตั้งค่า FEED_URL");
		}
		try {
			HttpResponse<InputStream> response = http.send(request(), HttpResponse.BodyHandlers.ofInputStream());
			try (InputStream body = response.body()) {
				return answer(response, body, itemIds);
			}
		} catch (IOException e) {
			return FeedResult.failed(Outcome.FAILED, "เชื่อมต่อฟีดไม่ได้: " + e.getClass().getSimpleName());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return FeedResult.failed(Outcome.FAILED, "ถูกขัดจังหวะ");
		}
	}

	private HttpRequest request() {
		return HttpRequest.newBuilder(URI.create(properties.url())).timeout(properties.readTimeout()).GET().build();
	}

	private static FeedResult answer(HttpResponse<InputStream> response, InputStream body, Set<Long> itemIds) {
		if (response.statusCode() == 429) {
			String wait = response.headers().firstValue("Retry-After").orElse("ไม่ระบุ");
			return FeedResult.failed(Outcome.RATE_LIMITED, "Shopee ตอบ 429 ให้รอ (Retry-After): " + wait);
		}
		if (response.statusCode() != 200) {
			return FeedResult.failed(Outcome.FAILED, "ฟีดตอบ HTTP " + response.statusCode());
		}
		FeedReader.Result read = FeedReader.read(body, itemIds);
		String found = "เจอ " + read.found().size() + "/" + itemIds.size() + " รายการ";
		return read.finished() ? new FeedResult(read.found(), Outcome.DONE, found)
				: new FeedResult(read.found(), Outcome.INCOMPLETE, "การเชื่อมต่อหลุดกลางทาง " + found);
	}

}
