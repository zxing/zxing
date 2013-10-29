package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class DecimalWithIsoFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}; length: {3}; fixLength: {4}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "06651234", true, "665 1234",4,true },
				{ "02231234", true, "223 1234",5,false},
				{ "01341234", false, null,5,true },
				{ "0123312",  false, null ,5,true },
				{ "66501234", false, null,4,true },
				{ "42341234", true, "234 0.1234",4,true },
				{ "21121234", true, "112 12.34",4,true },

		});
	}

	private String input;
	private boolean expectedMatch;
	private String expectedFormatted;
	private int length;
	private boolean fixedLength;
	


	public DecimalWithIsoFormatterTest(String input, boolean expectedMatch,
			String expectedFormatted, int length, boolean fixedLength) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted = expectedFormatted;
		this.length = length;
		this.fixedLength = fixedLength;
	}



	@Test
	public void test() {
		assertEquals(expectedMatch,
				new DecimalWithIsoFormatter(length, fixedLength).matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new DecimalWithIsoFormatter(length, fixedLength).format(input));
		}
	}


}
