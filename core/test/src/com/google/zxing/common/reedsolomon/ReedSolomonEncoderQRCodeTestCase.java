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

package com.google.zxing.common.reedsolomon;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class ReedSolomonEncoderQRCodeTestCase extends AbstractReedSolomonTestCase {

  /**
   * Tests example given in ISO 18004, Annex I
   */
  public void testISO18004Example() {
    int[] dataBytes = new int[] {
      0x10, 0x20, 0x0C, 0x56, 0x61, 0x80, 0xEC, 0x11,
      0xEC, 0x11, 0xEC, 0x11, 0xEC, 0x11, 0xEC, 0x11 };
    int[] expectedECBytes = new int[] {
      0xA5, 0x24, 0xD4, 0xC1, 0xED, 0x36, 0xC7, 0x87,
      0x2C, 0x55 };
    doTestQRCodeEncoding(dataBytes, expectedECBytes);
  }

  // Need more tests I am sure

}
