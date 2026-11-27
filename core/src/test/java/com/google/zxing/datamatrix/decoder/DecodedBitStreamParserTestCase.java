/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.datamatrix.decoder;

import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.common.DecoderResult;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * @author bbrown@google.com (Brian Brown)
 */
public final class DecodedBitStreamParserTestCase extends Assert {

  @Test
  public void testAsciiStandardDecode() throws Exception {
    // ASCII characters 0-127 are encoded as the value + 1
    byte[] bytes = {(byte) ('a' + 1), (byte) ('b' + 1), (byte) ('c' + 1),
                    (byte) ('A' + 1), (byte) ('B' + 1), (byte) ('C' + 1)};
    String decodedString = DecodedBitStreamParser.decode(bytes).getText();
    assertEquals("abcABC", decodedString);
  }

  @Test
  public void testAsciiDoubleDigitDecode() throws Exception {
    // ASCII double digit (00 - 99) Numeric Value + 130
    byte[] bytes = {(byte)       130 , (byte) (1 + 130),
                    (byte) (98 + 130), (byte) (99 + 130)};
    String decodedString = DecodedBitStreamParser.decode(bytes).getText();
    assertEquals("00019899", decodedString);
  }

  @Test(expected = FormatException.class)
  public void testBase256TruncatedLongLength() throws Exception {
    // 231 latches to Base 256. The next byte unrandomizes to 250 at codeword position 2,
    // which signals a multi-byte length, but no further byte is present to read.
    byte[] bytes = {(byte) 231, (byte) 38};
    DecodedBitStreamParser.decode(bytes);
  }

  @Test
  public void testEciUtf8AsciiAndUpperShift() throws Exception {
    // ECI 26 (UTF-8) is encoded as 241, 27. "café" in UTF-8 is 63 61 66 C3 A9.
    byte[] bytes = {
        (byte) 241, (byte) 27,
        (byte) ('c' + 1), (byte) ('a' + 1), (byte) ('f' + 1),
        (byte) 235, (byte) (0xC3 - 127),
        (byte) 235, (byte) (0xA9 - 127)
    };
    DecoderResult result = DecodedBitStreamParser.decode(bytes);
    assertEquals("caf\u00e9", result.getText());
    assertEquals(4, result.getSymbologyModifier());
  }

  @Test
  public void testEciUtf8Base256() throws Exception {
    String expected = "caf\u00e9\u4e2d\u6587";
    byte[] utf8 = expected.getBytes(StandardCharsets.UTF_8);
    // 241, 27 (ECI UTF-8), 231 (latch Base 256), randomized length, randomized payload bytes
    byte[] codewords = new byte[4 + utf8.length];
    codewords[0] = (byte) 241;
    codewords[1] = (byte) 27;
    codewords[2] = (byte) 231;
    codewords[3] = randomize255State(utf8.length, 4);
    for (int i = 0; i < utf8.length; i++) {
      codewords[4 + i] = randomize255State(utf8[i] & 0xFF, 5 + i);
    }
    DecoderResult result = DecodedBitStreamParser.decode(codewords);
    assertEquals(expected, result.getText());
    assertEquals(4, result.getSymbologyModifier());
  }

  @Test
  public void testEciMultipleCharsetSwitching() throws Exception {
    // Default ISO-8859-1 "é" (0xE9)
    // -> ECI 26 (UTF-8) "中" (0xE4 0xB8 0xAD)
    // -> ECI 22 (Cp1251) "Ж" (0xC6)
    // -> ECI 3 (ISO-8859-1) "ñ" (0xF1)
    byte[] bytes = {
        (byte) 235, (byte) (0xE9 - 127),
        (byte) 241, (byte) 27,
        (byte) 235, (byte) (0xE4 - 127),
        (byte) 235, (byte) (0xB8 - 127),
        (byte) 235, (byte) (0xAD - 127),
        (byte) 241, (byte) 23,
        (byte) 235, (byte) (0xC6 - 127),
        (byte) 241, (byte) 4,
        (byte) 235, (byte) (0xF1 - 127)
    };
    DecoderResult result = DecodedBitStreamParser.decode(bytes);
    assertEquals("\u00e9\u4e2d\u0416\u00f1", result.getText());
    assertEquals(4, result.getSymbologyModifier());
  }

  @Test
  public void testEciTwoCodewordAscii170() throws Exception {
    // ECI 170 (US-ASCII) is encoded in 2 codewords: c1 = (170 - 127) / 254 + 128 = 128,
    // c2 = (170 - 127) % 254 + 1 = 44.
    byte[] bytes = {
        (byte) 241, (byte) 128, (byte) 44,
        (byte) ('A' + 1), (byte) ('B' + 1)
    };
    DecoderResult result = DecodedBitStreamParser.decode(bytes);
    assertEquals("AB", result.getText());
    assertEquals(4, result.getSymbologyModifier());

    // Confirm US-ASCII charset is actually active (0xE9 is replaced with U+FFFD rather than "é").
    byte[] highByte = {
        (byte) 241, (byte) 128, (byte) 44,
        (byte) 235, (byte) (0xE9 - 127)
    };
    assertEquals("\ufffd", DecodedBitStreamParser.decode(highByte).getText());
  }

  @Test
  public void testEciUnsupportedValuesRejected() {
    byte[][] unsupportedCases = {
        // 1-codeword unsupported ECI 99
        {(byte) 241, (byte) 100, (byte) ('A' + 1)},
        // 2-codeword unsupported ECI 127
        {(byte) 241, (byte) 128, (byte) 1, (byte) ('A' + 1)},
        // 2-codeword unsupported ECI 899 (binary)
        {(byte) 241, (byte) 131, (byte) 11, (byte) ('A' + 1)},
        // 3-codeword unsupported ECI 16383
        {(byte) 241, (byte) 192, (byte) 1, (byte) 1, (byte) ('A' + 1)},
        // 3-codeword unsupported ECI 999999
        {(byte) 241, (byte) 207, (byte) 63, (byte) 129, (byte) ('A' + 1)}
    };
    for (byte[] bytes : unsupportedCases) {
      try {
        DecodedBitStreamParser.decode(bytes);
        fail("Expected FormatException for unsupported ECI");
      } catch (FormatException expected) {
        // expected
      }
    }
  }

  @Test
  public void testEciMalformedOrTruncatedRejected() {
    byte[][] invalidCases = {
        {(byte) 241},
        {(byte) 241, (byte) 0},
        {(byte) 241, (byte) 208},
        {(byte) 241, (byte) 255},
        {(byte) 241, (byte) 128},
        {(byte) 241, (byte) 128, (byte) 0},
        {(byte) 241, (byte) 128, (byte) 255},
        {(byte) 241, (byte) 192},
        {(byte) 241, (byte) 192, (byte) 1},
        {(byte) 241, (byte) 192, (byte) 0, (byte) 1},
        {(byte) 241, (byte) 192, (byte) 255, (byte) 1},
        {(byte) 241, (byte) 192, (byte) 1, (byte) 0},
        {(byte) 241, (byte) 192, (byte) 1, (byte) 255}
    };
    for (byte[] bytes : invalidCases) {
      try {
        DecodedBitStreamParser.decode(bytes);
        fail("Expected FormatException for malformed ECI");
      } catch (FormatException expected) {
        // expected
      }
    }
  }

  @Test
  public void testGS1FNC1WithAndWithoutAssumeGS1() throws Exception {
    // 232 (FNC1), "01", "AB", 232 (FNC1), "17"
    byte[] bytes = {
        (byte) 232,
        (byte) (1 + 130),
        (byte) ('A' + 1), (byte) ('B' + 1),
        (byte) 232,
        (byte) (17 + 130)
    };
    DecoderResult withoutHint = DecodedBitStreamParser.decode(bytes);
    assertEquals("\u001d01AB\u001d17", withoutHint.getText());
    assertEquals(2, withoutHint.getSymbologyModifier());

    Map<DecodeHintType,?> gs1Hints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    DecoderResult withHint = DecodedBitStreamParser.decode(bytes, gs1Hints);
    assertEquals("]d201AB\u001d17", withHint.getText());
    assertEquals(2, withHint.getSymbologyModifier());
  }

  @Test
  public void testGS1FNC1WithECIAndAssumeGS1() throws Exception {
    // 232 (FNC1), 241, 27 (ECI UTF-8), "café", 232 (FNC1), "17"
    byte[] bytes = {
        (byte) 232,
        (byte) 241, (byte) 27,
        (byte) ('c' + 1), (byte) ('a' + 1), (byte) ('f' + 1),
        (byte) 235, (byte) (0xC3 - 127),
        (byte) 235, (byte) (0xA9 - 127),
        (byte) 232,
        (byte) (17 + 130)
    };
    Map<DecodeHintType,?> gs1Hints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    DecoderResult withHint = DecodedBitStreamParser.decode(bytes, gs1Hints);
    assertEquals("]d5caf\u00e9\u001d17", withHint.getText());
    assertEquals(5, withHint.getSymbologyModifier());
  }

  @Test
  public void testFNC1WithUtf16BeEciDoesNotCorruptBytes() throws Exception {
    // 232 (FNC1), 241, 26 (ECI 25 = UTF-16BE), "中" (0x4E 0x2D), 232 (FNC1), "文" (0x65 0x87)
    byte[] bytes = {
        (byte) 232,
        (byte) 241, (byte) 26,
        (byte) (0x4E + 1), (byte) (0x2D + 1),
        (byte) 232,
        (byte) (0x65 + 1), (byte) 235, (byte) (0x87 - 127)
    };
    DecoderResult withoutHint = DecodedBitStreamParser.decode(bytes);
    assertEquals("\u001d\u4e2d\u001d\u6587", withoutHint.getText());
    assertEquals(5, withoutHint.getSymbologyModifier());

    Map<DecodeHintType,?> gs1Hints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    DecoderResult withHint = DecodedBitStreamParser.decode(bytes, gs1Hints);
    assertEquals("]d5\u4e2d\u001d\u6587", withHint.getText());
    assertEquals(5, withHint.getSymbologyModifier());
  }

  @Test
  public void testLiteralGSNotConflatedWithFNC1() throws Exception {
    // Codeword 30 is literal ASCII 29 (GS), NOT FNC1 (232)
    byte[] literalLeadingGS = {(byte) 30, (byte) ('A' + 1), (byte) ('B' + 1)};
    Map<DecodeHintType,?> gs1Hints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    DecoderResult result = DecodedBitStreamParser.decode(literalLeadingGS, gs1Hints);
    assertEquals("\u001dAB", result.getText());
    assertEquals(1, result.getSymbologyModifier());

    // Non-GS1 symbol with 4 data characters followed by FNC1 must not be treated as ]d2 without Structured Append
    byte[] fnc1AtIndex4 = {
        (byte) ('A' + 1), (byte) ('B' + 1), (byte) ('C' + 1), (byte) ('D' + 1),
        (byte) 232,
        (byte) ('E' + 1)
    };
    DecoderResult nonGs1 = DecodedBitStreamParser.decode(fnc1AtIndex4, gs1Hints);
    assertEquals("ABCD\u001dE", nonGs1.getText());
    assertEquals(1, nonGs1.getSymbologyModifier());
  }

  private static byte randomize255State(int value, int codewordPosition) {
    int pseudoRandomNumber = ((149 * codewordPosition) % 255) + 1;
    int tempVariable = value + pseudoRandomNumber;
    return (byte) (tempVariable <= 255 ? tempVariable : tempVariable - 256);
  }

  // TODO(bbrown): Add test cases for each encoding type
  // TODO(bbrown): Add test cases for switching encoding types
}