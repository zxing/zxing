package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class DateTimeFormatterTest {

	@Parameters(name = "{index}: in: {0}; has seconds: {1}; expected: {2}; formatted: {3}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "031230142530", true, true, "03-12-30 14:25:30" },
				{ "031234142530", true, false, null },
				{ "0312011425", false, true, "03-12-01 14:25" },
				{ "031201142530", false, false, null },
				{ "031201142", false, false, null },

		});
	}

	private String input;
	private boolean expectedMatch;
	private String expectedFormatted;
	private boolean hasSeconds;

	public DateTimeFormatterTest(String input, boolean hasSeconds,
			boolean expectedMatch, String expectedFormatted) {
		this.input = input;
		this.hasSeconds = hasSeconds;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted = expectedFormatted;
	}

	@Test
	public void test() {
		assertEquals(expectedMatch,
				new DateTimeFormatter(hasSeconds).matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new DateTimeFormatter(hasSeconds).format(input));
		}
	}


}
