package com.snapshop.dto.response;

import java.util.List;

/** @param priceHistory former prices, newest first */
public record ProductDetailResponse(ProductResponse product, List<PriceHistoryResponse> priceHistory) {
}
