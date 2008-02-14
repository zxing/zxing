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

import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.GenericResultPoint;

/**
 * <p>Decodes Code 39 barcodes. This does not supported "Full ASCII Code 39" yet.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class Code39Reader extends AbstractOneDReader {

  private static final String ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";
  private static final char[] ALPHABET = ALPHABET_STRING.toCharArray();

  private static final int[] CHARACTER_ENCODINGS = {
      0x038, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064, // 0-9
      0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x08C, 0x01C, // A-J
      0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016,  // K-T
      0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094, // U-*
      0x0A8, 0x0A2, 0x08A, 0x02A // $-%
  };

  private static final int ASTERISK_ENCODING = CHARACTER_ENCODINGS[39];

  private final boolean usingCheckDigit;

  /**
   * Creates a reader that assumes all encoded data is data, and does not treat the final
   * character as a check digit.
   */
  public Code39Reader() {
    usingCheckDigit = false;
  }

  /**
   * Creates a reader that can be configured to check the last character as a check digit.
   *
   * @param usingCheckDigit if true, treat the last data character as a check digit, not
   * data, and verify that the checksum passes
   */
  public Code39Reader(boolean usingCheckDigit) {
    this.usingCheckDigit = usingCheckDigit;
  }

  public Result decodeRow(final int rowNumber, final BitArray row) throws ReaderException {

    int[] start = findAsteriskPattern(row);

    int nextStart = start[1];

    int end = row.getSize();

    // Read off white space
    while (nextStart < end && !row.get(nextStart)) {
      nextStart++;
    }

    StringBuffer result = new StringBuffer();
    int[] counters = new int[9];
    char decodedChar;
    int lastStart;
    do {
      recordPattern(row, nextStart, counters);
      int pattern = toNarrowWidePattern(counters);
      decodedChar = patternToChar(pattern);
      result.append(decodedChar);
      lastStart = nextStart;
      for (int i = 0; i < counters.length; i++) {
        nextStart += counters[i];
      }
      // Read off white space
      while (nextStart < end && !row.get(nextStart)) {
        nextStart++;
      }
    } while (decodedChar != '*');
    result.deleteCharAt(result.length() - 1); // remove asterisk

    if (usingCheckDigit) {
      int max = result.length() - 1;
      int total = 0;
      for (int i = 0; i < max; i++) {
        total += ALPHABET_STRING.indexOf(result.charAt(i));
      }
      if (total % 43 != ALPHABET_STRING.indexOf(result.charAt(max))) {
        throw new ReaderException("Checksum failed");
      }
      result.deleteCharAt(max);
    }

    String resultString = result.toString();
    return new Result(resultString,
        new ResultPoint[]{new GenericResultPoint((float) (start[1] - start[0]) / 2.0f, (float) rowNumber),
            new GenericResultPoint((float) (nextStart - lastStart) / 2.0f, (float) rowNumber)});

  }

  private static int[] findAsteriskPattern(BitArray row) throws ReaderException {
    int width = row.getSize();
    int rowOffset = 0;
    while (rowOffset < width) {
      if (row.get(rowOffset)) {
        break;
      }
      rowOffset++;
    }

    int counterPosition = 0;
    int[] counters = new int[9];
    int patternStart = rowOffset;
    boolean isWhite = false;
    int patternLength = counters.length;

    for (int i = rowOffset; i < width; i++) {
      boolean pixel = row.get(i);
      if ((!pixel && isWhite) || (pixel && !isWhite)) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          try {
            if (toNarrowWidePattern(counters) == ASTERISK_ENCODING) {
              return new int[]{patternStart, i};
            }
          } catch (ReaderException re) {
            // no match, continue
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

  private static int toNarrowWidePattern(int[] counters) throws ReaderException {
    int minCounter = Integer.MAX_VALUE;
    for (int i = 0; i < counters.length; i++) {
      if (counters[i] < minCounter) {
        minCounter = counters[i];
      }
    }
    int maxNarrowCounter = (int) (minCounter * 1.5f);
    int wideCounters = 0;
    int pattern = 0;
    for (int i = 0; i < counters.length; i++) {
      if (counters[i] > maxNarrowCounter) {
        pattern |= 1 << (counters.length - 1 - i);
        wideCounters++;
      }
    }
    if (wideCounters != 3) {
      throw new ReaderException("Can't find 3 wide bars/spaces out of 9");
    }
    return pattern;
  }

  private static char patternToChar(int pattern) throws ReaderException {
    for (int i = 0; i < CHARACTER_ENCODINGS.length; i++) {
      if (CHARACTER_ENCODINGS[i] == pattern) {
        return ALPHABET[i];
      }
    }
    throw new ReaderException("Pattern did not match character encoding");
  }

}