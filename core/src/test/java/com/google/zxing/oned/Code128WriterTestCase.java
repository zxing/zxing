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
  public void testEncodeWithFunc3() throws Exception {
    String toEncode = "\u00f3" + "123";
    String expected = QUIET_SPACE + START_CODE_B + FNC3 +
        // "1"            "2"             "3"            check digit 51
        "10011100110" + "11001110010" + "11001011100" + "11101000110" + STOP + QUIET_SPACE;

    BitMatrix result = encode(toEncode, false, "123");

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);

    int width = result.getWidth();
    result = encode(toEncode, true, "123");

    assertEquals(width, result.getWidth());
  }

  @Test
  public void testEncodeWithFunc2() throws Exception {
    String toEncode = "\u00f2" + "123";
    String expected = QUIET_SPACE + START_CODE_B + FNC2 +
        // "1"            "2"             "3"             check digit 56
        "10011100110" + "11001110010" + "11001011100" + "11100010110" + STOP + QUIET_SPACE;

    BitMatrix result = encode(toEncode, false, "123");

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);

    int width = result.getWidth();
    result = encode(toEncode, true, "123");

    assertEquals(width, result.getWidth());
  }

  @Test
  public void testEncodeWithFunc1() throws Exception {
    String toEncode = "\u00f1" + "123";
    String expected = QUIET_SPACE + START_CODE_C + FNC1 +
        // "12"                           "3"            check digit 92
        "10110011100" + SWITCH_CODE_B + "11001011100" + "10101111000" + STOP + QUIET_SPACE;

    BitMatrix result = encode(toEncode, false, "123");

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);

    int width = result.getWidth();
    result = encode(toEncode, true, "123");

    assertEquals(width, result.getWidth());
  }

  @Test
  public void testRoundtrip() throws Exception {
    String toEncode = "\u00f1" + "10958" + "\u00f1" + "17160526";
    String expected = "1095817160526";

    BitMatrix encResult = encode(toEncode, false, expected);

    int width = encResult.getWidth();
    encResult = encode(toEncode, true, expected);
    //Compact encoding has one latch less and encodes as STARTA,FNC1,1,CODEC,09,58,FNC1,17,16,05,26
    assertEquals(width, encResult.getWidth() + 11);
  }

  @Test
  public void testLongCompact() throws Exception {
    //test longest possible input
    String toEncode = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    encode(toEncode, true, toEncode);
  }

  @Test
  public void testShift() throws Exception {
    //compare fast to compact
    String toEncode = "a\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\na\n";
    BitMatrix result = encode(toEncode, false, toEncode);

    int width = result.getWidth();
    result = encode(toEncode, true, toEncode);

    //big difference since the fast algoritm doesn't make use of SHIFT
    assertEquals(width, result.getWidth() + 253);
  }

  @Test
  public void testDigitMixCompaction() throws Exception {
    //compare fast to compact
    String toEncode = "A1A12A123A1234A12345AA1AA12AA123AA1234AA1235";
    BitMatrix result = encode(toEncode, false, toEncode);

    int width = result.getWidth();
    result = encode(toEncode, true, toEncode);

    //very good, no difference
    assertEquals(width, result.getWidth());
  }

  @Test
  public void testCompaction1() throws Exception {
    //compare fast to compact
    String toEncode = "AAAAAAAAAAA12AAAAAAAAA";
    BitMatrix result = encode(toEncode, false, toEncode);

    int width = result.getWidth();
    result = encode(toEncode, true, toEncode);

    //very good, no difference
    assertEquals(width, result.getWidth());
  }

  @Test
  public void testCompaction2() throws Exception {
    //compare fast to compact
    String toEncode = "AAAAAAAAAAA1212aaaaaaaaa";
    BitMatrix result = encode(toEncode, false, toEncode);

    int width = result.getWidth();
    result = encode(toEncode, true, toEncode);

    //very good, no difference
    assertEquals(width, result.getWidth());
  }

  @Test
  public void testEncodeWithFunc4() throws Exception {
    String toEncode = "\u00f4" + "123";
    String expected = QUIET_SPACE + START_CODE_B + FNC4B +
        // "1"            "2"             "3"            check digit 59
        "10011100110" + "11001110010" + "11001011100" + "11100011010" + STOP + QUIET_SPACE;

    BitMatrix result = encode(toEncode, false, null);

    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(expected, actual);

    int width = result.getWidth();
    result = encode(toEncode, true, null);
    assertEquals(width, result.getWidth());
  }

  @Test
  public void testEncodeWithFncsAndNumberInCodesetA() throws Exception {
    String toEncode = "\n" + "\u00f1" + "\u00f4" + "1" + "\n";

    String expected = QUIET_SPACE + START_CODE_A + LF + FNC1 + FNC4A +
        "10011100110" + LF + "10101111000" + STOP + QUIET_SPACE;

    BitMatrix result = encode(toEncode, false, null);

    String actual = BitMatrixTestCase.matrixToString(result);

    assertEquals(expected, actual);

    int width = result.getWidth();
    result = encode(toEncode, true, null);
    assertEquals(width, result.getWidth());
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
    // the compact encoder encodes this shorter as STARTB,a,b,SHIFT,NUL,a,b
    testEncode("ab\0ab", QUIET_SPACE + START_CODE_B +
        //  "a"             "b"            Switch to A     "\0"           Switch to B
        "10010110000" + "10010000110" + SWITCH_CODE_A + "10100001100" + SWITCH_CODE_B +
        //  "a"             "b"            check digit
        "10010110000" + "10010000110" + "11010001110" + STOP + QUIET_SPACE);
  }

  private void testEncode(String toEncode, String expected) throws Exception {
    BitMatrix result = encode(toEncode, false, toEncode);
    String actual = BitMatrixTestCase.matrixToString(result);
    assertEquals(toEncode, expected, actual);


    int width = result.getWidth();
    result = encode(toEncode, true, toEncode);
    assertTrue(result.getWidth() <= width);

  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetABadCharacter() throws Exception {
    // Lower case characters should not be accepted when the code set is forced to A.
    String toEncode = "ASDFx0123";

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "A");
    writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetBBadCharacter() throws Exception {
    String toEncode = "ASdf\00123"; // \0 (ascii value 0)
    // Characters with ASCII value below 32 should not be accepted when the code set is forced to B.

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "B");
    writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetCBadCharactersNonNum() throws Exception {
    String toEncode = "123a5678";
    // Non-digit characters should not be accepted when the code set is forced to C.

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "C");
    writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetCBadCharactersFncCode() throws Exception {
    String toEncode = "123\u00f2a678";
    // Function codes other than 1 should not be accepted when the code set is forced to C.

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "C");
    writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeWithForcedCodeSetFailureCodeSetCWrongAmountOfDigits() throws Exception {
    String toEncode = "123456789";
    // An uneven amount of digits should not be accepted when the code set is forced to C.

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.FORCE_CODE_SET, "C");
    writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
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

  private BitMatrix encode(String toEncode, boolean compact, String expectedLoopback) throws Exception {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    if (compact) {
      hints.put(EncodeHintType.CODE128_COMPACT, Boolean.TRUE);
    }
    BitMatrix encResult = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0, hints);
    if (expectedLoopback != null) {
      BitArray row = encResult.getRow(0, null);
      Result rtResult = reader.decodeRow(0, row, null);
      String actual = rtResult.getText();
      assertEquals(expectedLoopback, actual);
    }
    if (compact) {
      //check that what is encoded compactly yields the same on loopback as what was encoded fast.
      BitArray row = encResult.getRow(0, null);
      Result rtResult = reader.decodeRow(0, row, null);
      String actual = rtResult.getText();
      BitMatrix encResultFast = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);
      row = encResultFast.getRow(0, null);
      rtResult = reader.decodeRow(0, row, null);
      assertEquals(rtResult.getText(), actual);
    }
    return encResult;
  }
}
