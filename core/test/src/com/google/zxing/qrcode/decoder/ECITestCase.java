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

import com.google.zxing.common.BitSource;
import junit.framework.TestCase;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class ECITestCase extends TestCase {

  public void testParseECI() {
    doTestParseECI(0, new byte[] { (byte) 0x00 });
    doTestParseECI(127, new byte[] { (byte) 0x7F });
    doTestParseECI(128, new byte[] { (byte) 0x80, (byte) 0x80 });
    doTestParseECI(16383, new byte[] { (byte) 0xBF, (byte) 0xFF });
    doTestParseECI(16384, new byte[] { (byte) 0xC0, (byte) 0x40, (byte) 0x00 });
    doTestParseECI(2097151, new byte[] { (byte) 0xDF, (byte) 0xFF, (byte) 0xFF });
  }

  private static void doTestParseECI(int expectedValue, byte[] bytes) {
    BitSource bitSource = new BitSource(bytes);
    int actual = ECI.parseECI(bitSource);
    assertEquals(expectedValue, actual);
  }

}