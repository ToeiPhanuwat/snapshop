package com.snapshop.service.importing;

import com.snapshop.model.enums.GroupingMethod;
import com.snapshop.model.enums.ShopType;
import java.math.BigDecimal;

/**
 * One checked row of the import file.
 *
 * @param price            null when the file shows an abbreviated price such as "1.0พัน"
 * @param commissionAmount null when not a plain number
 * @param affiliateUrl     null when blank
 * @param imageUrl         null when blank
 * @param fileGroupLabel   group label used only inside this file (E001, R001, U001); never stored
 */
public record ImportRow(
		int lineNumber,
		long itemId,
		long shopId,
		String shopName,
		ShopType shopType,
		ProductName name,
		BigDecimal price,
		String soldText,
		BigDecimal commissionRate,
		BigDecimal commissionAmount,
		String productUrl,
		String affiliateUrl,
		String imageUrl,
		String fileGroupLabel,
		String groupName,
		GroupingMethod groupingMethod) {
}
