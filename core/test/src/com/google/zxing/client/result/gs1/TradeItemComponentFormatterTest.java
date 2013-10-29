package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TradeItemComponentFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "122345678945661560", true, "1223456789456 6 15 60"},
				{ "12234567894566156", false, null},
				{ "122345678945651560", false, null},
		});
	}

	private String input;
	private boolean expectedMatch;
	private String expectedFormatted;
	


	public TradeItemComponentFormatterTest(String input, boolean expectedMatch,
			String expectedFormatted) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted = expectedFormatted;
	}



	@Test
	public void test() {
		assertEquals(expectedMatch,
				new TradeItemComponentFormatter().matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new TradeItemComponentFormatter().format(input));
		}
	}


}
