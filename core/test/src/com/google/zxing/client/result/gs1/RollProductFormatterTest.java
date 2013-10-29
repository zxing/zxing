package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RollProductFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "12341234512311", true, "1234 12345 123 1 1"},
				{ "a23412345123a1", false, null},
				{ "12341234512301", true, "1234 12345 123 0 1"},
				{ "12341234512391", true, "1234 12345 123 9 1"},
				{ "12341234512321", false, null},
		});
	}

	private String input;
	private boolean expectedMatch;
	private String expectedFormatted;
	


	public RollProductFormatterTest(String input, boolean expectedMatch,
			String expectedFormatted) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted = expectedFormatted;
	}



	@Test
	public void test() {
		assertEquals(expectedMatch,
				new RollProductFormatter().matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new RollProductFormatter().format(input));
		}
	}


}
