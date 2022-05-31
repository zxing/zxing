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
public enum ErrorCorrectionLevel {

  /** L = ~7% correction */
  L(0x01),
  /** M = ~15% correction */
  M(0x00),
  /** Q = ~25% correction */
  Q(0x03),
  /** H = ~30% correction */
  H(0x02);

  private static final ErrorCorrectionLevel[] FOR_BITS = {M, L, H, Q};

  private final int bits;

  ErrorCorrectionLevel(int bits) {
    this.bits = bits;
  }

  public int getBits() {
    return bits;
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
