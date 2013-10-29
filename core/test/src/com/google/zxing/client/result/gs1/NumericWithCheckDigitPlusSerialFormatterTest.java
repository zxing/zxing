package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;





@RunWith(Parameterized.class)
public class NumericWithCheckDigitPlusSerialFormatterTest {

	@Parameters(name = "{index}: in: {0}; expected: {1}; formatted: {2}; length: {3}; serialLength: {4}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{"a2234567894566123", false,"a2234567894566123", 14,3}, 
				{"2234567894566"    , false,"2234567894566"    , 14,3}, 
				{"12234567894566333", true, "1223456789456 6 333", 14,3}, 
				{"22234567894566444", false,"22234567894566444", 14,3}, 
				{"5523145612334555" , true, "552314561233 4 555" , 13,3}, 
				{"4523145612334555" , false,"4523145612334555" , 13,3}, 
		});
	}

	
	private String input;
	private boolean expectedMatch;
	private int length;
	private int serialLength;
	private String expectedFormatted;
	
	public NumericWithCheckDigitPlusSerialFormatterTest(String input, boolean expectedMatch, String expectedFormatted,
			int length, int serialLength) {
		this.input = input;
		this.expectedMatch = expectedMatch;
		this.expectedFormatted=expectedFormatted;
		this.length = length;
		this.serialLength =serialLength;
	}



	@Test
	public void test() {
		assertEquals(expectedMatch, new NumericWithCheckDigitPlusSerialFormatter(length,serialLength).matches(input));
		if(expectedMatch){
			assertEquals(expectedFormatted, new NumericWithCheckDigitPlusSerialFormatter(length,serialLength).format(input));
		}
	}
}
