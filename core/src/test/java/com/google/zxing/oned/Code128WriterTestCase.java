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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class Code128WriterTestCase extends Assert {

  private static final String FNC1 = "11110101110";
  private static final String FNC2 = "11110101000";
  private static final String FNC3 = "10111100010";
  private static final String FNC4 = "10111101110";
  private static final String START_CODE_B = "11010010000";
  public static final String QUIET_SPACE = "00000";
  public static final String STOP = "1100011101011";

  private Writer writer;

  @Before
  public void setup() {
    writer = new Code128Writer();
  }

  @Test
  public void testEncodeWithFunc3() throws WriterException {
    String toEncode = "\u00f3" + "123";
    //                                                       "1"            "2"             "3"          check digit 51
    String expected = QUIET_SPACE + START_CODE_B + FNC3 + "10011100110" + "11001110010" + "11001011100" + "11101000110" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFunc2() throws WriterException {
    String toEncode = "\u00f2" + "123";
    //                                                       "1"            "2"             "3"          check digit 56
    String expected = QUIET_SPACE + START_CODE_B + FNC2 + "10011100110" + "11001110010" + "11001011100" + "11100010110" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFunc1() throws WriterException {
    String toEncode = "\u00f1" + "123";
    //                                                       "1"            "2"             "3"          check digit 61
    String expected = QUIET_SPACE + START_CODE_B + FNC1 + "10011100110" + "11001110010" + "11001011100" + "11001000010" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = matrixToString(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testEncodeWithFunc4() throws WriterException {
    String toEncode = "\u00f4" + "123";
    //                                                       "1"            "2"             "3"          check digit 59
    String expected = QUIET_SPACE + START_CODE_B + FNC4 + "10011100110" + "11001110010" + "11001011100" + "11100011010" + STOP + QUIET_SPACE;

    BitMatrix result = writer.encode(toEncode, BarcodeFormat.CODE_128, 0, 0);

    String actual = matrixToString(result);
    assertEquals(expected, actual);
  }

  private static String matrixToString(BitMatrix result) {
    StringBuilder builder = new StringBuilder(result.getWidth());
    for (int i = 0; i < result.getWidth(); i++) {
      builder.append(result.get(i, 0) ? '1' : '0');
    }
    return builder.toString();
  }
}
