package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;



@RunWith(Parameterized.class)
public class IsoCountryFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}; length: {3}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "123456", true, "123 456",2 },
				{ "123456789", true, "123 456 789",3 },
				{ "123456789", false, null,2 },
				{ "12345", false, null,2},
				{ "123", true, "123",1},
				{ "abc", false, null,1},

		});
	}

	private String input;
	private boolean expectedMatch;
	private String expectedFormatted;
	private int length;
	


	public IsoCountryFormatterTest(String input, boolean expectedMatch,
			String expectedFormatted, int length) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted = expectedFormatted;
		this.length = length;
	}



	@Test
	public void test() {
		assertEquals(expectedMatch,
				new IsoCountryFormatter(length).matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new IsoCountryFormatter(length).format(input));
		}
	}


}
