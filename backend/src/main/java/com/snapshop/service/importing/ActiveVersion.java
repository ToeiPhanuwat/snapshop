package com.snapshop.service.importing;

import com.snapshop.model.enums.ProductStatus;
import java.math.BigDecimal;

/** The stored current (active_flag = 'Y') version of an item. {@code price} is null when it was unknown. */
public record ActiveVersion(long productId, long itemId, long productTypeId, BigDecimal price, ProductStatus status) {
}
