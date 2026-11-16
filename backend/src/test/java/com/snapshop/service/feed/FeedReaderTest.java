package com.snapshop.service.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.snapshop.model.enums.ShopType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FeedReaderTest {

	static final String HEADER = "title,itemid,shopid,price,sale_price,stock,item_rating,shop_rating,"
			+ "is_official_shop,is_preferred_shop,image_link\n";

	@Test
	void readsWantedItemsIntoTypedRecords() {
		FeedReader.Result result = FeedReader.read(stream(HEADER
				+ "\"มี,คอมมา\nและขึ้นบรรทัดใหม่\",11,7,129.0,79,27,4.93,4.9,Official shop,Non-Preferred shop,https://cf/x\n"
				+ "other,12,7,10,10,1,5,5,Non-Official shop,Non-Preferred shop,\n"), Set.of(11L));

		FeedRecord r = result.found().get(11L);
		assertThat(result.found()).containsOnlyKeys(11L);
		assertThat(r.shopId()).isEqualTo(7);
		assertThat(r.listPrice()).isEqualByComparingTo(new BigDecimal("129"));
		assertThat(r.salePrice()).isEqualByComparingTo("79");
		assertThat(r.stock()).isEqualTo(27);
		assertThat(r.itemRating()).isEqualByComparingTo("4.93");
		assertThat(r.shopRating()).isEqualByComparingTo("4.9");
		assertThat(r.shopType()).isEqualTo(ShopType.MALL);
		assertThat(r.imageUrl()).isEqualTo("https://cf/x");
		assertThat(result.finished()).isTrue();
	}

	@Test
	void stopsReadingOnceEveryWantedItemIsFound() {
		// the stream fails after the wanted line; reading on would make the result unfinished
		FeedReader.Result result = FeedReader.read(new ThrowingAfter(HEADER + "t,11,7,1,1,1,1,1,x,y,z\n"), Set.of(11L));

		assertThat(result.found()).containsOnlyKeys(11L);
		assertThat(result.finished()).isTrue();
	}

	@Test
	void skipsBrokenRowsAndIgnoresUnknownValues() {
		FeedReader.Result result = FeedReader.read(stream("﻿" + HEADER
				+ "t,notanumber,7,1,1,1,1,1,a,b,c\n"
				+ "short,12\n"
				+ "t,13,7,abc,,,,,Weird,Weird,\n"), Set.of(12L, 13L));

		FeedRecord r = result.found().get(13L);
		assertThat(result.found()).containsOnlyKeys(13L);
		assertThat(r.salePrice()).isNull();
		assertThat(r.listPrice()).isNull();
		assertThat(r.stock()).isNull();
		assertThat(r.shopType()).isEqualTo(ShopType.REGULAR);
		assertThat(r.imageUrl()).isNull();
		assertThat(result.finished()).isTrue();
	}

	@Test
	void preferredShopWithoutOfficialBadgeIsPreferredAndOfficialWins() {
		FeedReader.Result result = FeedReader.read(stream(HEADER
				+ "t,1,7,1,1,1,1,1,Non-Official shop,Preferred shop,\n"
				+ "t,2,7,1,1,1,1,1,Official shop,Preferred shop,\n"
				+ "t,3,7,1,1,1,1,1,Non-Official shop,Non-Preferred shop,\n"), Set.of(1L, 2L, 3L));

		assertThat(result.found().get(1L).shopType()).isEqualTo(ShopType.PREFERRED);
		assertThat(result.found().get(2L).shopType()).isEqualTo(ShopType.MALL);
		assertThat(result.found().get(3L).shopType()).isEqualTo(ShopType.REGULAR);
	}

	@Test
	void reportsUnfinishedWhenTheStreamBreaksHalfway() {
		FeedReader.Result result = FeedReader.read(new ThrowingAfter(HEADER + "t,11,7,1,1,1,1,1,x,y,z\n"),
				Set.of(11L, 99L));

		assertThat(result.found()).containsOnlyKeys(11L);
		assertThat(result.finished()).isFalse();
	}

	private static InputStream stream(String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	/** Gives the text, then fails like a dropped connection. */
	private static final class ThrowingAfter extends InputStream {
		private final byte[] data;
		private int pos;

		ThrowingAfter(String text) {
			this.data = text.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public int read() throws IOException {
			if (pos >= data.length) {
				throw new IOException("connection reset");
			}
			return data[pos++] & 0xff;
		}
	}

}
