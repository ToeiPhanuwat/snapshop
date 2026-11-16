package com.snapshop.service.feed;

import com.snapshop.model.enums.ProductStatus;
import java.math.BigDecimal;

/** The stored current (active_flag = 'Y') version of an item, as far as the feed plan needs it. */
public record FeedTarget(long productId, long itemId, long shopId, BigDecimal price, ProductStatus status) {
}
