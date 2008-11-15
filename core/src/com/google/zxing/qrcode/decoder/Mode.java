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

/**
 * <p>See ISO 18004:2006, 6.4.1, Tables 2 and 3. This enum encapsulates the various modes in which
 * data can be encoded to bits in the QR code standard.</p>
 *
 * @author Sean Owen
 */
final class Mode {

  // No, we can't use an enum here. J2ME doesn't support it.

  static final Mode TERMINATOR = new Mode(new int[]{0, 0, 0}); // Not really a mode...
  static final Mode NUMERIC = new Mode(new int[]{10, 12, 14});
  static final Mode ALPHANUMERIC = new Mode(new int[]{9, 11, 13});
  static final Mode BYTE = new Mode(new int[]{8, 16, 16});
  static final Mode ECI = new Mode(null); // character counts don't apply
  static final Mode KANJI = new Mode(new int[]{8, 10, 12});
  static final Mode FNC1_FIRST_POSITION = new Mode(null);
  static final Mode FNC1_SECOND_POSITION = new Mode(null);

  private final int[] characterCountBitsForVersions;

  private Mode(int[] characterCountBitsForVersions) {
    this.characterCountBitsForVersions = characterCountBitsForVersions;
  }

  /**
   * @param bits four bits encoding a QR Code data mode
   * @return {@link Mode} encoded by these bits
   * @throws ReaderException if bits do not correspond to a known mode
   */
  static Mode forBits(int bits) throws ReaderException {
    switch (bits) {
      case 0x0:
        return TERMINATOR;
      case 0x1:
        return NUMERIC;
      case 0x2:
        return ALPHANUMERIC;
      case 0x4:
        return BYTE;
      case 0x5:
        return FNC1_FIRST_POSITION;
      case 0x7:
        return ECI;
      case 0x8:
        return KANJI;
      case 0x9:
        return FNC1_SECOND_POSITION;
      default:
        throw new ReaderException("Unsupported mode bits: " + bits);
    }
  }

  /**
   * @param version version in question
   * @return number of bits used, in this QR Code symbol {@link Version}, to encode the
   *         count of characters that will follow encoded in this {@link Mode}
   */
  int getCharacterCountBits(Version version) {
    if (characterCountBitsForVersions == null) {
      throw new IllegalArgumentException("Character count doesn't apply to this mode");
    }
    int number = version.getVersionNumber();
    int offset;
    if (number <= 9) {
      offset = 0;
    } else if (number <= 26) {
      offset = 1;
    } else {
      offset = 2;
    }
    return characterCountBitsForVersions[offset];
  }

}
