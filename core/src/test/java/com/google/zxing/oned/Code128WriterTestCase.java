/*
 * Copyright 2014 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.oned;

import com.google.zxing.common.BitMatrixTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

/**
 * Tests {@link Code128Writer}.
 */
public class Code128WriterTestCase extends Assert {

  private static final String FNC1 = "11110101110";
  private static final String FNC2 = "11110101000";
  private static final String FNC3 = "10111100010";
  private static final String FNC4 = "10111101110";
  private static final String START_CODE_A = "11010000100";
  private static final String START_CODE_B = "11010010000";
  private static final String START_CODE_C = "11010011100";
  private static final String SWITCH_CODE_A = "11101011110";
  private static final String SWITCH_CODE_B = "10111101110";
  private static final String QUIET_SPACE = "00000";
  private static final String STOP = "1100011101011";

  private Writer writer;
  private Code128Reader reader;

  @Before
  public void setUp() {
    writer = new Code128Writer();
    reader = new Code128Reader();
  }

  @Test
  public void testEncodeWithFunc3() throws WriterException {
    String toEncode = "\u00f3" + "123";
    //                                                       "1"            "2"             "3"          check digit 51
    String expected = QUIET_SPACE + START_CODE_B + FNC3 + "10011100110" + "11001110010" + "11001011100" + "11101000110" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFunc2() throws WriterException {
    String toEncode = "\u00f2" + "123";
    //                                                       "1"            "2"             "3"          check digit 56
    String expected = QUIET_SPACE + START_CODE_B + FNC2 + "10011100110" + "11001110010" + "11001011100" + "11100010110" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFunc1() throws WriterException {
    String toEncode = "\u00f1" + "123";
    //                                                       "12"                           "3"          check digit 92
    String expected = QUIET_SPACE + START_CODE_C + FNC1 + "10110011100" + SWITCH_CODE_B + "11001011100" + "10101111000" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testRoundtrip() throws Exception {
    String toEncode = "\u00f1" + "10958" + "\u00f1" + "17160526";
    String expected = "1095817160526";

    BitMatrix encResult = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);
    BitArray row = encResult.getRow(0, null);
    Result rtResult = reader.decodeRow(0, row, null);
    String actual = rtResult.getText();
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFunc4() throws WriterException {
    String toEncode = "\u00f4" + "123";
    //                                                       "1"            "2"             "3"          check digit 59
    String expected = QUIET_SPACE + START_CODE_B + FNC4 + "10011100110" + "11001110010" + "11001011100" + "11100011010" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }
  
  @Test
  public void testEncodeSwitchBetweenCodesetsAAndB() throws Exception {
    // start with A switch to B and back to A
    //                                                      "\0"            "A"             "B"             Switch to B     "a"             "b"             Switch to A     "\u0010"        check digit
    testEncode("\0ABab\u0010", QUIET_SPACE + START_CODE_A + "10100001100" + "10100011000" + "10001011000" + SWITCH_CODE_B + "10010110000" + "10010000110" + SWITCH_CODE_A + "10100111100" + "11001110100" + STOP + QUIET_SPACE);

    // start with B switch to A and back to B
    //                                                "a"             "b"             Switch to A     "\0             "Switch to B"   "a"             "b"             check digit
    testEncode("ab\0ab", QUIET_SPACE + START_CODE_B + "10010110000" + "10010000110" + SWITCH_CODE_A + "10100001100" + SWITCH_CODE_B + "10010110000" + "10010000110" + "11010001110" + STOP + QUIET_SPACE);
  }
  
  private void testEncode(String toEncode, String expected) throws Exception {
    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(toEncode, expected, actual);
    
    BitArray row = result.getRow(0, null);
    Result rtResult = reader.decodeRow(0, row, null);
    String actualRoundtripResultText = rtResult.getText();
    assertEquals(toEncode, actualRoundtripResultText);
  }
}
