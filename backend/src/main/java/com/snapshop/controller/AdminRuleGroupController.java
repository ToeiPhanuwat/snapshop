package com.snapshop.controller;

import com.snapshop.dto.request.ConfirmRuleGroupRequest;
import com.snapshop.dto.response.RuleGroupReviewResponse;
import com.snapshop.service.RuleGroupReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Review of rule groups (กลุ่ม R) against name groups (กลุ่ม E). */
@RestController
@RequestMapping("/api/admin/rule-groups")
public class AdminRuleGroupController {

	private final RuleGroupReviewService service;

	public AdminRuleGroupController(RuleGroupReviewService service) {
		this.service = service;
	}

	@GetMapping
	public List<RuleGroupReviewResponse> pending() {
		return service.pending();
	}

	@PostMapping("/{code}/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirm(@PathVariable String code, @Valid @RequestBody ConfirmRuleGroupRequest request) {
		service.confirm(code, request.mergeTypeCodes());
	}

}
