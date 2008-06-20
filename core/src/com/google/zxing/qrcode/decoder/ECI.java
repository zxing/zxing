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

/**
 * Superclass of classes encapsulating types ECIs, according to "Extended Channel Interpretations" 5.3.
 *
 * @author srowen@google.com (Sean Owen)
 */
abstract class ECI {

  private final int value;

  ECI(int value) {
    this.value = value;
  }

  int getValue() {
    return value;
  }

  static ECI getECIByValue(int value) {
    if (value < 0 || value > 999999) {
      throw new IllegalArgumentException("Bad ECI value: " + value);
    }
    if (value < 900) { // Character set ECIs use 000000 - 000899
      return CharacterSetECI.getCharacterSetECIByValue(value);
    }
    throw new IllegalArgumentException("Unsupported ECI value: " + value);
  }

  static int parseECI(BitSource bits) {
    int firstByte = bits.readBits(8);
    if ((firstByte & 0x80) == 0) {
      // just one byte
      return firstByte & 0x7F;
    } else if ((firstByte & 0xC0) == 0x80) {
      // two bytes
      int secondByte = bits.readBits(8);
      return ((firstByte & 0x3F) << 8) | secondByte;
    } else if ((firstByte & 0xE0) == 0xC0) {
      // three bytes
      int secondThirdBytes = bits.readBits(16);
      return ((firstByte & 0x1F) << 16) | secondThirdBytes;
    }
    throw new IllegalArgumentException("Bad ECI bits starting with byte " + firstByte);
  }

}
