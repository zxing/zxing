package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;



@RunWith(Parameterized.class)
public class NumericWithCheckDigitFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}; length: {3}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{"a2234567894566"    , false, null    , 14},  
				{"2234567894566"     , false, null     , 14},  
				{"12234567894566"    , true,  "1223456789456 6"    , 14},  
				{"22234567894566"    , false, null    , 14},  
				{"5523145612334"     , true,  "552314561233 4"     , 13},  
				{"4523145612334"     , false, null     , 13},  
				{"123485798123154641", true,  "12348579812315464 1", 18},  
				{"223485798123154641", false, null, 18},  
		});
	}

	private String input;
	private boolean expectedMatch;
	private int length;
	private String expectedFormatted;
	
	public NumericWithCheckDigitFormatterTest(String input, boolean expectedMatch, String expectedFormatted,
			int length) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted=expectedFormatted;
		this.length = length;
	}



	@Test
	public void test() {
		assertEquals(expectedMatch, new NumericWithCheckDigitFormatter(length).matches(input));
		if(expectedMatch){
			assertEquals(expectedFormatted, new NumericWithCheckDigitFormatter(length).format(input));
		}
	}
}

