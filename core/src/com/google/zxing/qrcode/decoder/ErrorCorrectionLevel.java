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
 * <p>See ISO 18004:2006, 6.5.1. This enum encapsulates the four error correction levels
 * defined by the QR code standard.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
final class ErrorCorrectionLevel {

  // No, we can't use an enum here. J2ME doesn't support it.

  /**
   * L = ~7% correction
   */
  static final ErrorCorrectionLevel L = new ErrorCorrectionLevel(0);
  /**
   * M = ~15% correction
   */
  static final ErrorCorrectionLevel M = new ErrorCorrectionLevel(1);
  /**
   * Q = ~25% correction
   */
  static final ErrorCorrectionLevel Q = new ErrorCorrectionLevel(2);
  /**
   * H = ~30% correction
   */
  static final ErrorCorrectionLevel H = new ErrorCorrectionLevel(3);

  private static final ErrorCorrectionLevel[] FOR_BITS = {M, L, H, Q};

  private final int ordinal;

  private ErrorCorrectionLevel(int ordinal) {
    this.ordinal = ordinal;
  }

  int ordinal() {
    return ordinal;
  }

  /**
   * @param bits int containing the two bits encoding a QR Code's error correction level
   * @return {@link ErrorCorrectionLevel} representing the encoded error correction level
   */
  static ErrorCorrectionLevel forBits(int bits) throws ReaderException {
    if (bits < 0 || bits >= FOR_BITS.length) {
      throw new ReaderException("Illegal error correction level bits" + bits);
    }
    return FOR_BITS[bits];
  }


}
