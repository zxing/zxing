package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;



@RunWith(Parameterized.class)
public class DateFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "031230", true, "03-12-30" },
				{ "031234", false, null },
				{ "031200", true, "03-12" },
				{ "031400", false, null },
				{ "0314", false, null },

		});
	}

	private String input;
	private boolean expectedMatch;
	private String expectedFormatted;

	public DateFormatterTest(String input, 
			boolean expectedMatch, String expectedFormatted) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted = expectedFormatted;
	}

	@Test
	public void test() {
		assertEquals(expectedMatch,
				new DateFormatter().matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new DateFormatter().format(input));
		}
	}


}
