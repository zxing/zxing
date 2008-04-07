/*
 * Copyright 2007 Google Inc.
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
 * <p>Encapsulates a QR Code's format information, including the data mask used and
 * error correction level.</p>
 *
 * @author srowen@google.com (Sean Owen)
 * @see DataMask
 * @see ErrorCorrectionLevel
 */
final class FormatInformation {

  private static final int FORMAT_INFO_MASK_QR = 0x5412;

  /**
   * See ISO 18004:2006, Annex C, Table C.1
   */
  private static final int[][] FORMAT_INFO_DECODE_LOOKUP = {
      {0x5412, 0x00},
      {0x5125, 0x01},
      {0x5E7C, 0x02},
      {0x5B4B, 0x03},
      {0x45F9, 0x04},
      {0x40CE, 0x05},
      {0x4F97, 0x06},
      {0x4AA0, 0x07},
      {0x77C4, 0x08},
      {0x72F3, 0x09},
      {0x7DAA, 0x0A},
      {0x789D, 0x0B},
      {0x662F, 0x0C},
      {0x6318, 0x0D},
      {0x6C41, 0x0E},
      {0x6976, 0x0F},
      {0x1689, 0x10},
      {0x13BE, 0x11},
      {0x1CE7, 0x12},
      {0x19D0, 0x13},
      {0x0762, 0x14},
      {0x0255, 0x15},
      {0x0D0C, 0x16},
      {0x083B, 0x17},
      {0x355F, 0x18},
      {0x3068, 0x19},
      {0x3F31, 0x1A},
      {0x3A06, 0x1B},
      {0x24B4, 0x1C},
      {0x2183, 0x1D},
      {0x2EDA, 0x1E},
      {0x2BED, 0x1F},
  };

  /**
   * Offset i holds the number of 1 bits in the binary representation of i
   */
  private static final int[] BITS_SET_IN_HALF_BYTE =
      new int[]{0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4};

  private final ErrorCorrectionLevel errorCorrectionLevel;
  private final byte dataMask;

  private FormatInformation(int formatInfo) throws ReaderException {
    // Bits 3,4
    errorCorrectionLevel = ErrorCorrectionLevel.forBits((formatInfo >> 3) & 0x03);
    // Bottom 3 bits
    dataMask = (byte) (formatInfo & 0x07);
  }

  static int numBitsDiffering(int a, int b) {
    a ^= b; // a now has a 1 bit exactly where its bit differs with b's
    // Count bits set quickly with a series of lookups:
    return BITS_SET_IN_HALF_BYTE[a & 0x0F] +
        BITS_SET_IN_HALF_BYTE[(a >>> 4 & 0x0F)] +
        BITS_SET_IN_HALF_BYTE[(a >>> 8 & 0x0F)] +
        BITS_SET_IN_HALF_BYTE[(a >>> 12 & 0x0F)] +
        BITS_SET_IN_HALF_BYTE[(a >>> 16 & 0x0F)] +
        BITS_SET_IN_HALF_BYTE[(a >>> 20 & 0x0F)] +
        BITS_SET_IN_HALF_BYTE[(a >>> 24 & 0x0F)] +
        BITS_SET_IN_HALF_BYTE[(a >>> 28 & 0x0F)];
  }

  /**
   * @param rawFormatInfo
   * @return
   */
  static FormatInformation decodeFormatInformation(int rawFormatInfo) throws ReaderException {
    FormatInformation formatInfo = doDecodeFormatInformation(rawFormatInfo);
    if (formatInfo != null) {
      return formatInfo;
    }
    // Should return null, but, some QR codes apparently
    // do not mask this info. Try again, first masking the raw bits so
    // the function will unmask
    return doDecodeFormatInformation(rawFormatInfo ^ FORMAT_INFO_MASK_QR);
  }

  private static FormatInformation doDecodeFormatInformation(int rawFormatInfo) throws ReaderException {
    // Unmask:
    int unmaskedFormatInfo = rawFormatInfo ^ FORMAT_INFO_MASK_QR;
    // Find the int in FORMAT_INFO_DECODE_LOOKUP with fewest bits differing
    int bestDifference = Integer.MAX_VALUE;
    int bestFormatInfo = 0;
    for (int i = 0; i < FORMAT_INFO_DECODE_LOOKUP.length; i++) {
      int[] decodeInfo = FORMAT_INFO_DECODE_LOOKUP[i];
      int targetInfo = decodeInfo[0];
      if (targetInfo == unmaskedFormatInfo) {
        // Found an exact match
        return new FormatInformation(decodeInfo[1]);
      }
      int bitsDifference = numBitsDiffering(unmaskedFormatInfo, targetInfo);
      if (bitsDifference < bestDifference) {
        bestFormatInfo = decodeInfo[1];
        bestDifference = bitsDifference;
      }
    }
    if (bestDifference <= 3) {
      return new FormatInformation(bestFormatInfo);
    }
    return null;
  }

  ErrorCorrectionLevel getErrorCorrectionLevel() {
    return errorCorrectionLevel;
  }

  byte getDataMask() {
    return dataMask;
  }

  public int hashCode() {
    return (errorCorrectionLevel.ordinal() << 3) | (int) dataMask;
  }

  public boolean equals(Object o) {
    if (!(o instanceof FormatInformation)) {
      return false;
    }
    FormatInformation other = (FormatInformation) o;
    return this.errorCorrectionLevel == other.errorCorrectionLevel &&
        this.dataMask == other.dataMask;
  }

}
