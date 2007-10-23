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
import com.google.zxing.common.BitMatrix;

/**
 * @author srowen@google.com (Sean Owen)
 */
final class BitMatrixParser {

  private final BitMatrix bitMatrix;
  private Version parsedVersion;
  private FormatInformation parsedFormatInfo;

  /**
   * @throws com.google.zxing.ReaderException
   *          if dimension is not >= 21 and 1 mod 4
   */
  BitMatrixParser(BitMatrix bitMatrix) throws ReaderException {
    int dimension = bitMatrix.getDimension();
    if (dimension < 21 || (dimension & 0x03) != 1) {
      throw new ReaderException("Dimension must be 1 mod 4 and >= 21");
    }
    this.bitMatrix = bitMatrix;
  }

  FormatInformation readFormatInformation() throws ReaderException {

    if (parsedFormatInfo != null) {
      return parsedFormatInfo;
    }

    // Read top-left format info bits
    int formatInfoBits = 0;
    for (int j = 0; j < 6; j++) {
      formatInfoBits = copyBit(8, j, formatInfoBits);
    }
    // .. and skip a bit in the timing pattern ...
    formatInfoBits = copyBit(8, 7, formatInfoBits);
    formatInfoBits = copyBit(8, 8, formatInfoBits);
    formatInfoBits = copyBit(7, 8, formatInfoBits);
    // .. and skip a bit in the timing pattern ...
    for (int i = 5; i >= 0; i--) {
      formatInfoBits = copyBit(i, 8, formatInfoBits);
    }

    parsedFormatInfo = FormatInformation.decodeFormatInformation(formatInfoBits);
    if (parsedFormatInfo != null) {
      return parsedFormatInfo;
    }

    // Hmm, failed. Try the top-right/bottom-left pattern
    int dimension = bitMatrix.getDimension();
    formatInfoBits = 0;
    int iMin = dimension - 8;
    for (int i = dimension - 1; i >= iMin; i--) {
      formatInfoBits = copyBit(i, 8, formatInfoBits);
    }
    for (int j = dimension - 7; j < dimension; j++) {
      formatInfoBits = copyBit(8, j, formatInfoBits);
    }

    parsedFormatInfo = FormatInformation.decodeFormatInformation(formatInfoBits);
    if (parsedFormatInfo != null) {
      return parsedFormatInfo;
    }
    throw new ReaderException("Could not decode format information");
  }

  Version readVersion() throws ReaderException {

    if (parsedVersion != null) {
      return parsedVersion;
    }

    int dimension = bitMatrix.getDimension();

    int provisionalVersion = (dimension - 17) >> 2;
    if (provisionalVersion <= 6) {
      return Version.getVersionForNumber(provisionalVersion);
    }

    // Read top-right version info: 3 wide by 6 tall
    int versionBits = 0;
    for (int i = 5; i >= 0; i--) {
      int jMin = dimension - 11;
      for (int j = dimension - 9; j >= jMin; j--) {
        versionBits = copyBit(i, j, versionBits);
      }
    }

    parsedVersion = Version.decodeVersionInformation(versionBits);
    if (parsedVersion != null) {
      return parsedVersion;
    }

    // Hmm, failed. Try bottom left: 6 wide by 3 tall
    versionBits = 0;
    for (int j = 5; j >= 0; j--) {
      int iMin = dimension - 11;
      for (int i = dimension - 11; i >= iMin; i--) {
        versionBits = copyBit(i, j, versionBits);
      }
    }

    parsedVersion = Version.decodeVersionInformation(versionBits);
    if (parsedVersion != null) {
      return parsedVersion;
    }
    throw new ReaderException("Could not decode version");
  }

  private int copyBit(int i, int j, int versionBits) {
    return bitMatrix.get(i, j) ? (versionBits << 1) | 0x1 : versionBits << 1;
  }

  byte[] readCodewords() throws ReaderException {

    FormatInformation formatInfo = readFormatInformation();
    Version version = readVersion();

    DataMask dataMask = DataMask.forReference((int) formatInfo.getDataMask());
    int dimension = bitMatrix.getDimension();
    dataMask.unmaskBitMatrix(bitMatrix.getBits(), dimension);

    BitMatrix functionPattern = version.buildFunctionPattern();

    boolean readingUp = true;
    byte[] result = new byte[version.getTotalCodewords()];
    int resultOffset = 0;
    int currentByte = 0;
    int bitsRead = 0;
    for (int j = dimension - 1; j > 0; j -= 2) {
      if (j == 6) {
        // Skip whole column with vertical alignment pattern;
        // saves time and makes the other code proceed more cleanly
        j--;
      }
      for (int count = 0; count < dimension; count++) {
        int i = readingUp ? dimension - 1 - count : count;
        for (int col = 0; col < 2; col++) {
          if (!functionPattern.get(i, j - col)) {
            bitsRead++;
            currentByte <<= 1;
            if (bitMatrix.get(i, j - col)) {
              currentByte |= 1;
            }
            if (bitsRead == 8) {
              result[resultOffset++] = (byte) currentByte;
              bitsRead = 0;
              currentByte = 0;
            }
          }
        }
      }
      readingUp = !readingUp; // switch directions
    }
    if (resultOffset != version.getTotalCodewords()) {
      throw new ReaderException("Did not read all codewords");
    }
    return result;
  }

}