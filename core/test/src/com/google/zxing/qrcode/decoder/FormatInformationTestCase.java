/*
 * Copyright 2007 ZXing authors
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

import com.google.zxing.ReaderException;
import junit.framework.TestCase;

/**
 * @author Sean Owen
 */
public final class FormatInformationTestCase extends TestCase {

  public void testBitsDiffering() {
    assertEquals(0, FormatInformation.numBitsDiffering(1, 1));
    assertEquals(1, FormatInformation.numBitsDiffering(0, 2));
    assertEquals(2, FormatInformation.numBitsDiffering(1, 2));
    assertEquals(32, FormatInformation.numBitsDiffering(-1, 0));
  }

  public void testDecode() throws ReaderException {
    // Normal case
    FormatInformation expected = FormatInformation.decodeFormatInformation(0x2BED ^ 0x5412);
    assertEquals((byte) 0x07, expected.getDataMask());
    assertEquals(ErrorCorrectionLevel.Q, expected.getErrorCorrectionLevel());
    // where the code forgot the mask!
    assertEquals(expected, FormatInformation.decodeFormatInformation(0x2BED));

    // 1,2,3,4 bits difference
    assertEquals(expected, FormatInformation.decodeFormatInformation(0x2BEF ^ 0x5412));
    assertEquals(expected, FormatInformation.decodeFormatInformation(0x2BEE ^ 0x5412));
    assertEquals(expected, FormatInformation.decodeFormatInformation(0x2BEA ^ 0x5412));
    assertNull(FormatInformation.decodeFormatInformation(0x2BE2 ^ 0x5412));
  }

}