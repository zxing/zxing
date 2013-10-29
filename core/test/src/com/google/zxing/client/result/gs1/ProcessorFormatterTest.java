package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class ProcessorFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ "1234564", true, "234 564"},
				{ "a234564", false, null},
				{ "1a34564", false, null},
				{ "1234a64", true, "234 a64"},
		});
	}

	private String input;
	private boolean expectedMatch;
	private String expectedFormatted;
	


	public ProcessorFormatterTest(String input, boolean expectedMatch,
			String expectedFormatted) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted = expectedFormatted;
	}



	@Test
	public void test() {
		assertEquals(expectedMatch,
				new ProcessorFormatter().matches(input));
		if (expectedMatch) {
			assertEquals(expectedFormatted, new ProcessorFormatter().format(input));
		}
	}


}
