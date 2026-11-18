package com.snapshop.service.importing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductNameTest {

	@ParameterizedTest(name = "[{index}] {0}|{1}")
	@CsvSource(delimiter = '|', value = {
			"''        | Dr.PONG serum | Dr.PONG serum",
			"(1ซอง)    | Dr.PONG serum | (1ซอง) Dr.PONG serum",
	})
	void fullNameJoinsPrefixAndCoreWithOneSpace(String prefix, String core, String full) {
		assertThat(new ProductName(prefix, core).raw()).isEqualTo(full);
	}

}
