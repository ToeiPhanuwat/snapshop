package com.snapshop.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Opens a UTF-8 CSV stream, skipping the byte order mark that spreadsheet programs and feeds put in front. */
public final class CsvReaders {

	private CsvReaders() {
	}

	public static Reader utf8(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 16);
		reader.mark(1);
		if (reader.read() != '﻿') {
			reader.reset();
		}
		return reader;
	}

}
