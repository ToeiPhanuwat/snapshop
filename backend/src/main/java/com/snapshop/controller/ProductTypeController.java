package com.snapshop.controller;

import com.snapshop.dto.request.TypeSearchRequest;
import com.snapshop.dto.response.PageResponse;
import com.snapshop.dto.response.ProductTypeDetailResponse;
import com.snapshop.dto.response.ProductTypeSummaryResponse;
import com.snapshop.service.ProductQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-types")
public class ProductTypeController {

	private final ProductQueryService service;

	public ProductTypeController(ProductQueryService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<ProductTypeSummaryResponse> search(@Valid @ModelAttribute TypeSearchRequest request) {
		return service.types(request);
	}

	@GetMapping("/{code}")
	public ProductTypeDetailResponse detail(@PathVariable String code) {
		return service.type(code);
	}

}
