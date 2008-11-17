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

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class MaskUtil {

  // The mask penalty calculation is complicated.  See Table 21 of JISX0510:2004 (p.45) for details.
  // Basically it applies four rules and summate all penalties.
  public static int CalculateMaskPenalty(final ByteMatrix matrix) {
    int penalty = 0;
    penalty += ApplyMaskPenaltyRule1(matrix);
    penalty += ApplyMaskPenaltyRule2(matrix);
    penalty += ApplyMaskPenaltyRule3(matrix);
    penalty += ApplyMaskPenaltyRule4(matrix);
    return penalty;
  }

  // Apply mask penalty rule 1 and return the penalty. Find repetitive cells with the same color and
  // give penalty to them. Example: 00000 or 11111.
  public static int ApplyMaskPenaltyRule1(final ByteMatrix matrix) {
    final int penalty = (ApplyMaskPenaltyRule1Internal(matrix, true) +
        ApplyMaskPenaltyRule1Internal(matrix, false));
    Debug.LOG_INFO("\tApplyMaskPenaltyRule1: " + penalty);
    return penalty;
  }

  // Apply mask penalty rule 2 and return the penalty. Find 2x2 blocks with the same color and give
  // penalty to them.
  //
  // JAVAPORT: Consider using ByteMatrix.getArray() instead.
  public static int ApplyMaskPenaltyRule2(final ByteMatrix matrix) {
    int penalty = 0;
    for (int y = 0; y < matrix.height() - 1; ++y) {
      for (int x = 0; x < matrix.width() - 1; ++x) {
        int value = matrix.get(y, x);
        if (value == matrix.get(y + 0, x + 1) &&
            value == matrix.get(y + 1, x + 0) &&
            value == matrix.get(y + 1, x + 1)) {
          penalty += 3;
        }
      }
    }
    Debug.LOG_INFO("\tApplyMaskPenaltyRule2: " + penalty);
    return penalty;
  }

  // Apply mask penalty rule 3 and return the penalty. Find consecutive cells of 00001011101 or
  // 10111010000, and give penalty to them.  If we find patterns like 000010111010000, we give
  // penalties twice (i.e. 40 * 2).
  //
  // JAVAPORT: This many calls to ByteMatrix.get() looks expensive. We should profile and consider
  // adding a byte[][] ByteMatrix.getArray() method, then using that array locally.
  public static int ApplyMaskPenaltyRule3(final ByteMatrix matrix) {
    int penalty = 0;
    for (int y = 0; y < matrix.height(); ++y) {
      for (int x = 0; x < matrix.width(); ++x) {
        // Tried to simplify following conditions but failed.
        if (x + 6 < matrix.width() &&
            matrix.get(y, x +  0) == 1 &&
            matrix.get(y, x +  1) == 0 &&
            matrix.get(y, x +  2) == 1 &&
            matrix.get(y, x +  3) == 1 &&
            matrix.get(y, x +  4) == 1 &&
            matrix.get(y, x +  5) == 0 &&
            matrix.get(y, x +  6) == 1 &&
            ((x + 10 < matrix.width() &&
                matrix.get(y, x +  7) == 0 &&
                matrix.get(y, x +  8) == 0 &&
                matrix.get(y, x +  9) == 0 &&
                matrix.get(y, x + 10) == 0) ||
                (x - 4 >= 0 &&
                    matrix.get(y, x -  1) == 0 &&
                    matrix.get(y, x -  2) == 0 &&
                    matrix.get(y, x -  3) == 0 &&
                    matrix.get(y, x -  4) == 0))) {
          penalty += 40;
        }
        if (y + 6 < matrix.height() &&
            matrix.get(y +  0, x) == 1  &&
            matrix.get(y +  1, x) == 0  &&
            matrix.get(y +  2, x) == 1  &&
            matrix.get(y +  3, x) == 1  &&
            matrix.get(y +  4, x) == 1  &&
            matrix.get(y +  5, x) == 0  &&
            matrix.get(y +  6, x) == 1 &&
            ((y + 10 < matrix.height() &&
                matrix.get(y +  7, x) == 0 &&
                matrix.get(y +  8, x) == 0 &&
                matrix.get(y +  9, x) == 0 &&
                matrix.get(y + 10, x) == 0) ||
                (y - 4 >= 0 &&
                    matrix.get(y -  1, x) == 0 &&
                    matrix.get(y -  2, x) == 0 &&
                    matrix.get(y -  3, x) == 0 &&
                    matrix.get(y -  4, x) == 0))) {
          penalty += 40;
        }
      }
    }
    Debug.LOG_INFO("\tApplyMaskPenaltyRule3: " + penalty);
    return penalty;
  }

  // Apply mask penalty rule 4 and return the penalty. Calculate the ratio of dark cells and give
  // penalty if the ratio is far from 50%. It gives 10 penalty for 5% distance. Examples:
  // -   0% => 100
  // -  40% =>  20
  // -  45% =>  10
  // -  50% =>   0
  // -  55% =>  10
  // -  55% =>  20
  // - 100% => 100
  public static int ApplyMaskPenaltyRule4(final ByteMatrix matrix) {
    int num_dark_cells = 0;
    for (int y = 0; y < matrix.height(); ++y) {
      for (int x = 0; x < matrix.width(); ++x) {
        if (matrix.get(y, x) == 1) {
          num_dark_cells += 1;
        }
      }
    }
    final int num_total_cells = matrix.height() * matrix.width();
    double dark_ratio = (double) num_dark_cells / num_total_cells;
    final int penalty = Math.abs((int) (dark_ratio * 100 - 50)) / 5 * 10;
    Debug.LOG_INFO("\tApplyMaskPenaltyRule4: " + penalty);
    return penalty;
  }

  // Return the mask bit for "mask_pattern" at "x" and "y". See 8.8 of JISX0510:2004 for mask
  // pattern conditions.
  public static int GetDataMaskBit(final int mask_pattern, final int x, final int y) {
    Debug.DCHECK(QRCode.IsValidMaskPattern(mask_pattern));
    switch (mask_pattern) {
      case 0:
        return ((y + x) % 2 == 0) ? 1 : 0;
      case 1:
        return (y % 2 == 0) ? 1 : 0;
      case 2:
        return (x % 3 == 0) ? 1 : 0;
      case 3:
        return ((y + x) % 3 == 0) ? 1 : 0;
      case 4:
        return (((y / 2) + (x / 3)) % 2 == 0) ? 1 : 0;
      case 5:
        return (((y * x) % 2) + ((y * x) % 3) == 0) ? 1 : 0;
      case 6:
        return ((((y * x) % 2) + ((y * x) % 3)) % 2 == 0) ? 1 : 0;
      case 7:
        return ((((y * x) % 3) + ((y + x) % 2)) % 2 == 0) ? 1 : 0;
      default:
        ;
    }
    Debug.LOG_ERROR("invalid mask pattern: " + mask_pattern);
    return -1;
  }

  // Helper function for ApplyMaskPenaltyRule1. We need this for doing this calculation in both
  // vertical and horizontal orders respectively.
  private static int ApplyMaskPenaltyRule1Internal(final ByteMatrix matrix, boolean is_horizontal) {
    int penalty = 0;
    int num_same_bit_cells = 0;
    int prev_bit = -1;
    // Horizontal mode:
    //   for (int i = 0; i < matrix.height(); ++i) {
    //     for (int j = 0; j < matrix.width(); ++j) {
    //       int bit = matrix.get(i, j);
    // Vertical mode:
    //   for (int i = 0; i < matrix.width(); ++i) {
    //     for (int j = 0; j < matrix.height(); ++j) {
    //       int bit = matrix.get(j, i);
    final int i_limit = is_horizontal ? matrix.height() : matrix.width();
    final int j_limit = is_horizontal ? matrix.width() : matrix.height();
    for (int i = 0; i < i_limit; ++i) {
      for (int j = 0; j < j_limit; ++j) {
        final int bit = is_horizontal ? matrix.get(i, j) : matrix.get(j, i);
        if (bit == prev_bit) {
          num_same_bit_cells += 1;
          // Found five repetitive cells with the same color (bit).
          // We'll give penalty of 3.
          if (num_same_bit_cells == 5) {
            penalty += 3;
          } else if (num_same_bit_cells > 5) {
            // After five repetitive cells, we'll add the penalty one
            // by one.
            penalty += 1;
          }
        } else {
          num_same_bit_cells = 1;  // Include the cell itself.
          prev_bit = bit;
        }
      }
      num_same_bit_cells = 0;  // Clear at each row/column.
    }
    return penalty;
  }

}
