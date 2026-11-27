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

package com.google.zxing.qrcode.decoder;

import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitSourceBuilder;
import com.google.zxing.common.DecoderResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 * Tests {@link DecodedBitStreamParser}.
 *
 * @author Sean Owen
 */
public final class DecodedBitStreamParserTestCase extends Assert {

  @Test
  public void testSimpleByteMode() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x04, 4); // Byte mode
    builder.write(0x03, 8); // 3 bytes
    builder.write(0xF1, 8);
    builder.write(0xF2, 8);
    builder.write(0xF3, 8);
    String result = DecodedBitStreamParser.decode(builder.toByteArray(),
        Version.getVersionForNumber(1), null, null).getText();
    assertEquals("\u00f1\u00f2\u00f3", result);
  }

  @Test
  public void testByteModeLatin1NotMistakenForUTF8() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x04, 4); // Byte mode
    builder.write(0x02, 8); // 2 bytes
    builder.write(0xC4, 8); // looks like a UTF-8 lead byte...
    builder.write(0xE4, 8); // ...but this is not a continuation byte
    String result = DecodedBitStreamParser.decode(builder.toByteArray(),
        Version.getVersionForNumber(1), null, null).getText();
    assertEquals("\u00c4\u00e4", result);
  }

  @Test
  public void testSimpleSJIS() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x04, 4); // Byte mode
    builder.write(0x04, 8); // 4 bytes
    builder.write(0xA1, 8);
    builder.write(0xA2, 8);
    builder.write(0xA3, 8);
    builder.write(0xD0, 8);
    String result = DecodedBitStreamParser.decode(builder.toByteArray(),
        Version.getVersionForNumber(1), null, null).getText();
    assertEquals("\uff61\uff62\uff63\uff90", result);
  }

  @Test
  public void testECI() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x07, 4); // ECI mode
    builder.write(0x02, 8); // ECI 2 = CP437 encoding
    builder.write(0x04, 4); // Byte mode
    builder.write(0x03, 8); // 3 bytes
    builder.write(0xA1, 8);
    builder.write(0xA2, 8);
    builder.write(0xA3, 8);
    String result = DecodedBitStreamParser.decode(builder.toByteArray(),
        Version.getVersionForNumber(1), null, null).getText();
    assertEquals("\u00ed\u00f3\u00fa", result);
  }

  @Test
  public void testHanzi() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x0D, 4); // Hanzi mode
    builder.write(0x01, 4); // Subset 1 = GB2312 encoding
    builder.write(0x01, 8); // 1 characters
    builder.write(0x03C1, 13);
    String result = DecodedBitStreamParser.decode(builder.toByteArray(),
        Version.getVersionForNumber(1), null, null).getText();
    assertEquals("\u963f", result);
  }

  @Test
  public void testHanziLevel1() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x0D, 4); // Hanzi mode
    builder.write(0x01, 4); // Subset 1 = GB2312 encoding
    builder.write(0x01, 8); // 1 characters
    // A5A2 (U+30A2) => A5A2 - A1A1 = 401, 4*60 + 01 = 0181
    builder.write(0x0181, 13);
    String result = DecodedBitStreamParser.decode(builder.toByteArray(),
        Version.getVersionForNumber(1), null, null).getText();
    assertEquals("\u30a2", result);
  }

  @Test
  public void testFNC1FirstPositionWithAndWithoutAssumeGS1() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x05, 4); // FNC1 in first position
    builder.write(0x02, 4); // Alphanumeric mode
    builder.write(6, 9);    // 6 chars: "A%B%%C" -> "A\u001dB%C"
    // Alphanumeric codes: A=10, %=38, B=11, C=12
    builder.write(10 * 45 + 38, 11); // "A%"
    builder.write(11 * 45 + 38, 11); // "B%"
    builder.write(38 * 45 + 12, 11); // "%C"
    byte[] bytes = builder.toByteArray();

    DecoderResult withoutHint = DecodedBitStreamParser.decode(bytes, Version.getVersionForNumber(1), null, null);
    assertEquals("A\u001dB%C", withoutHint.getText());
    assertEquals(3, withoutHint.getSymbologyModifier());

    Map<DecodeHintType,?> gs1Hints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    DecoderResult withHint = DecodedBitStreamParser.decode(bytes, Version.getVersionForNumber(1), null, gs1Hints);
    assertEquals("]Q3A\u001dB%C", withHint.getText());
    assertEquals(3, withHint.getSymbologyModifier());
  }

  @Test
  public void testFNC1FirstPositionWithECIAndAssumeGS1() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x07, 4); // ECI mode
    builder.write(26, 8);   // ECI 26 = UTF-8
    builder.write(0x05, 4); // FNC1 in first position
    builder.write(0x04, 4); // Byte mode
    builder.write(3, 8);    // 3 bytes: "A\u001dB"
    builder.write('A', 8);
    builder.write(0x1D, 8);
    builder.write('B', 8);
    byte[] bytes = builder.toByteArray();

    Map<DecodeHintType,?> gs1Hints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    DecoderResult withHint = DecodedBitStreamParser.decode(bytes, Version.getVersionForNumber(1), null, gs1Hints);
    assertEquals("]Q4A\u001dB", withHint.getText());
    assertEquals(4, withHint.getSymbologyModifier());

    // Non-GS1 QR code is not prefixed even when ASSUME_GS1 is supplied
    BitSourceBuilder nonGs1 = new BitSourceBuilder();
    nonGs1.write(0x04, 4);
    nonGs1.write(2, 8);
    nonGs1.write('A', 8);
    nonGs1.write('B', 8);
    DecoderResult plain = DecodedBitStreamParser.decode(
        nonGs1.toByteArray(), Version.getVersionForNumber(1), null, gs1Hints);
    assertEquals("AB", plain.getText());
    assertEquals(1, plain.getSymbologyModifier());
  }

  @Test
  public void testFNC1SecondPositionNumericApplicationIndicator() throws Exception {
    // Numeric application indicator 37 + Alphanumeric "A%B%%C" -> "37A\u001dB%C"
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x09, 4); // FNC1 in second position
    builder.write(37, 8);   // Application indicator "37"
    builder.write(0x02, 4); // Alphanumeric mode
    builder.write(6, 9);    // 6 chars: "A%B%%C"
    builder.write(10 * 45 + 38, 11);
    builder.write(11 * 45 + 38, 11);
    builder.write(38 * 45 + 12, 11);
    byte[] bytes = builder.toByteArray();

    DecoderResult result = DecodedBitStreamParser.decode(bytes, Version.getVersionForNumber(1), null, null);
    assertEquals("37A\u001dB%C", result.getText());
    assertEquals(5, result.getSymbologyModifier());

    // ASSUME_GS1 must not prepend ]Q5 for FNC1 in second position (AIM industry, not GS1)
    Map<DecodeHintType,?> gs1Hints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    DecoderResult withHint = DecodedBitStreamParser.decode(bytes, Version.getVersionForNumber(1), null, gs1Hints);
    assertEquals("37A\u001dB%C", withHint.getText());
    assertEquals(5, withHint.getSymbologyModifier());

    // Zero-padded two-digit numeric indicators: 0 -> "00", 5 -> "05", 99 -> "99"
    for (int[] pair : new int[][]{{0, '0', '0'}, {5, '0', '5'}, {99, '9', '9'}}) {
      BitSourceBuilder num = new BitSourceBuilder();
      num.write(0x09, 4);
      num.write(pair[0], 8);
      num.write(0x01, 4); // Numeric mode
      num.write(2, 10);
      num.write(42, 7);
      DecoderResult r = DecodedBitStreamParser.decode(num.toByteArray(), Version.getVersionForNumber(1), null, null);
      assertEquals("" + (char) pair[1] + (char) pair[2] + "42", r.getText());
      assertEquals(5, r.getSymbologyModifier());
    }
  }

  @Test
  public void testFNC1SecondPositionAlphabeticApplicationIndicator() throws Exception {
    for (char letter : new char[]{'A', 'Z', 'a', 'z'}) {
      BitSourceBuilder builder = new BitSourceBuilder();
      builder.write(0x09, 4);           // FNC1 in second position
      builder.write(letter + 100, 8);   // ASCII + 100
      builder.write(0x01, 4);           // Numeric mode
      builder.write(3, 10);
      builder.write(123, 10);
      DecoderResult result = DecodedBitStreamParser.decode(
          builder.toByteArray(), Version.getVersionForNumber(1), null, null);
      assertEquals(letter + "123", result.getText());
      assertEquals(5, result.getSymbologyModifier());
    }

    // With ECI -> symbology modifier 6
    BitSourceBuilder withEci = new BitSourceBuilder();
    withEci.write(0x07, 4);       // ECI mode
    withEci.write(26, 8);         // UTF-8
    withEci.write(0x09, 4);       // FNC1 in second position
    withEci.write('A' + 100, 8);  // Application indicator 'A'
    withEci.write(0x04, 4);       // Byte mode
    withEci.write(2, 8);
    withEci.write('O', 8);
    withEci.write('K', 8);
    DecoderResult result = DecodedBitStreamParser.decode(
        withEci.toByteArray(), Version.getVersionForNumber(1), null, null);
    assertEquals("AOK", result.getText());
    assertEquals(6, result.getSymbologyModifier());
  }

  @Test
  public void testFNC1SecondPositionInvalidApplicationIndicator() {
    for (int invalid : new int[]{100, 164, 191, 196, 223, 255}) {
      BitSourceBuilder builder = new BitSourceBuilder();
      builder.write(0x09, 4);
      builder.write(invalid, 8);
      assertFormatException(builder.toByteArray());
    }
    // Truncated application indicator (< 8 bits left)
    assertFormatException(new byte[]{(byte) 0x90});
  }

  @Test
  public void testFNC1OrderingAndMutualExclusion() {
    // Both FNC1 first and second position
    BitSourceBuilder both = new BitSourceBuilder();
    both.write(0x05, 4);
    both.write(0x09, 4);
    both.write(37, 8);
    assertFormatException(both.toByteArray());

    // Duplicate FNC1 first position
    BitSourceBuilder dupFirst = new BitSourceBuilder();
    dupFirst.write(0x05, 4);
    dupFirst.write(0x05, 4);
    assertFormatException(dupFirst.toByteArray());

    // FNC1 first position after data segment
    BitSourceBuilder afterData = new BitSourceBuilder();
    afterData.write(0x01, 4);
    afterData.write(1, 10);
    afterData.write(5, 4);
    afterData.write(0x05, 4);
    assertFormatException(afterData.toByteArray());

    // FNC1 second position after data segment
    BitSourceBuilder secondAfterData = new BitSourceBuilder();
    secondAfterData.write(0x01, 4);
    secondAfterData.write(1, 10);
    secondAfterData.write(5, 4);
    secondAfterData.write(0x09, 4);
    secondAfterData.write(37, 8);
    assertFormatException(secondAfterData.toByteArray());
  }

  @Test
  public void testHanziValidation() throws Exception {
    // Unknown Hanzi subset must throw FormatException instead of skipping payload bits
    for (int badSubset : new int[]{0x00, 0x02, 0x0F}) {
      BitSourceBuilder builder = new BitSourceBuilder();
      builder.write(0x0D, 4);      // Hanzi mode
      builder.write(badSubset, 4); // Invalid subset
      builder.write(0x01, 8);      // 1 char
      builder.write(0x03C1, 13);
      assertFormatException(builder.toByteArray());
    }

    // Invalid 13-bit Hanzi codewords: low byte 0x5E (0xFF), 0x5F (carries into high byte), high byte 0x55 (0xFB)
    for (int badCode : new int[]{0x005E, 0x005F, 0x03BE, 0x03BF, 0x1FDE, 0x1FE0}) {
      BitSourceBuilder builder = new BitSourceBuilder();
      builder.write(0x0D, 4);
      builder.write(0x01, 4);
      builder.write(0x01, 8);
      builder.write(badCode, 13);
      assertFormatException(builder.toByteArray());
    }

    // Valid range boundary: 0x0000 -> 0xA1A1 (ideographic space U+3000)
    BitSourceBuilder validBoundary = new BitSourceBuilder();
    validBoundary.write(0x0D, 4);
    validBoundary.write(0x01, 4);
    validBoundary.write(0x01, 8);
    validBoundary.write(0x0000, 13);
    assertEquals("\u3000", DecodedBitStreamParser.decode(
        validBoundary.toByteArray(), Version.getVersionForNumber(1), null, null).getText());
  }

  @Test
  public void testKanjiValidation() throws Exception {
    // Valid Shift_JIS Kanji: 0x0000 -> 0x8140 (ideographic space U+3000)
    BitSourceBuilder valid = new BitSourceBuilder();
    valid.write(0x08, 4);
    valid.write(1, 8);
    valid.write(0x0000, 13);
    assertEquals("\u3000", DecodedBitStreamParser.decode(
        valid.toByteArray(), Version.getVersionForNumber(1), null, null).getText());

    // Invalid Shift_JIS trail bytes: 0x7F (0x003F), 0xFD (0x173D -> 0x9FFD), 0xFF (0x00BF -> 0x81FF)
    for (int badCode : new int[]{0x003F, 0x173D, 0x00BF, 0x1740 + 0x3F}) {
      BitSourceBuilder invalid = new BitSourceBuilder();
      invalid.write(0x08, 4);
      invalid.write(1, 8);
      invalid.write(badCode, 13);
      assertFormatException(invalid.toByteArray());
    }
  }

  @Test
  public void testStructuredAppendValidation() throws Exception {
    // Valid Structured Append: symbol 2 of 3 (0x12), parity 0xAB
    BitSourceBuilder valid = new BitSourceBuilder();
    valid.write(0x03, 4);
    valid.write(0x12, 8);
    valid.write(0xAB, 8);
    valid.write(0x01, 4);
    valid.write(1, 10);
    valid.write(7, 4);
    DecoderResult result = DecodedBitStreamParser.decode(
        valid.toByteArray(), Version.getVersionForNumber(1), null, null);
    assertEquals("7", result.getText());
    assertTrue(result.hasStructuredAppend());
    assertEquals(0x12, result.getStructuredAppendSequenceNumber());
    assertEquals(0xAB, result.getStructuredAppendParity());

    // Position > total (symbol 4 of 3 -> 0x32)
    BitSourceBuilder badPos = new BitSourceBuilder();
    badPos.write(0x03, 4);
    badPos.write(0x32, 8);
    badPos.write(0xAB, 8);
    assertFormatException(badPos.toByteArray());

    // Total symbols == 1 (totalMinusOne == 0 -> 0x00)
    BitSourceBuilder badTotal = new BitSourceBuilder();
    badTotal.write(0x03, 4);
    badTotal.write(0x00, 8);
    badTotal.write(0xAB, 8);
    assertFormatException(badTotal.toByteArray());

    // Duplicate Structured Append
    BitSourceBuilder dupSa = new BitSourceBuilder();
    dupSa.write(0x03, 4);
    dupSa.write(0x01, 8);
    dupSa.write(0xAA, 8);
    dupSa.write(0x03, 4);
    dupSa.write(0x01, 8);
    dupSa.write(0xAA, 8);
    assertFormatException(dupSa.toByteArray());

    // Structured Append after data segment
    BitSourceBuilder saAfterData = new BitSourceBuilder();
    saAfterData.write(0x01, 4);
    saAfterData.write(1, 10);
    saAfterData.write(7, 4);
    saAfterData.write(0x03, 4);
    saAfterData.write(0x01, 8);
    saAfterData.write(0xAA, 8);
    assertFormatException(saAfterData.toByteArray());
  }

  @Test
  public void testECIMinimalEncodingValidation() throws Exception {
    // Valid 2-byte ECI 170 (US-ASCII): 10000000 10101010
    BitSourceBuilder validTwoByte = new BitSourceBuilder();
    validTwoByte.write(0x07, 4);
    validTwoByte.write(0x80, 8);
    validTwoByte.write(170, 8);
    validTwoByte.write(0x04, 4);
    validTwoByte.write(2, 8);
    validTwoByte.write('H', 8);
    validTwoByte.write('i', 8);
    DecoderResult res = DecodedBitStreamParser.decode(
        validTwoByte.toByteArray(), Version.getVersionForNumber(1), null, null);
    assertEquals("Hi", res.getText());
    assertEquals(2, res.getSymbologyModifier());

    // Overlong 2-byte encoding of ECI 26 (< 128)
    BitSourceBuilder overlong2 = new BitSourceBuilder();
    overlong2.write(0x07, 4);
    overlong2.write(0x80, 8);
    overlong2.write(26, 8);
    assertFormatException(overlong2.toByteArray());

    // Overlong 3-byte encoding of ECI 26 (< 16384)
    BitSourceBuilder overlong3 = new BitSourceBuilder();
    overlong3.write(0x07, 4);
    overlong3.write(0xC0, 8);
    overlong3.write(0x00, 8);
    overlong3.write(26, 8);
    assertFormatException(overlong3.toByteArray());
  }

  @Test
  public void testAbbreviatedTerminatorMustBeZero() throws Exception {
    // Numeric mode (4) + count=2 (10) + "42" (7) = 21 bits -> 3 bits left in 3-byte array
    BitSourceBuilder valid = new BitSourceBuilder();
    valid.write(0x01, 4);
    valid.write(2, 10);
    valid.write(42, 7);
    valid.write(0, 3); // valid 3-bit abbreviated terminator 000
    assertEquals("42", DecodedBitStreamParser.decode(
        valid.toByteArray(), Version.getVersionForNumber(1), null, null).getText());

    BitSourceBuilder invalid = new BitSourceBuilder();
    invalid.write(0x01, 4);
    invalid.write(2, 10);
    invalid.write(42, 7);
    invalid.write(0b101, 3); // non-zero trailing bits
    assertFormatException(invalid.toByteArray());
  }

  private static void assertFormatException(byte[] bytes) {
    try {
      DecodedBitStreamParser.decode(bytes, Version.getVersionForNumber(1), null, null);
      fail("Expected FormatException");
    } catch (FormatException expected) {
      // expected
    }
  }

}
