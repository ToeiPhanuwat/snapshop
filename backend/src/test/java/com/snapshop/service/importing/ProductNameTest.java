package com.snapshop.service.importing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductNameTest {

	@ParameterizedTest(name = "[{index}] {0}")
	@CsvSource(delimiter = '|', value = {
			"Dr.PONG serum                       | ''                     | Dr.PONG serum",
			"[ส่งฟรี] Dr.PONG serum             | '[ส่งฟรี] '            | Dr.PONG serum",
			"**Dr.PONG All Serum                 | **                     | Dr.PONG All Serum",
			"> English:Dr.PONG All Serum         | > English:             | Dr.PONG All Serum",
			"ยาสีฟัน Dr.kiss white dr.pong       | 'ยาสีฟัน '             | Dr.kiss white dr.pong",
			"promo DR. PONG                      | 'promo '               | DR. PONG",
			"[กด 1 ฟรี 1] 28D ESSENCE            | ''                     | [กด 1 ฟรี 1] 28D ESSENCE",
	})
	void splitsAtFirstDrIgnoringCase(String raw, String prefix, String core) {
		ProductName name = ProductName.of(raw);

		assertThat(name.prefix()).isEqualTo(prefix);
		assertThat(name.core()).isEqualTo(core);
		assertThat(name.raw()).isEqualTo(raw);
	}

}
