package com.snapshop.service;

import com.snapshop.dto.request.ProductSearchRequest;
import com.snapshop.dto.request.TypeSearchRequest;
import com.snapshop.dto.response.PageResponse;
import com.snapshop.dto.response.ProductDetailResponse;
import com.snapshop.dto.response.ProductResponse;
import com.snapshop.dto.response.ProductTypeDetailResponse;
import com.snapshop.dto.response.ProductTypeSummaryResponse;
import com.snapshop.exception.NotFoundException;
import com.snapshop.repository.ProductQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What the user site can read: only visible items in types with at least two of them. */
@Service
@Transactional(readOnly = true)
public class ProductQueryService {

	private final ProductQueryRepository queries;

	public ProductQueryService(ProductQueryRepository queries) {
		this.queries = queries;
	}

	public PageResponse<ProductTypeSummaryResponse> types(TypeSearchRequest request) {
		return queries.findPublicTypes(request);
	}

	public ProductTypeDetailResponse type(String code) {
		ProductTypeSummaryResponse summary = queries.findPublicType(code)
				.orElseThrow(() -> new NotFoundException("ไม่พบกลุ่มสินค้า " + code));
		return ProductTypeDetailResponse.of(summary, queries.findPublicOffers(code));
	}

	public PageResponse<ProductResponse> products(ProductSearchRequest request) {
		return queries.findPublicProducts(request);
	}

	public ProductDetailResponse product(long itemId) {
		ProductResponse product = queries.findPublicProduct(itemId)
				.orElseThrow(() -> new NotFoundException("ไม่พบสินค้า " + itemId));
		return new ProductDetailResponse(product, queries.findPriceHistory(itemId));
	}

}
