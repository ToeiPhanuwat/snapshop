package com.snapshop.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** One import at a time: a PostgreSQL advisory lock held until the import transaction ends. */
@Repository
public class ImportLockRepository {

	private static final long IMPORT_LOCK_KEY = 7_400_001L;

	private final JdbcClient jdbc;

	public ImportLockRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void lockForThisTransaction() {
		jdbc.sql("SELECT pg_advisory_xact_lock(?)").param(IMPORT_LOCK_KEY).query().singleRow();
	}

}
