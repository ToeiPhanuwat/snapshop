package com.snapshop.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Feed look-ups run one at a time on their own thread: each one may read a multi-gigabyte file. */
@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean("feedSyncExecutor")
	Executor feedSyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("feed-sync-");
		return executor;
	}

}
