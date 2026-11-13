package com.snapshop.controller;

import com.snapshop.dto.response.ImportPreviewResponse;
import com.snapshop.dto.response.ImportResultResponse;
import com.snapshop.dto.response.InboxFileResponse;
import com.snapshop.service.InboxService;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Files prepared outside the system (by a person or by AI) wait here for an admin decision. */
@RestController
@RequestMapping("/api/admin/inbox")
@Validated
public class AdminInboxController {

	private final InboxService inbox;

	public AdminInboxController(InboxService inbox) {
		this.inbox = inbox;
	}

	@GetMapping
	public List<InboxFileResponse> list() {
		return inbox.list();
	}

	@GetMapping("/{name}/preview")
	public ImportPreviewResponse preview(@PathVariable String name,
			@RequestParam(defaultValue = "DRPONG") @Pattern(regexp = "[A-Z0-9_]{1,32}") String category) {
		return inbox.preview(category, name);
	}

	@PostMapping("/{name}/import")
	public ImportResultResponse importFile(@PathVariable String name,
			@RequestParam(defaultValue = "DRPONG") @Pattern(regexp = "[A-Z0-9_]{1,32}") String category) {
		return inbox.importFile(category, name);
	}

	@PostMapping("/{name}/discard")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void discard(@PathVariable String name) {
		inbox.discard(name);
	}

}
