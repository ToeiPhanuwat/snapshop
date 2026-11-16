package com.snapshop.service.feed;

import com.snapshop.model.enums.ShopType;
import com.snapshop.service.CsvReaders;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Boundary: reads the feed stream and keeps only the wanted items, turned into checked {@link FeedRecord}s.
 * Reading stops as soon as every wanted item is found, so a 4 GB feed is read only as far as needed.
 */
final class FeedReader {

	private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.get();
	private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d+)?");
	private static final Pattern WHOLE = Pattern.compile("\\d{1,9}");
	private static final Pattern ID = Pattern.compile("\\d{1,18}");
	private static final String OFFICIAL = "Official shop";
	private static final String PREFERRED = "Preferred shop";

	private FeedReader() {
	}

	/** @param finished false when the stream broke before the wanted items were all found or the feed ended */
	record Result(Map<Long, FeedRecord> found, boolean finished) {
	}

	static Result read(InputStream in, Set<Long> wanted) {
		Map<Long, FeedRecord> found = new HashMap<>();
		try (CSVParser csv = FORMAT.parse(CsvReaders.utf8(in))) {
			Iterator<CSVRecord> records = csv.iterator();
			while (found.size() < wanted.size() && records.hasNext()) {
				parse(records.next(), wanted).ifPresent(r -> found.put(r.itemId(), r));
			}
			return new Result(found, true);
		} catch (IOException | UncheckedIOException e) {
			return new Result(found, false);
		}
	}

	private static Optional<FeedRecord> parse(CSVRecord record, Set<Long> wanted) {
		String itemId = text(record, "itemid");
		String shopId = text(record, "shopid");
		if (!record.isConsistent() || !ID.matcher(itemId).matches() || !ID.matcher(shopId).matches()
				|| !wanted.contains(Long.parseLong(itemId))) {
			return Optional.empty();
		}
		return Optional.of(new FeedRecord(Long.parseLong(itemId), Long.parseLong(shopId), number(record, "sale_price"),
				number(record, "price"), stock(record), number(record, "item_rating"), number(record, "shop_rating"),
				shopType(text(record, "is_official_shop"), text(record, "is_preferred_shop")),
				blankToNull(text(record, "image_link"))));
	}

	/** Official shop is MALL; otherwise preferred shop is PREFERRED; every other shop is REGULAR. */
	static ShopType shopType(String officialShop, String preferredShop) {
		if (OFFICIAL.equals(officialShop)) {
			return ShopType.MALL;
		}
		return PREFERRED.equals(preferredShop) ? ShopType.PREFERRED : ShopType.REGULAR;
	}

	private static Integer stock(CSVRecord record) {
		String text = text(record, "stock");
		return WHOLE.matcher(text).matches() ? Integer.valueOf(text) : null;
	}

	private static BigDecimal number(CSVRecord record, String column) {
		String text = text(record, column);
		return NUMBER.matcher(text).matches() ? new BigDecimal(text) : null;
	}

	private static String text(CSVRecord record, String column) {
		return record.isSet(column) ? record.get(column).strip() : "";
	}

	private static String blankToNull(String text) {
		return text.isEmpty() ? null : text;
	}

}
