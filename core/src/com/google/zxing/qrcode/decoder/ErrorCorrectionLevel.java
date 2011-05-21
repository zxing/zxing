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

/**
 * <p>See ISO 18004:2006, 6.5.1. This enum encapsulates the four error correction levels
 * defined by the QR code standard.</p>
 *
 * @author Sean Owen
 */
public final class ErrorCorrectionLevel {

  // No, we can't use an enum here. J2ME doesn't support it.

  /**
   * L = ~7% correction
   */
  public static final ErrorCorrectionLevel L = new ErrorCorrectionLevel(0, 0x01, "L");
  /**
   * M = ~15% correction
   */
  public static final ErrorCorrectionLevel M = new ErrorCorrectionLevel(1, 0x00, "M");
  /**
   * Q = ~25% correction
   */
  public static final ErrorCorrectionLevel Q = new ErrorCorrectionLevel(2, 0x03, "Q");
  /**
   * H = ~30% correction
   */
  public static final ErrorCorrectionLevel H = new ErrorCorrectionLevel(3, 0x02, "H");

  private static final ErrorCorrectionLevel[] FOR_BITS = {M, L, H, Q};

  private final int ordinal;
  private final int bits;
  private final String name;

  private ErrorCorrectionLevel(int ordinal, int bits, String name) {
    this.ordinal = ordinal;
    this.bits = bits;
    this.name = name;
  }

  public int ordinal() {
    return ordinal;
  }

  public int getBits() {
    return bits;
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return name;
  }

  /**
   * @param bits int containing the two bits encoding a QR Code's error correction level
   * @return ErrorCorrectionLevel representing the encoded error correction level
   */
  public static ErrorCorrectionLevel forBits(int bits) {
    if (bits < 0 || bits >= FOR_BITS.length) {
      throw new IllegalArgumentException();
    }
    return FOR_BITS[bits];
  }


}
