package com.snapshop.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where to read the Shopee product feed. The link is a secret: it comes from the {@code FEED_URL} environment
 * variable and is never stored in the repository. Empty means the feed is switched off.
 *
 * @param connectTimeout time to open the connection
 * @param readTimeout    longest silence on an open connection
 */
@ConfigurationProperties("snapshop.feed")
public record FeedProperties(String url, Duration connectTimeout, Duration readTimeout) {

	public FeedProperties {
		url = url == null ? "" : url.strip();
		connectTimeout = connectTimeout == null ? Duration.ofSeconds(15) : connectTimeout;
		readTimeout = readTimeout == null ? Duration.ofMinutes(5) : readTimeout;
	}

	public boolean enabled() {
		return !url.isBlank();
	}

}
