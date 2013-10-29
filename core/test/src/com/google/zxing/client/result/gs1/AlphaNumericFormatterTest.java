package com.google.zxing.client.result.gs1;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AlphaNumericFormatterTest {

	
	@Parameters(name = "{index}: in: {0}; length: {1}; expected: {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] { 
        		{"0123abcABC", 20, true},
        		{"0123abcABC", 3, false},
        		{"01|", 3, false},
        		{"", 2, true},
        		{"!\"%&*+,-./:;<=>?", 20, true},
        		{"[]{}", 20, false},
        		{(String)null, 20, false},
        });
    }
	
	private String input;
	private int length;
	private boolean expected;
	
		
	
	public AlphaNumericFormatterTest(String input, int length, boolean expected) {
		this.input = input;
		this.length = length;
		this.expected = expected;
	}



	@Test
	public void test() {
		// "^[!\"%&'()*+,\\-./0-9:;<=>?A-Z_a-z]*$"
		assertEquals(expected, new AlphaNumericFormatter(length).matches(input));
	}

}
