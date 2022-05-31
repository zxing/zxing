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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sean Owen
 */
public final class ModeTestCase extends Assert {

  @Test
  public void testForBits() {
    assertSame(Mode.TERMINATOR, Mode.forBits(0x00));
    assertSame(Mode.NUMERIC, Mode.forBits(0x01));
    assertSame(Mode.ALPHANUMERIC, Mode.forBits(0x02));
    assertSame(Mode.BYTE, Mode.forBits(0x04));
    assertSame(Mode.KANJI, Mode.forBits(0x08));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadMode() {
    Mode.forBits(0x10);
  }

  @Test
  public void testCharacterCount() {
    // Spot check a few values
    assertEquals(10, Mode.NUMERIC.getCharacterCountBits(Version.getVersionForNumber(5)));
    assertEquals(12, Mode.NUMERIC.getCharacterCountBits(Version.getVersionForNumber(26)));
    assertEquals(14, Mode.NUMERIC.getCharacterCountBits(Version.getVersionForNumber(40)));
    assertEquals(9, Mode.ALPHANUMERIC.getCharacterCountBits(Version.getVersionForNumber(6)));
    assertEquals(8, Mode.BYTE.getCharacterCountBits(Version.getVersionForNumber(7)));
    assertEquals(8, Mode.KANJI.getCharacterCountBits(Version.getVersionForNumber(8)));
  }

}
