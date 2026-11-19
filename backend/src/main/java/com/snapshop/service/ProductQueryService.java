package com.snapshop.service;

import com.snapshop.dto.request.ProductSearchRequest;
import com.snapshop.dto.request.TypeSearchRequest;
import com.snapshop.dto.response.PageResponse;
import com.snapshop.dto.response.ProductDetailResponse;
import com.snapshop.dto.response.ProductResponse;
import com.snapshop.dto.response.ProductTypeDetailResponse;
import com.snapshop.dto.response.ProductTypeSummaryResponse;
import com.snapshop.exception.NotFoundException;
import com.snapshop.model.entity.ProductType;
import com.snapshop.repository.ProductQueryRepository;
import com.snapshop.repository.ProductTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What the user site can read: only visible items in types with at least two of them. */
@Service
@Transactional(readOnly = true)
public class ProductQueryService {

	private final ProductQueryRepository queries;
	private final ProductTypeRepository types;

	public ProductQueryService(ProductQueryRepository queries, ProductTypeRepository types) {
		this.queries = queries;
		this.types = types;
	}

	public PageResponse<ProductTypeSummaryResponse> types(TypeSearchRequest request) {
		return queries.findPublicTypes(request);
	}

	/** A name group merged into a rule group answers with the rule group, so its old link still leads somewhere. */
	public ProductTypeDetailResponse type(String code) {
		String shown = displayCode(code);
		ProductTypeSummaryResponse summary = queries.findPublicType(shown)
				.orElseThrow(() -> new NotFoundException("ไม่พบกลุ่มสินค้า " + code));
		return ProductTypeDetailResponse.of(summary, queries.findPublicOffers(shown));
	}

	private String displayCode(String code) {
		return types.findByCode(code).map(ProductType::getMergedIntoTypeId).flatMap(types::findById)
				.map(ProductType::getCode).orElse(code);
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
