package com.snapshop.service.importing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVRecord;

/** Reads one CSV record; adds a Thai message per problem to the shared error list. */
final class RecordReader {

	private static final Pattern PRODUCT_URL = Pattern.compile("^https://shopee\\.co\\.th/product/(\\d+)/(\\d+)/?$");
	private static final Pattern DIGITS = Pattern.compile("\\d+");
	private static final Pattern PLAIN_NUMBER = Pattern.compile("\\d+(\\.\\d+)?");

	private final CSVRecord record;
	private final int line;
	private final List<String> errors;
	private final Map<Long, Integer> lineByItem;

	RecordReader(CSVRecord record, List<String> errors, Map<Long, Integer> lineByItem) {
		this.record = record;
		this.line = (int) record.getRecordNumber() + 1;
		this.errors = errors;
		this.lineByItem = lineByItem;
	}

	Optional<ImportRow> read() {
		int errorsBefore = errors.size();
		Optional<Long> itemId = itemId();
		Optional<Long> shopId = itemId.flatMap(this::shopId);
		Optional<String> shopName = required(Columns.SHOP_NAME, "ชื่อร้านค้าว่าง");
		Optional<String> name = required(Columns.NAME, "ชื่อสินค้าว่าง");
		Optional<String> groupLabel = required(Columns.GROUP_LABEL, "รหัสกลุ่มว่าง");
		Optional<String> groupName = required(Columns.GROUP_NAME, "ชื่อกลุ่มว่าง");
		Optional<RuleGroup> ruleGroup = ruleGroup();
		if (errors.size() > errorsBefore) {
			return Optional.empty();
		}
		return Optional.of(new ImportRow(line, itemId.orElseThrow(), shopId.orElseThrow(), shopName.orElseThrow(),
				new ProductName(get(Columns.NAME_PREFIX).strip(), name.orElseThrow()), amount(Columns.PRICE),
				optional(Columns.SOLD), amount(Columns.COMMISSION_RATE), amount(Columns.COMMISSION),
				get(Columns.PRODUCT_URL).strip(), optional(Columns.AFFILIATE_URL),
				groupLabel.orElseThrow(), groupName.orElseThrow(), ruleGroup.orElse(null)));
	}

	private Optional<Long> itemId() {
		String text = get(Columns.ITEM_ID).strip();
		if (!DIGITS.matcher(text).matches()) {
			error("รหัสสินค้าไม่ใช่ตัวเลข '" + text + "'");
			return Optional.empty();
		}
		long itemId = Long.parseLong(text);
		Integer firstLine = lineByItem.putIfAbsent(itemId, line);
		if (firstLine != null) {
			error("รหัสสินค้าซ้ำกับแถว " + firstLine);
			return Optional.empty();
		}
		return Optional.of(itemId);
	}

	private Optional<Long> shopId(long itemId) {
		String url = get(Columns.PRODUCT_URL).strip();
		Matcher m = PRODUCT_URL.matcher(url);
		if (!m.matches() || Long.parseLong(m.group(2)) != itemId) {
			error("ลิงก์สินค้าไม่ตรงกับรหัสสินค้า '" + url + "'");
			return Optional.empty();
		}
		return Optional.of(Long.parseLong(m.group(1)));
	}

	/** Both columns blank means no rule group; one blank and one filled is an error. */
	private Optional<RuleGroup> ruleGroup() {
		String label = get(Columns.RULE_GROUP_LABEL).strip();
		String name = get(Columns.RULE_GROUP_NAME).strip();
		if (label.isEmpty() != name.isEmpty()) {
			error("รหัสกลุ่มตามกฎกับชื่อกลุ่มตามกฎต้องมีทั้งคู่หรือว่างทั้งคู่");
		}
		return label.isEmpty() || name.isEmpty() ? Optional.empty() : Optional.of(new RuleGroup(label, name));
	}

	private Optional<String> required(String column, String message) {
		String text = get(column);
		if (text.isBlank()) {
			error(message);
			return Optional.empty();
		}
		return Optional.of(text);
	}

	private String optional(String column) {
		String text = get(column).strip();
		return text.isEmpty() ? null : text;
	}

	/** "฿1,311.50" or "5%" become numbers; abbreviated text such as "1.0พัน" becomes null. */
	private BigDecimal amount(String column) {
		String text = get(column).replace("฿", "").replace(",", "").replace("%", "").strip();
		return PLAIN_NUMBER.matcher(text).matches() ? new BigDecimal(text) : null;
	}

	private String get(String column) {
		return record.isSet(column) ? record.get(column) : "";
	}

	private void error(String message) {
		errors.add("แถว " + line + ": " + message);
	}

}
