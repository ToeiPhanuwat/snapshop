package com.snapshop.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** A former price and the time it was current. */
public record PriceHistoryResponse(BigDecimal price, Instant validFrom, Instant validTo) {
}
