package com.snapshop.controller;

import com.snapshop.dto.response.ImportResultResponse;
import com.snapshop.repository.ImportBatchRepository;
import com.snapshop.service.ImportService;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/imports")
@Validated
public class AdminImportController {

	private final ImportService importService;
	private final ImportBatchRepository batches;

	public AdminImportController(ImportService importService, ImportBatchRepository batches) {
		this.importService = importService;
		this.batches = batches;
	}

	@PostMapping
	public ImportResultResponse importFile(@RequestPart("file") MultipartFile file,
			@RequestParam(defaultValue = "DRPONG") @Pattern(regexp = "[A-Z0-9_]{1,32}") String category)
			throws IOException {
		try (InputStream in = file.getInputStream()) {
			return importService.importFile(category, file.getOriginalFilename(), in);
		}
	}

	@GetMapping
	public List<ImportResultResponse> recentImports() {
		return batches.findTop20ByOrderByIdDesc().stream().map(ImportResultResponse::from).toList();
	}

}
