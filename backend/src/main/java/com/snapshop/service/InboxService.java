package com.snapshop.service;

import com.snapshop.config.ImportProperties;
import com.snapshop.dto.response.ImportPreviewResponse;
import com.snapshop.dto.response.ImportResultResponse;
import com.snapshop.dto.response.InboxFileResponse;
import com.snapshop.exception.InvalidRequestException;
import com.snapshop.exception.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Prepared CSV files wait in the inbox folder until an admin previews and imports or discards them.
 * Files are moved to {@code imported/} or {@code rejected/} so each one is handled once.
 */
@Service
public class InboxService {

	private static final Pattern FILE_NAME = Pattern.compile("[^/\\\\:*?\"<>|]{1,200}\\.csv", Pattern.CASE_INSENSITIVE);
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

	private final Path inbox;
	private final ImportService importService;

	public InboxService(ImportProperties properties, ImportService importService) {
		this.inbox = properties.inboxDir().toAbsolutePath().normalize();
		this.importService = importService;
	}

	public List<InboxFileResponse> list() {
		try (Stream<Path> files = Files.list(createdInbox())) {
			return files.filter(Files::isRegularFile)
					.filter(p -> FILE_NAME.matcher(p.getFileName().toString()).matches())
					.map(InboxService::describe)
					.sorted(Comparator.comparing(InboxFileResponse::modifiedAt).reversed())
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public ImportPreviewResponse preview(String category, String name) {
		Path file = existing(name);
		try (InputStream in = Files.newInputStream(file)) {
			return importService.preview(category, name, in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Imports in one transaction; only after it committed is the file moved out of the inbox. */
	public ImportResultResponse importFile(String category, String name) {
		Path file = existing(name);
		ImportResultResponse result;
		try (InputStream in = Files.newInputStream(file)) {
			result = importService.importFile(category, name, in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		moveTo("imported", file);
		return result;
	}

	public void discard(String name) {
		moveTo("rejected", existing(name));
	}

	/**
	 * Boundary check: only a plain .csv file name is accepted. The name cannot hold a path separator, drive
	 * colon or "..", so it always resolves to a file directly inside the inbox.
	 */
	private Path existing(String name) {
		if (!FILE_NAME.matcher(name).matches() || name.contains("..")) {
			throw new InvalidRequestException("ชื่อไฟล์ไม่ถูกต้อง: " + name);
		}
		Path file = inbox.resolve(name);
		if (!Files.isRegularFile(file)) {
			throw new NotFoundException("ไม่พบไฟล์ " + name + " ใน inbox");
		}
		return file;
	}

	private void moveTo(String folder, Path file) {
		try {
			Path target = Files.createDirectories(inbox.resolve(folder))
					.resolve(ZonedDateTime.now(BANGKOK).format(STAMP) + "-" + file.getFileName());
			Files.move(file, target);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Path createdInbox() throws IOException {
		return Files.createDirectories(inbox);
	}

	private static InboxFileResponse describe(Path file) {
		try {
			return new InboxFileResponse(file.getFileName().toString(), Files.size(file),
					Files.getLastModifiedTime(file).toInstant());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
