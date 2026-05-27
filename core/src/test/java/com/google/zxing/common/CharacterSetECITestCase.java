/*
 * Copyright 2026 ZXing authors
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

import com.google.zxing.FormatException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Tests {@link CharacterSetECI}.
 */
public final class CharacterSetECITestCase extends Assert {

  @Test
  public void testStandardCharsets() throws FormatException {
    assertSame(CharacterSetECI.ISO8859_1, CharacterSetECI.getCharacterSetECI(StandardCharsets.ISO_8859_1));
    assertSame(CharacterSetECI.ISO8859_1, CharacterSetECI.getCharacterSetECIByName("ISO-8859-1"));
    assertSame(CharacterSetECI.ISO8859_1, CharacterSetECI.getCharacterSetECIByValue(1));
    assertSame(StandardCharsets.ISO_8859_1, CharacterSetECI.ISO8859_1.getCharset());

    assertSame(CharacterSetECI.UTF8, CharacterSetECI.getCharacterSetECI(StandardCharsets.UTF_8));
    assertSame(CharacterSetECI.UTF8, CharacterSetECI.getCharacterSetECIByName("UTF-8"));
    assertSame(CharacterSetECI.UTF8, CharacterSetECI.getCharacterSetECIByValue(26));
    assertSame(StandardCharsets.UTF_8, CharacterSetECI.UTF8.getCharset());

    assertSame(CharacterSetECI.ASCII, CharacterSetECI.getCharacterSetECI(StandardCharsets.US_ASCII));
    assertSame(CharacterSetECI.ASCII, CharacterSetECI.getCharacterSetECIByName("US-ASCII"));
    assertSame(CharacterSetECI.ASCII, CharacterSetECI.getCharacterSetECIByValue(27));
    assertSame(StandardCharsets.US_ASCII, CharacterSetECI.ASCII.getCharset());

    assertSame(CharacterSetECI.UnicodeBigUnmarked,
               CharacterSetECI.getCharacterSetECI(StandardCharsets.UTF_16BE));
    assertSame(CharacterSetECI.UnicodeBigUnmarked, CharacterSetECI.getCharacterSetECIByName("UTF-16BE"));
    assertSame(CharacterSetECI.UnicodeBigUnmarked, CharacterSetECI.getCharacterSetECIByValue(25));
    assertSame(StandardCharsets.UTF_16BE, CharacterSetECI.UnicodeBigUnmarked.getCharset());
  }

  @Test
  public void testGB18030UsesPrimaryCharset() throws FormatException {
    if (Charset.isSupported("GB18030")) {
      assertSame(CharacterSetECI.GB18030, CharacterSetECI.getCharacterSetECIByName("GBK"));
      assertSame(CharacterSetECI.GB18030, CharacterSetECI.getCharacterSetECIByValue(29));
      assertEquals(Charset.forName("GB18030"), CharacterSetECI.GB18030.getCharset());
    }
  }

}
