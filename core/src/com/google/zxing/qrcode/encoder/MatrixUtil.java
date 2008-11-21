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

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class MatrixUtil {

  private static final int kPositionDetectionPattern[][] =  {
      {1, 1, 1, 1, 1, 1, 1},
      {1, 0, 0, 0, 0, 0, 1},
      {1, 0, 1, 1, 1, 0, 1},
      {1, 0, 1, 1, 1, 0, 1},
      {1, 0, 1, 1, 1, 0, 1},
      {1, 0, 0, 0, 0, 0, 1},
      {1, 1, 1, 1, 1, 1, 1},
  };

  private static final int kHorizontalSeparationPattern[][] = {
      {0, 0, 0, 0, 0, 0, 0, 0},
  };

  private static final int kVerticalSeparationPattern[][] = {
      {0}, {0}, {0}, {0}, {0}, {0}, {0},
  };

  private static final int kPositionAdjustmentPattern[][] = {
      {1, 1, 1, 1, 1},
      {1, 0, 0, 0, 1},
      {1, 0, 1, 0, 1},
      {1, 0, 0, 0, 1},
      {1, 1, 1, 1, 1},
  };

  // From Appendix E. Table 1, JIS0510X:2004 (p 71). The table was double-checked by komatsu.
  private static final int kPositionAdjustmentPatternCoordinateTable[][] = {
      {-1, -1, -1, -1,  -1,  -1,  -1},  // Version 1
      { 6, 18, -1, -1,  -1,  -1,  -1},  // Version 2
      { 6, 22, -1, -1,  -1,  -1,  -1},  // Version 3
      { 6, 26, -1, -1,  -1,  -1,  -1},  // Version 4
      { 6, 30, -1, -1,  -1,  -1,  -1},  // Version 5
      { 6, 34, -1, -1,  -1,  -1,  -1},  // Version 6
      { 6, 22, 38, -1,  -1,  -1,  -1},  // Version 7
      { 6, 24, 42, -1,  -1,  -1,  -1},  // Version 8
      { 6, 26, 46, -1,  -1,  -1,  -1},  // Version 9
      { 6, 28, 50, -1,  -1,  -1,  -1},  // Version 10
      { 6, 30, 54, -1,  -1,  -1,  -1},  // Version 11
      { 6, 32, 58, -1,  -1,  -1,  -1},  // Version 12
      { 6, 34, 62, -1,  -1,  -1,  -1},  // Version 13
      { 6, 26, 46, 66,  -1,  -1,  -1},  // Version 14
      { 6, 26, 48, 70,  -1,  -1,  -1},  // Version 15
      { 6, 26, 50, 74,  -1,  -1,  -1},  // Version 16
      { 6, 30, 54, 78,  -1,  -1,  -1},  // Version 17
      { 6, 30, 56, 82,  -1,  -1,  -1},  // Version 18
      { 6, 30, 58, 86,  -1,  -1,  -1},  // Version 19
      { 6, 34, 62, 90,  -1,  -1,  -1},  // Version 20
      { 6, 28, 50, 72,  94,  -1,  -1},  // Version 21
      { 6, 26, 50, 74,  98,  -1,  -1},  // Version 22
      { 6, 30, 54, 78, 102,  -1,  -1},  // Version 23
      { 6, 28, 54, 80, 106,  -1,  -1},  // Version 24
      { 6, 32, 58, 84, 110,  -1,  -1},  // Version 25
      { 6, 30, 58, 86, 114,  -1,  -1},  // Version 26
      { 6, 34, 62, 90, 118,  -1,  -1},  // Version 27
      { 6, 26, 50, 74,  98, 122,  -1},  // Version 28
      { 6, 30, 54, 78, 102, 126,  -1},  // Version 29
      { 6, 26, 52, 78, 104, 130,  -1},  // Version 30
      { 6, 30, 56, 82, 108, 134,  -1},  // Version 31
      { 6, 34, 60, 86, 112, 138,  -1},  // Version 32
      { 6, 30, 58, 86, 114, 142,  -1},  // Version 33
      { 6, 34, 62, 90, 118, 146,  -1},  // Version 34
      { 6, 30, 54, 78, 102, 126, 150},  // Version 35
      { 6, 24, 50, 76, 102, 128, 154},  // Version 36
      { 6, 28, 54, 80, 106, 132, 158},  // Version 37
      { 6, 32, 58, 84, 110, 136, 162},  // Version 38
      { 6, 26, 54, 82, 110, 138, 166},  // Version 39
      { 6, 30, 58, 86, 114, 142, 170},  // Version 40
  };

  // Type info cells at the left top corner.
  private static final int[][] kTypeInfoCoordinates = {
      {8, 0},
      {8, 1},
      {8, 2},
      {8, 3},
      {8, 4},
      {8, 5},
      {8, 7},
      {8, 8},
      {7, 8},
      {5, 8},
      {4, 8},
      {3, 8},
      {2, 8},
      {1, 8},
      {0, 8},
  };

  // From Appendix D in JISX0510:2004 (p. 67)
  private static final int kVersionInfoPoly = 0x1f25;  // 1 1111 0010 0101

  // From Appendix C in JISX0510:2004 (p.65).
  private static final int kTypeInfoPoly = 0x537;
  private static final int kTypeInfoMaskPattern = 0x5412;

  // Set all cells to -1.  -1 means that the cell is empty (not set yet).
  //
  // JAVAPORT: We shouldn't need to do this at all. The code should be rewritten to begin encoding
  // with the ByteMatrix initialized all to zero.
  public static void ClearMatrix(ByteMatrix matrix) {
    matrix.clear((byte) -1);
  }

  // Build 2D matrix of QR Code from "data_bits" with "ec_level", "version" and "mask_pattern". On
  // success, store the result in "matrix" and return true.  On error, return false.
  public static void BuildMatrix(final BitVector data_bits, int ec_level, int version,
      int mask_pattern, ByteMatrix matrix) throws WriterException {
    MatrixUtil.ClearMatrix(matrix);
    EmbedBasicPatterns(version, matrix);
    // Type information appear with any version.
    EmbedTypeInfo(ec_level, mask_pattern, matrix);
    // Version info appear if version >= 7.
    MaybeEmbedVersionInfo(version, matrix);
    // Data should be embedded at end.
    EmbedDataBits(data_bits, mask_pattern, matrix);
  }

  // Embed basic patterns. On success, modify the matrix and return true. On error, return false.
  // The basic patterns are:
  // - Position detection patterns
  // - Timing patterns
  // - Dark dot at the left bottom corner
  // - Position adjustment patterns, if need be
  public static void EmbedBasicPatterns(int version, ByteMatrix matrix) throws WriterException {
    // Let's get started with embedding big squares at corners.
    EmbedPositionDetectionPatternsAndSeparators(matrix);
    // Then, embed the dark dot at the left bottom corner.
    EmbedDarkDotAtLeftBottomCorner(matrix);

    // Position adjustment patterns appear if version >= 2.
    MaybeEmbedPositionAdjustmentPatterns(version, matrix);
    // Timing patterns should be embedded after position adj. patterns.
    EmbedTimingPatterns(matrix);
  }

  // Embed type information. On success, modify the matrix.
  public static void EmbedTypeInfo(int ec_level, int mask_pattern, ByteMatrix matrix) throws WriterException {
    BitVector type_info_bits = new BitVector();
    MakeTypeInfoBits(ec_level, mask_pattern, type_info_bits);

    for (int i = 0; i < type_info_bits.size(); ++i) {
      // Place bits in LSB to MSB order.  LSB (least significant bit) is the last value in
      // "type_info_bits".
      final int bit = type_info_bits.at(type_info_bits.size() - 1 - i);

      // Type info bits at the left top corner. See 8.9 of JISX0510:2004 (p.46).
      final int x1 = kTypeInfoCoordinates[i][0];
      final int y1 = kTypeInfoCoordinates[i][1];
      matrix.set(y1, x1, bit);

      if (i < 8) {
        // Right top corner.
        final int x2 = matrix.width() - i - 1;
        final int y2 = 8;
        matrix.set(y2, x2, bit);
      } else {
        // Left bottom corner.
        final int x2 = 8;
        final int y2 = matrix.height() - 7 + (i - 8);
        matrix.set(y2, x2, bit);
      }
    }
  }

  // Embed version information if need be. On success, modify the matrix and return true. On error,
  // return false. See 8.10 of JISX0510:2004 (p.47) for how to embed version information. Return
  // true on success, otherwise return false.
  public static void MaybeEmbedVersionInfo(int version, ByteMatrix matrix) throws WriterException {
    if (version < 7) {  // Version info is necessary if version >= 7.
      return;  // Don't need version info.
    }
    BitVector version_info_bits = new BitVector();
    MakeVersionInfoBits(version, version_info_bits);

    int bit_index = 6 * 3 - 1;  // It will decrease from 17 to 0.
    for (int i = 0; i < 6; ++i) {
      for (int j = 0; j < 3; ++j) {
        // Place bits in LSB (least significant bit) to MSB order.
        final int bit = version_info_bits.at(bit_index);
        bit_index--;
        // Left bottom corner.
        matrix.set(matrix.height() - 11 + j, i, bit);
        // Right bottom corner.
        matrix.set(i, matrix.height() - 11 + j, bit);
      }
    }
  }

  // Embed "data_bits" using "mask_pattern". On success, modify the matrix and return true. On
  // error, return false. For debugging purposes, it skips masking process if "mask_pattern" is -1.
  // See 8.7 of JISX0510:2004 (p.38) for how to embed data bits.
  public static void EmbedDataBits(final BitVector data_bits, int mask_pattern, ByteMatrix matrix)
      throws WriterException {
    int bit_index = 0;
    int direction = -1;
    // Start from the right bottom cell.
    int x = matrix.width() - 1;
    int y = matrix.height() - 1;
    while (x > 0) {
      // Skip the vertical timing pattern.
      if (x == 6) {
        x -= 1;
      }
      while (y >= 0 && y < matrix.height()) {
        for (int i = 0; i < 2; ++i) {
          final int xx = x - i;
          // Skip the cell if it's not empty.
          if (!IsEmpty(matrix.get(y, xx))) {
            continue;
          }
          int bit;
          if (bit_index < data_bits.size()) {
            bit = data_bits.at(bit_index);
            ++bit_index;
          } else {
            // Padding bit. If there is no bit left, we'll fill the left cells with 0, as described
            // in 8.4.9 of JISX0510:2004 (p. 24).
            bit = 0;
          }

          // Skip masking if mask_pattern is -1.
          if (mask_pattern != -1) {
            final int mask = MaskUtil.GetDataMaskBit(mask_pattern, xx, y);
            bit ^= mask;
          }
          matrix.set(y, xx, bit);
        }
        y += direction;
      }
      direction = -direction;  // Reverse the direction.
      y += direction;
      x -= 2;  // Move to the left.
    }
    // All bits should be consumed.
    if (bit_index != data_bits.size()) {
      throw new WriterException("Not all bits consumed: " + bit_index + "/" + data_bits.size());
    }
  }

  // Return the position of the most significant bit set (to one) in the "value". The most
  // significant bit is position 32. If there is no bit set, return 0. Examples:
  // - FindMSBSet(0) => 0
  // - FindMSBSet(1) => 1
  // - FindMSBSet(255) => 8
  public static int FindMSBSet(int value) {
    int num_digits = 0;
    while (value != 0) {
      value >>>= 1;
      ++num_digits;
    }
    return num_digits;
  }

  // Calculate BCH (Bose-Chaudhuri-Hocquenghem) code for "value" using polynomial "poly". The BCH
  // code is used for encoding type information and version information.
  // Example: Calculation of version information of 7.
  // f(x) is created from 7.
  //   - 7 = 000111 in 6 bits
  //   - f(x) = x^2 + x^2 + x^1
  // g(x) is given by the standard (p. 67)
  //   - g(x) = x^12 + x^11 + x^10 + x^9 + x^8 + x^5 + x^2 + 1
  // Multiply f(x) by x^(18 - 6)
  //   - f'(x) = f(x) * x^(18 - 6)
  //   - f'(x) = x^14 + x^13 + x^12
  // Calculate the remainder of f'(x) / g(x)
  //         x^2
  //         __________________________________________________
  //   g(x) )x^14 + x^13 + x^12
  //         x^14 + x^13 + x^12 + x^11 + x^10 + x^7 + x^4 + x^2
  //         --------------------------------------------------
  //                              x^11 + x^10 + x^7 + x^4 + x^2
  //
  // The remainder is x^11 + x^10 + x^7 + x^4 + x^2
  // Encode it in binary: 110010010100
  // The return value is 0xc94 (1100 1001 0100)
  //
  // Since all coefficients in the polynomials are 1 or 0, we can do the calculation by bit
  // operations. We don't care if cofficients are positive or negative.
  public static int CalculateBCHCode(int value, int poly) {
    // If poly is "1 1111 0010 0101" (version info poly), msb_set_in_poly is 13. We'll subtract 1
    // from 13 to make it 12.
    final int msb_set_in_poly = FindMSBSet(poly);
    value <<= msb_set_in_poly - 1;
    // Do the division business using exclusive-or operations.
    while (FindMSBSet(value) >= msb_set_in_poly) {
      value ^= poly << (FindMSBSet(value) - msb_set_in_poly);
    }
    // Now the "value" is the remainder (i.e. the BCH code)
    return value;
  }

  // Make bit vector of type information. On success, store the result in "bits" and return true.
  // On error, return false. Encode error correction level and mask pattern. See 8.9 of
  // JISX0510:2004 (p.45) for details.
  public static void MakeTypeInfoBits(int ec_level, final int mask_pattern, BitVector bits) throws WriterException {
    final int ec_code = QRCode.GetECLevelCode(ec_level);
    if (!QRCode.IsValidMaskPattern(mask_pattern)) {
      throw new WriterException("Invalid mask pattern");
    }
    final int type_info = (ec_code << 3) | mask_pattern;
    bits.appendBits(type_info, 5);

    final int bch_code = MatrixUtil.CalculateBCHCode(type_info, kTypeInfoPoly);
    bits.appendBits(bch_code, 10);

    BitVector mask_bits = new BitVector();
    mask_bits.appendBits(kTypeInfoMaskPattern, 15);
    bits.xor(mask_bits);

    if (bits.size() != 15) {  // Just in case.
      throw new WriterException("should not happen but we got: " + bits.size());
    }
  }

  // Make bit vector of version information. On success, store the result in "bits" and return true.
  // On error, return false. See 8.10 of JISX0510:2004 (p.45) for details.
  public static void MakeVersionInfoBits(int version, BitVector bits) throws WriterException {
    bits.appendBits(version, 6);
    final int bch_code = MatrixUtil.CalculateBCHCode(version, kVersionInfoPoly);
    bits.appendBits(bch_code, 12);

    if (bits.size() != 18) {  // Just in case.
      throw new WriterException("should not happen but we got: " + bits.size());
    }
  }

  // Check if "value" is empty.
  private static boolean IsEmpty(final int value) {
    return value == -1;
  }

  // Check if "value" is valid.
  private static boolean IsValidValue(final int value) {
    return (value == -1 ||  // Empty.
        value == 0 ||  // Light (white).
        value == 1);  // Dark (black).
  }

  private static void EmbedTimingPatterns(ByteMatrix matrix) throws WriterException {
    // -8 is for skipping position detection patterns (size 7), and two horizontal/vertical
    // separation patterns (size 1). Thus, 8 = 7 + 1.
    for (int i = 8; i < matrix.width() - 8; ++i) {
      final int bit = (i + 1) % 2;
      // Horizontal line.
      if (!IsValidValue(matrix.get(6, i))) {
        throw new WriterException();
      }
      if (IsEmpty(matrix.get(6, i))) {
        matrix.set(6, i, bit);
      }
      // Vertical line.
      if (!IsValidValue(matrix.get(i, 6))) {
        throw new WriterException();
      }
      if (IsEmpty(matrix.get(i, 6))) {
        matrix.set(i, 6, bit);
      }
    }
  }

  // Embed the lonely dark dot at left bottom corner. JISX0510:2004 (p.46)
  private static void EmbedDarkDotAtLeftBottomCorner(ByteMatrix matrix) throws WriterException {
    if (matrix.get(matrix.height() - 8, 8) == 0) {
      throw new WriterException();
    }
    matrix.set(matrix.height() - 8, 8, 1);
  }

  private static void EmbedHorizontalSeparationPattern(final int x_start, final int y_start,
      ByteMatrix matrix) throws WriterException {
    // We know the width and height.
    if (kHorizontalSeparationPattern[0].length != 8 || kHorizontalSeparationPattern.length != 1) {
      throw new WriterException("Bad horizontal separation pattern");
    }
    for (int x = 0; x < 8; ++x) {
      if (!IsEmpty(matrix.get(y_start, x_start + x))) {
        throw new WriterException();
      }
      matrix.set(y_start, x_start + x, kHorizontalSeparationPattern[0][x]);
    }
  }

  private static void EmbedVerticalSeparationPattern(final int x_start, final int y_start,
      ByteMatrix matrix) throws WriterException {
    // We know the width and height.
    if (kVerticalSeparationPattern[0].length != 1 || kVerticalSeparationPattern.length != 7) {
      throw new WriterException("Bad vertical separation pattern");
    }
    for (int y = 0; y < 7; ++y) {
      if (!IsEmpty(matrix.get(y_start + y, x_start))) {
        throw new WriterException();
      }
      matrix.set(y_start + y, x_start, kVerticalSeparationPattern[y][0]);
    }
  }

  // Note that we cannot unify the function with EmbedPositionDetectionPattern() despite they are
  // almost identical, since we cannot write a function that takes 2D arrays in different sizes in
  // C/C++. We should live with the fact.
  private static void EmbedPositionAdjustmentPattern(final int x_start, final int y_start,
      ByteMatrix matrix) throws WriterException {
    // We know the width and height.
    if (kPositionAdjustmentPattern[0].length != 5 || kPositionAdjustmentPattern.length != 5) {
      throw new WriterException("Bad position adjustment");
    }
    for (int y = 0; y < 5; ++y) {
      for (int x = 0; x < 5; ++x) {
        if (!IsEmpty(matrix.get(y_start + y, x_start + x))) {
          throw new WriterException();
        }
        matrix.set(y_start + y, x_start + x, kPositionAdjustmentPattern[y][x]);
      }
    }
  }

  private static void EmbedPositionDetectionPattern(final int x_start, final int y_start,
      ByteMatrix matrix) throws WriterException {
    // We know the width and height.
    if (kPositionDetectionPattern[0].length != 7 || kPositionDetectionPattern.length != 7) {
      throw new WriterException("Bad position detection pattern");
    }
    for (int y = 0; y < 7; ++y) {
      for (int x = 0; x < 7; ++x) {
        if (!IsEmpty(matrix.get(y_start + y, x_start + x))) {
          throw new WriterException();
        }
        matrix.set(y_start + y, x_start + x, kPositionDetectionPattern[y][x]);
      }
    }
  }

  // Embed position detection patterns and surrounding vertical/horizontal separators.
  private static void EmbedPositionDetectionPatternsAndSeparators(ByteMatrix matrix) throws WriterException {
    // Embed three big squares at corners.
    final int pdp_width = kPositionDetectionPattern[0].length;
    // Left top corner.
    EmbedPositionDetectionPattern(0, 0, matrix);
    // Right top corner.
    EmbedPositionDetectionPattern(matrix.width() - pdp_width, 0, matrix);
    // Left bottom corner.
    EmbedPositionDetectionPattern(0, matrix.width() - pdp_width, matrix);

    // Embed horizontal separation patterns around the squares.
    final int hsp_width = kHorizontalSeparationPattern[0].length;
    // Left top corner.
    EmbedHorizontalSeparationPattern(0, hsp_width - 1, matrix);
    // Right top corner.
    EmbedHorizontalSeparationPattern(matrix.width() - hsp_width,
        hsp_width - 1, matrix);
    // Left bottom corner.
    EmbedHorizontalSeparationPattern(0, matrix.width() - hsp_width, matrix);

    // Embed vertical separation patterns around the squares.
    final int vsp_size = kVerticalSeparationPattern.length;
    // Left top corner.
    EmbedVerticalSeparationPattern(vsp_size, 0, matrix);
    // Right top corner.
    EmbedVerticalSeparationPattern(matrix.height() - vsp_size - 1, 0, matrix);
    // Left bottom corner.
    EmbedVerticalSeparationPattern(vsp_size, matrix.height() - vsp_size,
        matrix);
  }

  // Embed position adjustment patterns if need be.
  private static void MaybeEmbedPositionAdjustmentPatterns(final int version, ByteMatrix matrix) throws WriterException {
    if (version < 2) {  // The patterns appear if version >= 2
      return;
    }
    final int index = version - 1;
    final int[] coordinates = kPositionAdjustmentPatternCoordinateTable[index];
    final int num_coordinates = kPositionAdjustmentPatternCoordinateTable[index].length;
    for (int i = 0; i < num_coordinates; ++i) {
      for (int j = 0; j < num_coordinates; ++j) {
        final int y = coordinates[i];
        final int x = coordinates[j];
        if (x == -1 || y == -1) {
          continue;
        }
        // If the cell is unset, we embed the position adjustment pattern here.
        if (IsEmpty(matrix.get(y, x))) {
          // -2 is necessary since the x/y coordinates point to the center of the pattern, not the
          // left top corner.
          EmbedPositionAdjustmentPattern(x - 2, y - 2, matrix);
        }
      }
    }
  }

}
