package com.snapshop.dto.response;

import com.snapshop.model.entity.ImportBatch;
import java.time.Instant;

public record ImportResultResponse(
		long batchId,
		String categoryCode,
		String sourceFiles,
		int rowCount,
		int insertedCount,
		int priceChangedCount,
		int unchangedCount,
		int notInLatestCount,
		int newTypeCount,
		Instant importedAt) {

	public static ImportResultResponse from(ImportBatch b) {
		ImportBatch.Counts c = b.getCounts();
		return new ImportResultResponse(b.getId(), b.getCategoryCode(), b.getSourceFiles(), c.rows(), c.inserted(),
				c.priceChanged(), c.unchanged(), c.notInLatest(), c.newTypes(), b.getImportedAt());
	}

}
