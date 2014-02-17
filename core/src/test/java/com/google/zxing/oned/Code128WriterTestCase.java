package com.google.zxing.oned;

import org.junit.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import static org.junit.Assert.*;

public class Code128WriterTestCase {
	private static final String FNC3 = "10111100010";
	private static final String START_CODE_B = "11010010000";
	public static final String QUIET_SPACE = "00000";
	public static final String STOP = "1100011101011";
	
	@Test
	public void testEncodeWithFunc3() throws WriterException {
		String toEncode = "\u00f3" + "123";
		//                                                       "1"            "2"             "3"          check digit 51
		String expected = QUIET_SPACE + START_CODE_B + FNC3 + "10011100110" + "11001110010" + "11001011100" + "11101000110" + STOP + QUIET_SPACE;
		
		Writer writer = new Code128Writer();
		BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);
		
		StringBuilder actual = new StringBuilder(result.getWidth());
		for (int i = 0; i < result.getWidth(); i++) {
			actual.append(result.get(i, 0) ? '1' : '0');
		}
		assertEquals(expected, actual.toString());
	}
}
