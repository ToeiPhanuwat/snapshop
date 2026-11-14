package com.snapshop.controller;

import com.snapshop.dto.request.ProductSearchRequest;
import com.snapshop.dto.response.PageResponse;
import com.snapshop.dto.response.ProductDetailResponse;
import com.snapshop.dto.response.ProductResponse;
import com.snapshop.service.ProductQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductQueryService service;

	public ProductController(ProductQueryService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<ProductResponse> search(@Valid @ModelAttribute ProductSearchRequest request) {
		return service.products(request);
	}

	@GetMapping("/{itemId}")
	public ProductDetailResponse detail(@PathVariable long itemId) {
		return service.product(itemId);
	}

}
