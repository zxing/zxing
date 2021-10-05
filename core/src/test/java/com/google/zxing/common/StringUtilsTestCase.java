/*
 * Copyright 2012 ZXing authors
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

package com.google.zxing.common;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Tests {@link StringUtils}.
 */
public final class StringUtilsTestCase extends Assert {

  @Test
  public void testRandom() {
    Random r = new Random(1234L);
    byte[] bytes = new byte[1000];
    r.nextBytes(bytes);
    assertEquals(Charset.defaultCharset(), StringUtils.guessCharset(bytes, null));
  }

  @Test
  public void testShortShiftJIS1() {
    // 金魚
    doTest(new byte[] { (byte) 0x8b, (byte) 0xe0, (byte) 0x8b, (byte) 0x9b, }, StringUtils.SHIFT_JIS_CHARSET, "SJIS");
  }

  @Test
  public void testShortISO885911() {
    // båd
    doTest(new byte[] { (byte) 0x62, (byte) 0xe5, (byte) 0x64, }, StandardCharsets.ISO_8859_1, "ISO8859_1");
  }

  @Test
  public void testShortUTF81() {
    // Español
    doTest(new byte[] { (byte) 0x45, (byte) 0x73, (byte) 0x70, (byte) 0x61, (byte) 0xc3,
                        (byte) 0xb1, (byte) 0x6f, (byte) 0x6c },
           StandardCharsets.UTF_8, "UTF8");
  }

  @Test
  public void testMixedShiftJIS1() {
    // Hello 金!
    doTest(new byte[] { (byte) 0x48, (byte) 0x65, (byte) 0x6c, (byte) 0x6c, (byte) 0x6f,
                        (byte) 0x20, (byte) 0x8b, (byte) 0xe0, (byte) 0x21, },
           StringUtils.SHIFT_JIS_CHARSET, "SJIS");
  }

  @Test
  public void testUTF16BE() {
    // 调压柜
    doTest(new byte[] { (byte) 0xFE, (byte) 0xFF, (byte) 0x8c, (byte) 0x03, (byte) 0x53, (byte) 0x8b,
                        (byte) 0x67, (byte) 0xdc, },
           StandardCharsets.UTF_16,
           StandardCharsets.UTF_16.name());
  }

  @Test
  public void testUTF16LE() {
    // 调压柜
    doTest(new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x03, (byte) 0x8c, (byte) 0x8b, (byte) 0x53,
                        (byte) 0xdc, (byte) 0x67, },
           StandardCharsets.UTF_16,
           StandardCharsets.UTF_16.name());
  }

  private static void doTest(byte[] bytes, Charset charset, String encoding) {
    Charset guessedCharset = StringUtils.guessCharset(bytes, null);
    String guessedEncoding = StringUtils.guessEncoding(bytes, null);
    assertEquals(charset, guessedCharset);
    assertEquals(encoding, guessedEncoding);
  }

  /**
   * Utility for printing out a string in given encoding as a Java statement, since it's better
   * to write that into the Java source file rather than risk character encoding issues in the
   * source file itself.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    String text = args[0];
    Charset charset = Charset.forName(args[1]);
    StringBuilder declaration = new StringBuilder();
    declaration.append("new byte[] { ");
    for (byte b : text.getBytes(charset)) {
      declaration.append("(byte) 0x");
      int value = b & 0xFF;
      if (value < 0x10) {
        declaration.append('0');
      }
      declaration.append(Integer.toHexString(value));
      declaration.append(", ");
    }
    declaration.append('}');
    System.out.println(declaration);
  }

}
