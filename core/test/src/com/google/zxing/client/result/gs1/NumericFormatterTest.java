package com.google.zxing.client.result.gs1;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;




@RunWith(Parameterized.class)
public class NumericFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; length: {2}; fixLength: {3}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "12345",true,  5, true },
				{ "2345", false, 5, true },
				{ "a1234", false, 5,true },
				{ "0x123", false, 5, false},
				{ "123", true,  5, false},
				{ "123", false, 2,false},
				
		});
	}

	private String input;
	private boolean expectedMatch;
	private int length;
	private boolean fixLength;
	


	public NumericFormatterTest(String input, boolean expectedMatch,
			int length, boolean fixLength) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.length = length;
		this.fixLength = fixLength;
	}



	@Test
	public void test() {
		assertEquals(expectedMatch,
				new NumericFormatter(length, fixLength).matches(input));
	}


}
