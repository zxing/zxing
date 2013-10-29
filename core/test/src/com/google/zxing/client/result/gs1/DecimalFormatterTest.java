package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;




@RunWith(Parameterized.class)
public class DecimalFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}; length: {3}; fixLength: {4}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "01234", true, "1234",4,true },
				{ "01234", true, "1234",5,false},
				{ "01234", false, null,5,true },
				{ "0312",  false, null ,5,true },
				{ "61234", false, null,4,true },
				{ "41234", true, "0.1234",4,true },
				{ "21234", true, "12.34",4,true },

		});
	}

	private String input;
	private boolean expectedMatch;
	private String expectedFormatted;
	private int length;
	private boolean fixedLength;
	


	public DecimalFormatterTest(String input, boolean expectedMatch,
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
				new DecimalFormatter(length, fixedLength).matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new DecimalFormatter(length, fixedLength).format(input));
		}
	}


}
