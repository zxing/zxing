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
import com.google.zxing.EncodeHintType;
import com.google.zxing.Result;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

import java.util.Map;
import java.util.EnumMap;

/**
 * Tests {@link Code128Writer}.
 */
public class Code128WriterTestCase extends Assert {

  private static final String FNC1 = "11110101110";
  private static final String FNC2 = "11110101000";
  private static final String FNC3 = "10111100010";
  private static final String FNC4A = "11101011110";
  private static final String FNC4B = "10111101110";
  private static final String START_CODE_A = "11010000100";
  private static final String START_CODE_B = "11010010000";
  private static final String START_CODE_C = "11010011100";
  private static final String SWITCH_CODE_A = "11101011110";
  private static final String SWITCH_CODE_B = "10111101110";
  private static final String QUIET_SPACE = "00000";
  private static final String STOP = "1100011101011";
  private static final String LF = "10000110010";

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
    String expected = QUIET_SPACE + START_CODE_B + FNC3 +
        // "1"            "2"             "3"            check digit 51
        "10011100110" + "11001110010" + "11001011100" + "11101000110" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFunc2() throws WriterException {
    String toEncode = "\u00f2" + "123";
    String expected = QUIET_SPACE + START_CODE_B + FNC2 +
        // "1"            "2"             "3"             check digit 56
        "10011100110" + "11001110010" + "11001011100" + "11100010110" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFunc1() throws WriterException {
    String toEncode = "\u00f1" + "123";
    String expected = QUIET_SPACE + START_CODE_C + FNC1 +
        // "12"                           "3"            check digit 92
        "10110011100" + SWITCH_CODE_B + "11001011100" + "10101111000" + STOP + QUIET_SPACE;

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
    String expected = QUIET_SPACE + START_CODE_B + FNC4B +
        // "1"            "2"             "3"            check digit 59
        "10011100110" + "11001110010" + "11001011100" + "11100011010" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFncsAndNumberInCodesetA() throws Exception {
    String toEncode = "\n" + "\u00f1" + "\u00f4" + "1" + "\n";

    String expected = QUIET_SPACE + START_CODE_A + LF + FNC1 + FNC4A +
        "10011100110" + LF + "10101111000" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = BitMatrixTestCase.matrixToString(result);

    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeSwitchBetweenCodesetsAAndB() throws Exception {
    // start with A switch to B and back to A
    testEncode("\0ABab\u0010", QUIET_SPACE + START_CODE_A +
        // "\0"            "A"             "B"             Switch to B     "a"             "b"
        "10100001100" + "10100011000" + "10001011000" + SWITCH_CODE_B + "10010110000" + "10010000110" +
        // Switch to A    "\u0010"        check digit
        SWITCH_CODE_A + "10100111100" + "11001110100" + STOP + QUIET_SPACE);

    // start with B switch to A and back to B
    testEncode("ab\0ab", QUIET_SPACE + START_CODE_B +
        //  "a"             "b"            Switch to A     "\0"           Switch to B
        "10010110000" + "10010000110" + SWITCH_CODE_A + "10100001100" + SWITCH_CODE_B +
        //  "a"             "b"            check digit
        "10010110000" + "10010000110" + "11010001110" + STOP + QUIET_SPACE);
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

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetABadCharacter() throws Exception {
    // Lower case characters should not be accepted when the code set is forced to A.
    String toEncode = "ASDFx0123";

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "A");
    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetBBadCharacter() throws Exception {
    String toEncode = "ASdf\00123"; // \0 (ascii value 0)
    // Characters with ASCII value below 32 should not be accepted when the code set is forced to B.

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "B");
    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetCBadCharactersNonNum() throws Exception {
    String toEncode = "123a5678";
    // Non-digit characters should not be accepted when the code set is forced to C.

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "C");
    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetCBadCharactersFncCode() throws Exception {
    String toEncode = "123\u00f2a678";
    // Function codes other than 1 should not be accepted when the code set is forced to C.

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "C");
    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetCWrongAmountOfDigits() throws Exception {
    String toEncode = "123456789";
    // An uneven amount of digits should not be accepted when the code set is forced to C.

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "C");
    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test
  public void testEncodeWithForcedCodeSetFailureCodeSetA() throws Exception {
    String toEncode = "AB123";
    //                          would default to B   "A"             "B"             "1"
    String expected = QUIET_SPACE + START_CODE_A + "10100011000" + "10001011000" + "10011100110" +
        // "2"             "3"           check digit 10
        "11001110010" + "11001011100" + "11001000100" + STOP + QUIET_SPACE;

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "A");
    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithForcedCodeSetFailureCodeSetB() throws Exception {
    String toEncode = "1234";
    //                          would default to C   "1"             "2"             "3"
    String expected = QUIET_SPACE + START_CODE_B + "10011100110" + "11001110010" + "11001011100" +
        // "4"           check digit 88
        "11001001110" + "11110010010" + STOP + QUIET_SPACE;

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "B");
    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);
  }
}
