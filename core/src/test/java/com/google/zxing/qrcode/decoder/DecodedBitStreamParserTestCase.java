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

import com.google.zxing.common.BitSourceBuilder;
import com.google.zxing.common.DecoderResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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
    DecoderResult dr = DecodedBitStreamParser.decode(builder.toByteArray(),
      Version.getVersionForNumber(1), null, null);
    String result = dr.getText();
    assertEquals("\u963f", result);
    assertEquals(1, dr.getByteSegments().size());
  }

  @Test
  public void testHanziLevel1() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x0D, 4); // Hanzi mode
    builder.write(0x01, 4); // Subset 1 = GB2312 encoding
    builder.write(0x01, 8); // 1 characters
    // A5A2 (U+30A2) => A5A2 - A1A1 = 401, 4*60 + 01 = 0181
    builder.write(0x0181, 13);
    DecoderResult dr = DecodedBitStreamParser.decode(builder.toByteArray(),
        Version.getVersionForNumber(1), null, null);
    String result = dr.getText();
    assertEquals("\u30a2", result);
    assertEquals(1, dr.getByteSegments().size());
  }

  @Test
  public void testByteStreams() throws Exception {
    byte[] rawBytes = new byte[] {70, -89, 55, 23, 38, -61, -94, -14, -10, 23, 7, 7, 50, -25, 102, 23, 38, 70, 86, -30, -26, -106, -26, 102, -14, -9, 118, 103, 7, 70, -10, -10, -57, 50, -10, -122, -105, 55, 70, -9, 39, -110, -9, 55, 23, 38, -58, 23, 87, 70, -126, -25, 6, -121, 3, -9, -125, -45, 19, 114, 103, 54, 102, -29, -43, 99, 36, 99, 85, -90, -44, 103, -107, -91, -124, -106, 117, 52, 118, -57, -90, 68, 115, -105, -106, 85, 18, 102, -25, 87, 67, -43, 87, 119, -125, 19, 101, 5, 7, 85, 38, 71, -122, -75, 55, 116, 103, -126, 3, -53, 68, 78, 115, -52, 64, -122, -90, -28, -28, 20, -43, 38, -61, 50, 3, -88, -90, -48, 75, -38, 64, 86, -74, -124, -106, 21, -112, -20, 17, -20, 17, -20, 17, -20, 17, -20, 17, -20, 17, -20, 17, -20, 17, -20};
    List<byte[]> streams = DecodedBitStreamParser.decode(
      rawBytes, Version.getVersionForNumber(1), null, null
    ).getByteSegments();

    byte[] streamBytes = new byte[0];
    for (byte[] newSegment : streams) {
      streamBytes = combine(streamBytes, newSegment);
    }
    assertEquals(
      "sqrl://apps.varden.info/wfptools/history/sqrlauth.php?x=17&sfn=V2F5ZmFyZXIgSGlzdG9yeQ&nut=Uwx16PPuRdxkSwFxQYCBADCjnNAMRl3EKJH6XQkhIaY",
      new String(streamBytes)
    );
  }

  @Test
  public void testByteStreamsNumeric() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x01, 4); // Numeric mode
    builder.write(0x01, 10); // 1 numeric
    builder.write(0x08, 4);

    DecoderResult result = DecodedBitStreamParser.decode(builder.toByteArray(),
      Version.getVersionForNumber(1), null, null);
    assertEquals(1, result.getByteSegments().size());
    assertEquals(56, result.getByteSegments().get(0)[0]); // 56 is ascii for number 8
  }

  @Test
  public void testByteStreamsTwoNumeric() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x01, 4); // Numeric mode
    builder.write(0x02, 10); // 2 numeric
    builder.write(0x0C, 7); // C = 12

    DecoderResult result = DecodedBitStreamParser.decode(builder.toByteArray(),
      Version.getVersionForNumber(1), null, null);
    assertEquals(1, result.getByteSegments().size());
    assertEquals(49, result.getByteSegments().get(0)[0]); // 49 is ascii for number 1
    assertEquals(50, result.getByteSegments().get(0)[1]); // 50 is ascii for number 2
  }

  @Test
  public void testByteStreamsAlphaNumeric() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x02, 4); // AlphaNumeric mode
    builder.write(0x01, 9); // 1 alpha numeric
    builder.write(0x0C, 6); // 12th char with 0-9 first = C

    DecoderResult result = DecodedBitStreamParser.decode(builder.toByteArray(),
      Version.getVersionForNumber(1), null, null);
    assertEquals(1, result.getByteSegments().size());
    assertEquals(67, result.getByteSegments().get(0)[0]); // 67 is ascii for char C
  }

  /**
   * Single % should be replaced with hex 1D after decoding in FNC1 mode.
   *
   * @throws Exception  Could throw FormatException if alpha numeric mode is not formatted correctly.
   */
  @Test
  public void testByteStreamsAlphaNumericWithSinglePercent() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x05, 4); // FNC1 mode
    builder.write(0x02, 4); // AlphaNumeric mode
    builder.write(0x01, 9); // 1 alpha numeric
    builder.write(0x26, 6); // %

    DecoderResult result = DecodedBitStreamParser.decode(builder.toByteArray(),
      Version.getVersionForNumber(1), null, null);
    assertEquals(1, result.getByteSegments().size());
    assertEquals(29, result.getByteSegments().get(0)[0]); // 29 hex = 1D which is the correct replacement
  }

  /**
   * Double %% should be replaced with single % after decoding in FNC1 mode.
   *
   * @throws Exception  Could throw FormatException if alpha numeric mode is not formatted correctly.
   */
  @Test
  public void testByteStreamsAlphaNumericWithDoublePercent() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x05, 4); // FNC1 mode
    builder.write(0x02, 4); // AlphaNumeric mode
    builder.write(0x02, 9); // 1 alpha numeric
    builder.write(0x6D4, 11); // %%

    DecoderResult result = DecodedBitStreamParser.decode(builder.toByteArray(),
      Version.getVersionForNumber(1), null, null);
    assertEquals(1, result.getByteSegments().size());
    assertEquals(37, result.getByteSegments().get(0)[0]); // 37 is ascii for char %
  }

  /**
   * Double %% should be replaced with single % after decoding in FNC1 mode.
   * This tests the removeByte function when you have other chars in the stream.
   *
   * @throws Exception  Could throw FormatException if alpha numeric mode is not formatted correctly.
   */
  @Test
  public void testByteStreamsAlphaNumericWithDoublePercentWithChars() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x05, 4); // FNC1 mode
    builder.write(0x02, 4); // AlphaNumeric mode
    builder.write(0x06, 9); // 1 alpha numeric
    builder.write(0x2E, 11); // 11
    builder.write(0x6D4, 11); // %%
    builder.write(0x2E, 11); // 11

    DecoderResult result = DecodedBitStreamParser.decode(builder.toByteArray(),
      Version.getVersionForNumber(1), null, null);
    assertEquals(1, result.getByteSegments().size());
    assertEquals(49, result.getByteSegments().get(0)[0]); // 49 is ascii for number 1
    assertEquals(49, result.getByteSegments().get(0)[1]); // 49 is ascii for number 1
    assertEquals(37, result.getByteSegments().get(0)[2]); // 37 is ascii for char %
    assertEquals(49, result.getByteSegments().get(0)[3]); // 49 is ascii for number 1
    assertEquals(49, result.getByteSegments().get(0)[4]); // 49 is ascii for number 1
  }

  @Test
  public void testByteStreamsKanji() throws Exception {
    BitSourceBuilder builder = new BitSourceBuilder();
    builder.write(0x08, 4); // Kanji mode
    builder.write(0x01, 8); // 1 Kanji char
    builder.write(0x01, 13); // First kanji char?

    DecoderResult result = DecodedBitStreamParser.decode(builder.toByteArray(),
      Version.getVersionForNumber(1), null, null);
    assertEquals(1, result.getByteSegments().size());
  }


  public static byte[] combine(byte[] a, byte[] b) {
    byte[] keys = new byte[a.length + b.length];
    System.arraycopy(a, 0, keys, 0, a.length);
    System.arraycopy(b, 0, keys, a.length, b.length);
    return keys;
  }

  // TODO definitely need more tests here

}
