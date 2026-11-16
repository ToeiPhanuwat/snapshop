package com.snapshop.repository;

import com.snapshop.dto.response.FeedSyncRunResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Log of feed look-ups (table feed_sync_run). Each call commits on its own, so a crash leaves RUNNING visible. */
@Repository
public class FeedSyncRunRepository {

	private final JdbcClient jdbc;

	public FeedSyncRunRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public long start(Long batchId, int requestedCount, Instant now) {
		GeneratedKeyHolder keys = new GeneratedKeyHolder();
		jdbc.sql("""
				INSERT INTO feed_sync_run (import_batch_id, started_at, requested_count, outcome)
				VALUES (:batch, :now, :requested, 'RUNNING')""")
				.param("batch", batchId).param("now", Timestamp.from(now)).param("requested", requestedCount)
				.update(keys, "run_id");
		return keys.getKey().longValue();
	}

	public void finish(long runId, int foundCount, String outcome, String message, Instant now) {
		jdbc.sql("""
				UPDATE feed_sync_run SET finished_at = :now, found_count = :found, outcome = :outcome, message = :message
				WHERE run_id = :id""")
				.param("now", Timestamp.from(now)).param("found", foundCount).param("outcome", outcome)
				.param("message", message).param("id", runId).update();
	}

	public boolean hasRunning() {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM feed_sync_run WHERE outcome = 'RUNNING')")
				.query(Boolean.class).single();
	}

	/** After a restart nothing can still be running, so a RUNNING row means the app stopped halfway. */
	public void failInterrupted(Instant now) {
		jdbc.sql("""
				UPDATE feed_sync_run SET finished_at = :now, outcome = 'FAILED', message = 'แอปหยุดกลางทาง'
				WHERE outcome = 'RUNNING'""").param("now", Timestamp.from(now)).update();
	}

	public FeedSyncRunResponse find(long runId) {
		return jdbc.sql(SELECT + " WHERE run_id = :id").param("id", runId).query(FeedSyncRunRepository::row).single();
	}

	public List<FeedSyncRunResponse> recent() {
		return jdbc.sql(SELECT + " ORDER BY run_id DESC LIMIT 20").query(FeedSyncRunRepository::row).list();
	}

	private static final String SELECT = """
			SELECT run_id, import_batch_id, started_at, finished_at, requested_count, found_count, outcome, message
			FROM feed_sync_run""";

	private static FeedSyncRunResponse row(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
		Timestamp finished = rs.getTimestamp("finished_at");
		long batch = rs.getLong("import_batch_id");
		return new FeedSyncRunResponse(rs.getLong("run_id"), rs.wasNull() ? null : batch,
				rs.getTimestamp("started_at").toInstant(), finished == null ? null : finished.toInstant(),
				rs.getInt("requested_count"), rs.getInt("found_count"), rs.getString("outcome"),
				rs.getString("message"));
	}

}
