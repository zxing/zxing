package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AlphaNumericWithIsoFormatterTest {

	@Parameters(name = "{index}: in: {0}; length: {1}; expected: {2}; formatted: {3}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "123abcAB", 5, true, "123 abcAB" },
				{ "0123abcABC", 3, false, null }, 
				{ "01AabcABC", 10, false, null }, 
				{ "01|", 3, false, null },
				{ "234", 2, true, "234 " }, 
				{ "456!\"%&*+,-./:;<=>?", 20, true , "456 !\"%&*+,-./:;<=>?"},
				{ "[]{}", 20, false ,null}, 
				{ (String) null, 20, false, null }, });
	}

	private String input;
	private int length;
	private boolean expectedMatch;
	private String expectedFormatted;

	public AlphaNumericWithIsoFormatterTest(String input, int length,
			boolean expectedMatch, String expectedFormatted) {
		this.input = input;
		this.length = length;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted = expectedFormatted;
	}

	@Test
	public void test() {
		// "^[!\"%&'()*+,\\-./0-9:;<=>?A-Z_a-z]*$"
		assertEquals(expectedMatch,
				new AlphaNumericWithIsoFormatter(length).matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new AlphaNumericWithIsoFormatter(
					length).format(input));
		}
	}

}
