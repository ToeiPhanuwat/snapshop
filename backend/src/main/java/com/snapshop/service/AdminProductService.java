package com.snapshop.service;

import com.snapshop.dto.request.AdminProductSearchRequest;
import com.snapshop.dto.request.TypeSearchRequest;
import com.snapshop.dto.response.AdminProductResponse;
import com.snapshop.dto.response.AdminProductTypeResponse;
import com.snapshop.dto.response.PageResponse;
import com.snapshop.exception.NotFoundException;
import com.snapshop.model.entity.MarketplaceProduct;
import com.snapshop.model.entity.ProductType;
import com.snapshop.model.enums.ActiveFlag;
import com.snapshop.model.enums.DeactivationReason;
import com.snapshop.repository.MarketplaceProductRepository;
import com.snapshop.repository.ProductQueryRepository;
import com.snapshop.repository.ProductTypeRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin decisions the import cannot make: resolve REVIEW, move an item to another type, mark it discontinued. */
@Service
@Transactional
public class AdminProductService {

	private final MarketplaceProductRepository products;
	private final ProductTypeRepository types;
	private final ProductQueryRepository queries;

	public AdminProductService(MarketplaceProductRepository products, ProductTypeRepository types,
			ProductQueryRepository queries) {
		this.products = products;
		this.types = types;
		this.queries = queries;
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminProductResponse> products(AdminProductSearchRequest request) {
		return queries.findAdminProducts(request);
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminProductTypeResponse> types(TypeSearchRequest request) {
		return queries.findAdminTypes(request);
	}

	public AdminProductResponse resolveReview(long itemId) {
		current(itemId).resolveReview();
		return reload(itemId);
	}

	/** Moves every version of the item, so its price history stays with it. */
	public AdminProductResponse move(long itemId, String productTypeCode) {
		current(itemId);
		ProductType target = types.findByCode(productTypeCode)
				.orElseThrow(() -> new NotFoundException("ไม่พบกลุ่มสินค้า " + productTypeCode));
		products.findByItemIdOrderByIdDesc(itemId).forEach(p -> p.moveTo(target.getId()));
		return reload(itemId);
	}

	public void markDiscontinued(long itemId) {
		current(itemId).deactivate(DeactivationReason.DISCONTINUED, Instant.now());
	}

	private MarketplaceProduct current(long itemId) {
		return products.findByItemIdAndActiveFlag(itemId, ActiveFlag.Y)
				.orElseThrow(() -> new NotFoundException("ไม่พบสินค้าปัจจุบัน " + itemId));
	}

	private AdminProductResponse reload(long itemId) {
		products.flush();
		return queries.findAdminProduct(itemId).orElseThrow();
	}

}
