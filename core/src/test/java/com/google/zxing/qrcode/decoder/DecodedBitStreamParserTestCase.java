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

  // TODO definitely need more tests here

}
