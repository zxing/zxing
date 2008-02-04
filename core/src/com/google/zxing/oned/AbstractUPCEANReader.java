/*
 * Copyright 2008 Google Inc.
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

package com.google.zxing.oned;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ReaderException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.GenericResultPoint;

/**
 * <p>Encapsulates functionality and implementation that is common to UPC and EAN families
 * of one-dimensional barcodes.</p>
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author srowen@google.com (Sean Owen)
 * @author alasdair@google.com (Alasdair Mackintosh)
 */
public abstract class AbstractUPCEANReader extends AbstractOneDReader implements UPCEANReader {

  private static final float MAX_VARIANCE = 0.4f;

  /** Start/end guard pattern. */
  protected static final int[] START_END_PATTERN = {1, 1, 1,};

  /** Pattern marking the middle of a UPC/EAN pattern, separating the two halves. */
  protected static final int[] MIDDLE_PATTERN = {1, 1, 1, 1, 1};

  /** "Odd", or "L" patterns used to encode UPC/EAN digits. */
  protected static final int[][] L_PATTERNS = {
      {3, 2, 1, 1}, // 0
      {2, 2, 2, 1}, // 1
      {2, 1, 2, 2}, // 2
      {1, 4, 1, 1}, // 3
      {1, 1, 3, 2}, // 4
      {1, 2, 3, 1}, // 5
      {1, 1, 1, 4}, // 6
      {1, 3, 1, 2}, // 7
      {1, 2, 1, 3}, // 8
      {3, 1, 1, 2}  // 9
  };

  /** As above but also including the "even", or "G" patterns used to encode UPC/EAN digits. */
  protected static final int[][] L_AND_G_PATTERNS;

  static {
    L_AND_G_PATTERNS = new int[20][];
    for (int i = 0; i < 10; i++) {
      L_AND_G_PATTERNS[i] = L_PATTERNS[i];
    }
    for (int i = 10; i < 20; i++) {
      int[] widths = L_PATTERNS[i - 10];
      int[] reversedWidths = new int[widths.length];
      for (int j = 0; j < widths.length; j++) {
        reversedWidths[j] = widths[widths.length - j - 1];
      }
      L_AND_G_PATTERNS[i] = reversedWidths;
    }
  }

  static int[] findStartGuardPattern(final BitArray row) throws ReaderException {
    boolean foundStart = false;
    int[] startRange = null;
    int nextStart = 0;
    while (!foundStart) {
      startRange = findGuardPattern(row, nextStart, false, START_END_PATTERN);
      int start = startRange[0];
      nextStart = startRange[1];
      // As a check, we want to see some white in front of this "start pattern",
      // maybe as wide as the start pattern itself?
      foundStart = isWhiteRange(row, Math.max(0, start - 2 * (startRange[1] - start)), start);
    }
    return startRange;
  }

  public final Result decodeRow(int rowNumber, BitArray row) throws ReaderException {
    return decodeRow(rowNumber, row, findStartGuardPattern(row));
  }

  public final Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange) throws ReaderException {

    StringBuffer result = new StringBuffer();

    int endStart = decodeMiddle(row, startGuardRange, result);

    int[] endRange = decodeEnd(row, endStart);

    // Check for whitespace after the pattern
    int end = endRange[1];
    if (!isWhiteRange(row, end, Math.min(row.getSize(), end + 2 * (end - endRange[0])))) {
      throw new ReaderException("Pattern not followed by whitespace");
    }

    String resultString = result.toString();
    if (!checkChecksum(resultString)) {
      throw new ReaderException("Checksum failed");
    }

    return new Result(resultString, new ResultPoint[]{
        new GenericResultPoint((float) (startGuardRange[1] - startGuardRange[0]) / 2.0f, (float) rowNumber),
        new GenericResultPoint((float) (endRange[1] - endRange[0]) / 2.0f, (float) rowNumber)});
  }

  /**
   * @return true iff row consists of white values in the range [start,end)
   */
  protected static boolean isWhiteRange(BitArray row, int start, int end) {
    for (int i = start; i < end; i++) {
      if (row.get(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Computes the UPC/EAN checksum on a string of digits
   * @param s
   * @return
   */
  protected boolean checkChecksum(String s) throws ReaderException {
    int sum = 0;
    int length = s.length();
    for (int i = length - 2; i >= 0; i -= 2) {
      int digit = (int) s.charAt(i) - (int) '0';
      if (digit < 0 || digit > 9) {
        throw new ReaderException("Illegal character during checksum");
      }
      sum += digit;
    }
    sum *= 3;
    for (int i = length - 1; i >= 0; i -= 2) {
      int digit = (int) s.charAt(i) - (int) '0';
      if (digit < 0 || digit > 9) {
        throw new ReaderException("Illegal character during checksum");
      }
      sum += digit;
    }
    return sum % 10 == 0;
  }

  /**
   * Subclasses override this to decode the portion of a barcode between the start and end guard patterns.
   *
   * @param row row of black/white values to search
   * @param startRange start/end offset of start guard pattern
   * @param resultString {@link StringBuffer} to append decoded chars to
   * @return horizontal offset of first pixel after the "middle" that was decoded
   * @throws ReaderException if decoding could not complete successfully
   */
  protected abstract int decodeMiddle(BitArray row, int[] startRange, StringBuffer resultString)
      throws ReaderException;

  protected int[] decodeEnd(BitArray row, int endStart) throws ReaderException {
    return findGuardPattern(row, endStart, false, START_END_PATTERN);
  }

  /**
   * @param row row of black/white values to search
   * @param rowOffset position to start search
   * @param whiteFirst if true, indicates that the pattern specifies white/black/white/...
   *  pixel counts, otherwise, it is interpreted as black/white/black/...
   * @param pattern pattern of counts of number of black and white pixels that are being
   *  searched for as a pattern
   * @return start/end horizontal offset of guard pattern, as an array of two ints
   * @throws ReaderException if pattern is not found
   */
  protected static int[] findGuardPattern(BitArray row, int rowOffset, boolean whiteFirst, int[] pattern)
      throws ReaderException {
    int patternLength = pattern.length;
    int[] counters = new int[patternLength];
    int width = row.getSize();
    boolean isWhite = false;
    while (rowOffset < width) {
      isWhite = !row.get(rowOffset);
      if (whiteFirst == isWhite) {
        break;
      }
      rowOffset++;
    }

    int counterPosition = 0;
    int patternStart = rowOffset;
    for (int x = rowOffset; x < width; x++) {
      boolean pixel = row.get(x);
      if ((!pixel && isWhite) || (pixel && !isWhite)) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (patternMatchVariance(counters, pattern) < MAX_VARIANCE) {
            return new int[] {patternStart, x};
          }
          patternStart += counters[0] + counters[1];
          for (int y = 2; y < patternLength; y++) {
            counters[y - 2] = counters[y];
          }
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    throw new ReaderException("Can't find pattern");
  }

  /**
   * Attempts to decode a single UPC/EAN-encoded digit.
   *
   * @param row row of black/white values to decode
   * @param counters the counts of runs of observed black/white/black/... values
   * @param rowOffset horizontal offset to start decoding from
   * @param patterns the set of patterns to use to decode -- sometimes different encodings
   *  for the digits 0-9 are used, and this indicates the encodings for 0 to 9 that should
   *  be used
   * @return horizontal offset of first pixel beyond the decoded digit
   * @throws ReaderException if digit cannot be decoded
   */
  protected static int decodeDigit(BitArray row,
                                   int[] counters,
                                   int rowOffset,
                                   int[][] patterns) throws ReaderException {
    recordPattern(row, rowOffset, counters);
    float bestVariance = MAX_VARIANCE; // worst variance we'll accept
    int bestMatch = -1;
    for (int d = 0; d < patterns.length; d++) {
      int[] pattern = patterns[d];
      float variance = patternMatchVariance(counters, pattern);
      if (variance < bestVariance) {
        bestVariance = variance;
        bestMatch = d;
      }
    }
    if (bestMatch >= 0) {
      return bestMatch;
    } else {
      throw new ReaderException("Could not match any digit in pattern");
    }
  }

}