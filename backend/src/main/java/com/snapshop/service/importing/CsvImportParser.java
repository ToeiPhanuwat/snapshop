package com.snapshop.service.importing;

import com.snapshop.exception.ImportValidationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/** Boundary: turns the merged, grouped CSV into checked {@link ImportRow}s, or rejects the whole file. */
@Component
public class CsvImportParser {

	private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.get();

	public List<ImportRow> parse(InputStream in) {
		try (CSVParser csv = FORMAT.parse(reader(in))) {
			requireColumns(csv.getHeaderNames());
			return readRows(csv);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static List<ImportRow> readRows(CSVParser csv) {
		List<String> errors = new ArrayList<>();
		List<ImportRow> rows = new ArrayList<>();
		Map<Long, Integer> lineByItem = new HashMap<>();
		for (CSVRecord record : csv) {
			new RecordReader(record, errors, lineByItem).read().ifPresent(rows::add);
		}
		if (rows.isEmpty() && errors.isEmpty()) {
			errors.add("ไฟล์ไม่มีข้อมูลสินค้า");
		}
		if (!errors.isEmpty()) {
			throw new ImportValidationException(errors);
		}
		return rows;
	}

	private static void requireColumns(List<String> headers) {
		List<String> missing = Columns.ALL.stream().filter(c -> !headers.contains(c)).toList();
		if (!missing.isEmpty()) {
			throw new ImportValidationException(List.of("ไม่พบคอลัมน์: " + String.join(", ", missing)));
		}
	}

	private static Reader reader(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		reader.mark(1);
		if (reader.read() != '﻿') {
			reader.reset();
		}
		return reader;
	}

}
