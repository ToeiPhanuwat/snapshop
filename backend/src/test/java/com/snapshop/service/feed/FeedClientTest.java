package com.snapshop.service.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.snapshop.config.FeedProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FeedClientTest {

	HttpServer server;

	@AfterEach
	void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void fetchesWantedItemsFromTheFeedUrl() throws IOException {
		serve(200, null, FeedReaderTest.HEADER + "t,11,7,129,79,5,4.5,4.8,Official shop,Non-Preferred shop,https://cf/x\n");

		FeedResult result = client().fetch(Set.of(11L));

		assertThat(result.outcome()).isEqualTo(FeedResult.Outcome.DONE);
		assertThat(result.found()).containsOnlyKeys(11L);
	}

	@Test
	void saysHowManyItemsWereFoundWhenTheFeedEndsEarly() throws IOException {
		serve(200, null, FeedReaderTest.HEADER + "t,11,7,1,1,1,1,1,a,b,c\n");

		FeedResult result = client().fetch(Set.of(11L, 99L));

		assertThat(result.outcome()).isEqualTo(FeedResult.Outcome.DONE);
		assertThat(result.found()).containsOnlyKeys(11L);
		assertThat(result.message()).contains("1/2");
	}

	@Test
	void reportsRateLimitWithRetryAfter() throws IOException {
		serve(429, "1800", "");

		FeedResult result = client().fetch(Set.of(11L));

		assertThat(result.outcome()).isEqualTo(FeedResult.Outcome.RATE_LIMITED);
		assertThat(result.message()).contains("1800");
		assertThat(result.found()).isEmpty();
	}

	@Test
	void reportsFailureForOtherStatusAndForNoUrl() throws IOException {
		serve(500, null, "");

		assertThat(client().fetch(Set.of(11L)).outcome()).isEqualTo(FeedResult.Outcome.FAILED);
		assertThat(new FeedClient(new FeedProperties("", null, null)).fetch(Set.of(11L)).outcome())
				.isEqualTo(FeedResult.Outcome.FAILED);
	}

	@Test
	void reportsFailureWhenTheFeedCannotBeReached() throws IOException {
		serve(200, null, "");
		FeedClient unreachable = client();
		server.stop(0);

		FeedResult result = unreachable.fetch(Set.of(11L));

		assertThat(result.outcome()).isEqualTo(FeedResult.Outcome.FAILED);
		assertThat(result.message()).contains("เชื่อมต่อฟีดไม่ได้");
	}

	@Test
	void keepsTheInterruptFlagWhenTheThreadIsInterrupted() throws IOException {
		serve(200, null, FeedReaderTest.HEADER);
		Thread.currentThread().interrupt();
		try {
			FeedResult result = client().fetch(Set.of(11L));

			assertThat(result.outcome()).isEqualTo(FeedResult.Outcome.FAILED);
			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		} finally {
			Thread.interrupted();
		}
	}

	private FeedClient client() {
		return new FeedClient(new FeedProperties("http://localhost:" + server.getAddress().getPort() + "/feed",
				Duration.ofSeconds(2), Duration.ofSeconds(5)));
	}

	private void serve(int status, String retryAfter, String body) throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/feed", exchange -> {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			if (retryAfter != null) {
				exchange.getResponseHeaders().add("Retry-After", retryAfter);
			}
			exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
			if (bytes.length > 0) {
				exchange.getResponseBody().write(bytes);
			}
			exchange.close();
		});
		server.start();
	}

}
