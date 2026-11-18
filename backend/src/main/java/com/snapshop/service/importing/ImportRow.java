package com.snapshop.service.importing;

import java.math.BigDecimal;

/**
 * One checked row of the import file.
 *
 * @param price            null when the file shows an abbreviated price such as "1.0พัน"; the feed supplies it
 * @param commissionAmount null when not a plain number
 * @param affiliateUrl     null when blank
 * @param fileGroupLabel   group label used only inside this file (E001…); never stored
 * @param ruleGroup        null when the row has no rule group
 */
public record ImportRow(
		int lineNumber,
		long itemId,
		long shopId,
		String shopName,
		ProductName name,
		BigDecimal price,
		String soldText,
		BigDecimal commissionRate,
		BigDecimal commissionAmount,
		String productUrl,
		String affiliateUrl,
		String fileGroupLabel,
		String groupName,
		RuleGroup ruleGroup) {
}
