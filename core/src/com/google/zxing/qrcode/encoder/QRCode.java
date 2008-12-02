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

package com.google.zxing.qrcode.encoder;

import com.google.zxing.common.ByteMatrix;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class QRCode {

  // Magic numbers.
  private static final int MIN_VERSION = 1;
  private static final int MAX_VERSION = 40;
  // For matrix width, see 7.3.1 of JISX0510:2004 (p.5).
  private static final int MIN_MATRIX_WIDTH = 21;  // Version 1
  private static final int MAX_MATRIX_WIDTH = 177;  // Version 40 (21 + 4 * (40 -1)).
  public static final int NUM_MASK_PATTERNS = 8;

  // See table 3 of JISX0510:2004 (p.16)
  private static final int[][] NUM_BITS_TABLE = {
      // NUMERIC  ALPHANUMERIC  8BIT_BYTE  KANJI
      {       10,            9,         8,     8 },  // Version 1-9
      {       12,           11,        16,    10 },  // Version 10-26
      {       14,           13,        16,    12 },  // Version 27-40
  };

  private int mode;
  private ErrorCorrectionLevel ecLevel;
  private int version;
  private int matrixWidth;
  private int maskPattern;
  private int numTotalBytes;
  private int numDataBytes;
  private int numECBytes;
  private int numRSBlocks;
  private ByteMatrix matrix;


  // They call encoding "mode". The modes are defined in 8.3 of JISX0510:2004 (p.14). It's unlikely
  // (probably we will not support complicated modes) but if you add an item to this, please also
  // add it to modeToString(), getModeCode(), getNumBitsForLength(), Encoder.appendBytes(), and
  // Encoder.chooseMode().
  //
  // JAVAPORT: These used to be C++ enums, but the code evaluates them as integers, and requires
  // negative values. I don't want to take the ParsedResultType approach of a class full of statics
  // of that class's type. The best compromise here is integer constants.
  //
  // Formerly enum Mode
  public static final int MODE_UNDEFINED = -1;
  public static final int MODE_NUMERIC = 0;
  public static final int MODE_ALPHANUMERIC = 1;
  public static final int MODE_8BIT_BYTE = 2;
  public static final int MODE_KANJI = 3;  // Shift_JIS
  // The following modes are unimplemented.
  // MODE_ECI,
  // MODE_MIXED,
  // MODE_CONCATENATED,
  // MODE_FNC1,
  public static final int NUM_MODES = 4;

  public QRCode() {
    mode = MODE_UNDEFINED;
    ecLevel = null;
    version = -1;
    matrixWidth = -1;
    maskPattern = -1;
    numTotalBytes = -1;
    numDataBytes = -1;
    numECBytes = -1;
    numRSBlocks = -1;
    matrix = null;
  }

  // Mode of the QR Code.
  public int getMode() {
    return mode;
  }

  // Error correction level of the QR Code.
  public ErrorCorrectionLevel getECLevel() {
    return ecLevel;
  }

  // Version of the QR Code.  The bigger size, the bigger version.
  public int getVersion() {
    return version;
  }

  // ByteMatrix width of the QR Code.
  public int getMatrixWidth() {
    return matrixWidth;
  }

  // Mask pattern of the QR Code.
  public int getMaskPattern() {
    return maskPattern;
  }

  // Number of total bytes in the QR Code.
  public int getNumTotalBytes() {
    return numTotalBytes;
  }

  // Number of data bytes in the QR Code.
  public int getNumDataBytes() {
    return numDataBytes;
  }

  // Number of error correction bytes in the QR Code.
  public int getNumECBytes() {
    return numECBytes;
  }

  // Number of Reedsolomon blocks in the QR Code.
  public int getNumRSBlocks() {
    return numRSBlocks;
  }

  // ByteMatrix data of the QR Code.
  public final ByteMatrix getMatrix() {
    return matrix;
  }
  

  // Return the value of the module (cell) pointed by "x" and "y" in the matrix of the QR Code. They
  // call cells in the matrix "modules". 1 represents a black cell, and 0 represents a white cell.
  public int at(int x, int y) {
    // The value must be zero or one.
    int value = matrix.get(y, x);
    if (!(value == 0 || value == 1)) {
      // this is really like an assert... not sure what better exception to use?
      throw new RuntimeException("Bad value");
    }
    return value;
  }

  // Checks all the member variables are set properly. Returns true on success. Otherwise, returns
  // false.
  public boolean isValid() {
    return (
        // First check if all version are not uninitialized.
        mode != MODE_UNDEFINED &&
            ecLevel != null &&
            version != -1 &&
            matrixWidth != -1 &&
            maskPattern != -1 &&
            numTotalBytes != -1 &&
            numDataBytes != -1 &&
            numECBytes != -1 &&
            numRSBlocks != -1 &&
            // Then check them in other ways..
            isValidVersion(version) &&
            isValidMode(mode) &&
            isValidMatrixWidth(matrixWidth) &&
            isValidMaskPattern(maskPattern) &&
            numTotalBytes == numDataBytes + numECBytes &&
            // ByteMatrix stuff.
            matrix != null &&
            matrixWidth == matrix.width() &&
            // See 7.3.1 of JISX0510:2004 (p.5).
            matrixWidth == MIN_MATRIX_WIDTH + (version - 1) * 4 &&
            matrix.width() == matrix.height()); // Must be square.
  }

  // Return debug String.
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("<<\n");
    result.append(" mode: ");
    result.append(modeToString(mode));
    result.append("\n ecLevel: ");
    result.append(ecLevel);
    result.append("\n version: ");
    result.append(version);
    result.append("\n matrixWidth: ");
    result.append(matrixWidth);
    result.append("\n maskPattern: ");
    result.append(maskPattern);
    result.append("\n numTotalBytes: ");
    result.append(numTotalBytes);
    result.append("\n numDataBytes: ");
    result.append(numDataBytes);
    result.append("\n numECBytes: ");
    result.append(numECBytes);
    result.append("\n numRSBlocks: ");
    result.append(numRSBlocks);
    if (matrix == null) {
      result.append("\n matrix: null\n");
    } else {
      result.append("\n matrix:\n");
      result.append(matrix.toString());
    }
    result.append(">>\n");
    return result.toString();
  }

  public void setMode(int value) {
    mode = value;
  }

  public void setECLevel(ErrorCorrectionLevel value) {
    ecLevel = value;
  }

  public void setVersion(int value) {
    version = value;
  }

  public void setMatrixWidth(int value) {
    matrixWidth = value;
  }

  public void setMaskPattern(int value) {
    maskPattern = value;
  }

  public void setNumTotalBytes(int value) {
    numTotalBytes = value;
  }

  public void setNumDataBytes(int value) {
    numDataBytes = value;
  }

  public void setNumECBytes(int value) {
    numECBytes = value;
  }

  public void setNumRSBlocks(int value) {
    numRSBlocks = value;
  }

  // This takes ownership of the 2D array.
  public void setMatrix(ByteMatrix value) {
    matrix = value;
  }

  // Check if "version" is valid.
  public static boolean isValidVersion(final int version) {
    return version >= MIN_VERSION && version <= MAX_VERSION;
  }

  // Check if "mode" is valid.
  public static boolean isValidMode(final int mode) {
    return mode >= 0 && mode < NUM_MODES;
  }

  // Check if "width" is valid.
  public static boolean isValidMatrixWidth(int width) {
    return width >= MIN_MATRIX_WIDTH && width <= MAX_MATRIX_WIDTH;
  }

  // Check if "mask_pattern" is valid.
  public static boolean isValidMaskPattern(int maskPattern) {
    return maskPattern >= 0 && maskPattern < NUM_MASK_PATTERNS;
  }

  // Convert "mode" to String for debugging.
  public static String modeToString(int mode) {
    switch (mode) {
      case QRCode.MODE_UNDEFINED:
        return "UNDEFINED";
      case QRCode.MODE_NUMERIC:
        return "NUMERIC";
      case QRCode.MODE_ALPHANUMERIC:
        return "ALPHANUMERIC";
      case QRCode.MODE_8BIT_BYTE:
        return "8BIT_BYTE";
      case QRCode.MODE_KANJI:
        return "KANJI";
      default:
        break;
    }
    return "UNKNOWN";
  }

  // Return the code of mode. On error, return -1. The codes of modes are defined in the table 2 of
  // JISX0510:2004 (p.16).
  public static int getModeCode(final int mode) throws WriterException {
    switch (mode) {
      case QRCode.MODE_NUMERIC:
        return 1;
      case QRCode.MODE_ALPHANUMERIC:
        return 2;
      case QRCode.MODE_8BIT_BYTE:
        return 4;
      case QRCode.MODE_KANJI:
        return 8;
      default:
        throw new WriterException("Unknown mode: " + mode);
    }
  }

  // Return the number of bits needed for representing the length info of QR Code with "version" and
  // "mode". On error, return -1.
  static int getNumBitsForLength(int version, int mode) {
    if (!isValidVersion(version)) {
      throw new IllegalArgumentException("Invalid version: " + version);
    }
    if (!isValidMode(mode)) {
      throw new IllegalArgumentException("Invalid mode: " + mode);
    }
    if (version >= 1 && version <= 9) {
      return NUM_BITS_TABLE[0][mode];
    } else if (version >= 10 && version <= 26) {
      return NUM_BITS_TABLE[1][mode];
    } else if (version >= 27 && version <= 40) {
      return NUM_BITS_TABLE[2][mode];
    }
    throw new IllegalArgumentException("Bad version: " + version);
  }

  // Return true if the all values in the matrix are binary numbers.
  //
  // JAVAPORT: This is going to be super expensive and unnecessary, we should not call this in
  // production. I'm leaving it because it may be useful for testing. It should be removed entirely
  // if ByteMatrix is changed never to contain a -1.
  /*
  private static boolean EverythingIsBinary(final ByteMatrix matrix) {
    for (int y = 0; y < matrix.height(); ++y) {
      for (int x = 0; x < matrix.width(); ++x) {
        int value = matrix.get(y, x);
        if (!(value == 0 || value == 1)) {
          // Found non zero/one value.
          return false;
        }
      }
    }
    return true;
  }
   */

}
