package com.snapshop.controller;

import com.snapshop.dto.request.AdminProductSearchRequest;
import com.snapshop.dto.request.MoveProductRequest;
import com.snapshop.dto.request.TypeSearchRequest;
import com.snapshop.dto.response.AdminProductResponse;
import com.snapshop.dto.response.AdminProductTypeResponse;
import com.snapshop.dto.response.PageResponse;
import com.snapshop.service.AdminProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

	private final AdminProductService service;

	public AdminProductController(AdminProductService service) {
		this.service = service;
	}

	@GetMapping("/products")
	public PageResponse<AdminProductResponse> products(@Valid @ModelAttribute AdminProductSearchRequest request) {
		return service.products(request);
	}

	@GetMapping("/product-types")
	public PageResponse<AdminProductTypeResponse> types(@Valid @ModelAttribute TypeSearchRequest request) {
		return service.types(request);
	}

	@PostMapping("/products/{itemId}/resolve")
	public AdminProductResponse resolve(@PathVariable long itemId) {
		return service.resolveReview(itemId);
	}

	@PostMapping("/products/{itemId}/move")
	public AdminProductResponse move(@PathVariable long itemId, @Valid @RequestBody MoveProductRequest request) {
		return service.move(itemId, request.productTypeCode());
	}

	@PostMapping("/products/{itemId}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable long itemId) {
		service.markDiscontinued(itemId);
	}

}
