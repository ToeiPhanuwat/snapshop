package com.snapshop.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One import round: one file and what it changed. */
@Entity
@Table(name = "import_batch")
public class ImportBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "batch_id")
	private Long id;

	@Column(name = "category_code", nullable = false)
	private String categoryCode;

	@Column(name = "source_files", nullable = false)
	private String sourceFiles;

	@Column(name = "row_count", nullable = false)
	private int rowCount;

	@Column(name = "inserted_count", nullable = false)
	private int insertedCount;

	@Column(name = "price_changed_count", nullable = false)
	private int priceChangedCount;

	@Column(name = "unchanged_count", nullable = false)
	private int unchangedCount;

	@Column(name = "not_in_latest_count", nullable = false)
	private int notInLatestCount;

	@Column(name = "new_type_count", nullable = false)
	private int newTypeCount;

	@Column(name = "imported_at", nullable = false)
	private Instant importedAt;

	protected ImportBatch() {
	}

	public ImportBatch(String categoryCode, String sourceFiles, Counts counts, Instant importedAt) {
		this.categoryCode = categoryCode;
		this.sourceFiles = sourceFiles;
		this.rowCount = counts.rows();
		this.insertedCount = counts.inserted();
		this.priceChangedCount = counts.priceChanged();
		this.unchangedCount = counts.unchanged();
		this.notInLatestCount = counts.notInLatest();
		this.newTypeCount = counts.newTypes();
		this.importedAt = importedAt;
	}

	public record Counts(int rows, int inserted, int priceChanged, int unchanged, int notInLatest, int newTypes) {
	}

	public Long getId() {
		return id;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public String getSourceFiles() {
		return sourceFiles;
	}

	public Counts getCounts() {
		return new Counts(rowCount, insertedCount, priceChangedCount, unchangedCount, notInLatestCount, newTypeCount);
	}

	public Instant getImportedAt() {
		return importedAt;
	}

}
