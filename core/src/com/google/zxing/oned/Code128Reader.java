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

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.GenericResultPoint;

import java.util.Hashtable;

/**
 * <p>Decodes Code 128 barcodes.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class Code128Reader extends AbstractOneDReader {

  private static final int[][] CODE_PATTERNS = {
      {2, 1, 2, 2, 2, 2}, // 0
      {2, 2, 2, 1, 2, 2},
      {2, 2, 2, 2, 2, 1},
      {1, 2, 1, 2, 2, 3},
      {1, 2, 1, 3, 2, 2},
      {1, 3, 1, 2, 2, 2}, // 5
      {1, 2, 2, 2, 1, 3},
      {1, 2, 2, 3, 1, 2},
      {1, 3, 2, 2, 1, 2},
      {2, 2, 1, 2, 1, 3},
      {2, 2, 1, 3, 1, 2}, // 10
      {2, 3, 1, 2, 1, 2},
      {1, 1, 2, 2, 3, 2},
      {1, 2, 2, 1, 3, 2},
      {1, 2, 2, 2, 3, 1},
      {1, 1, 3, 2, 2, 2}, // 15
      {1, 2, 3, 1, 2, 2},
      {1, 2, 3, 2, 2, 1},
      {2, 2, 3, 2, 1, 1},
      {2, 2, 1, 1, 3, 2},
      {2, 2, 1, 2, 3, 1}, // 20
      {2, 1, 3, 2, 1, 2},
      {2, 2, 3, 1, 1, 2},
      {3, 1, 2, 1, 3, 1},
      {3, 1, 1, 2, 2, 2},
      {3, 2, 1, 1, 2, 2}, // 25
      {3, 2, 1, 2, 2, 1},
      {3, 1, 2, 2, 1, 2},
      {3, 2, 2, 1, 1, 2},
      {3, 2, 2, 2, 1, 1},
      {2, 1, 2, 1, 2, 3}, // 30
      {2, 1, 2, 3, 2, 1},
      {2, 3, 2, 1, 2, 1},
      {1, 1, 1, 3, 2, 3},
      {1, 3, 1, 1, 2, 3},
      {1, 3, 1, 3, 2, 1}, // 35
      {1, 1, 2, 3, 1, 3},
      {1, 3, 2, 1, 1, 3},
      {1, 3, 2, 3, 1, 1},
      {2, 1, 1, 3, 1, 3},
      {2, 3, 1, 1, 1, 3}, // 40
      {2, 3, 1, 3, 1, 1},
      {1, 1, 2, 1, 3, 3},
      {1, 1, 2, 3, 3, 1},
      {1, 3, 2, 1, 3, 1},
      {1, 1, 3, 1, 2, 3}, // 45
      {1, 1, 3, 3, 2, 1},
      {1, 3, 3, 1, 2, 1},
      {3, 1, 3, 1, 2, 1},
      {2, 1, 1, 3, 3, 1},
      {2, 3, 1, 1, 3, 1}, // 50
      {2, 1, 3, 1, 1, 3},
      {2, 1, 3, 3, 1, 1},
      {2, 1, 3, 1, 3, 1},
      {3, 1, 1, 1, 2, 3},
      {3, 1, 1, 3, 2, 1}, // 55
      {3, 3, 1, 1, 2, 1},
      {3, 1, 2, 1, 1, 3},
      {3, 1, 2, 3, 1, 1},
      {3, 3, 2, 1, 1, 1},
      {3, 1, 4, 1, 1, 1}, // 60
      {2, 2, 1, 4, 1, 1},
      {4, 3, 1, 1, 1, 1},
      {1, 1, 1, 2, 2, 4},
      {1, 1, 1, 4, 2, 2},
      {1, 2, 1, 1, 2, 4}, // 65
      {1, 2, 1, 4, 2, 1},
      {1, 4, 1, 1, 2, 2},
      {1, 4, 1, 2, 2, 1},
      {1, 1, 2, 2, 1, 4},
      {1, 1, 2, 4, 1, 2}, // 70
      {1, 2, 2, 1, 1, 4},
      {1, 2, 2, 4, 1, 1},
      {1, 4, 2, 1, 1, 2},
      {1, 4, 2, 2, 1, 1},
      {2, 4, 1, 2, 1, 1}, // 75
      {2, 2, 1, 1, 1, 4},
      {4, 1, 3, 1, 1, 1},
      {2, 4, 1, 1, 1, 2},
      {1, 3, 4, 1, 1, 1},
      {1, 1, 1, 2, 4, 2}, // 80
      {1, 2, 1, 1, 4, 2},
      {1, 2, 1, 2, 4, 1},
      {1, 1, 4, 2, 1, 2},
      {1, 2, 4, 1, 1, 2},
      {1, 2, 4, 2, 1, 1}, // 85
      {4, 1, 1, 2, 1, 2},
      {4, 2, 1, 1, 1, 2},
      {4, 2, 1, 2, 1, 1},
      {2, 1, 2, 1, 4, 1},
      {2, 1, 4, 1, 2, 1}, // 90
      {4, 1, 2, 1, 2, 1},
      {1, 1, 1, 1, 4, 3},
      {1, 1, 1, 3, 4, 1},
      {1, 3, 1, 1, 4, 1},
      {1, 1, 4, 1, 1, 3}, // 95
      {1, 1, 4, 3, 1, 1},
      {4, 1, 1, 1, 1, 3},
      {4, 1, 1, 3, 1, 1},
      {1, 1, 3, 1, 4, 1},
      {1, 1, 4, 1, 3, 1}, // 100
      {3, 1, 1, 1, 4, 1},
      {4, 1, 1, 1, 3, 1},
      {2, 1, 1, 4, 1, 2},
      {2, 1, 1, 2, 1, 4},
      {2, 1, 1, 2, 3, 2}, // 105
      {2, 3, 3, 1, 1, 1, 2}
  };

  private static final int MAX_AVG_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.25f);
  private static final int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);

  private static final int CODE_SHIFT = 98;

  private static final int CODE_CODE_C = 99;
  private static final int CODE_CODE_B = 100;
  private static final int CODE_CODE_A = 101;

  private static final int CODE_FNC_1 = 102;
  private static final int CODE_FNC_2 = 97;
  private static final int CODE_FNC_3 = 96;
  private static final int CODE_FNC_4_A = 101;
  private static final int CODE_FNC_4_B = 100;

  private static final int CODE_START_A = 103;
  private static final int CODE_START_B = 104;
  private static final int CODE_START_C = 105;
  private static final int CODE_STOP = 106;

  private static int[] findStartPattern(BitArray row) throws ReaderException {
    int width = row.getSize();
    int rowOffset = 0;
    while (rowOffset < width) {
      if (row.get(rowOffset)) {
        break;
      }
      rowOffset++;
    }

    int counterPosition = 0;
    int[] counters = new int[6];
    int patternStart = rowOffset;
    boolean isWhite = false;
    int patternLength = counters.length;

    for (int i = rowOffset; i < width; i++) {
      boolean pixel = row.get(i);
      if ((!pixel && isWhite) || (pixel && !isWhite)) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          int bestVariance = MAX_AVG_VARIANCE;
          int bestMatch = -1;
          for (int startCode = CODE_START_A; startCode <= CODE_START_C; startCode++) {
            int variance = patternMatchVariance(counters, CODE_PATTERNS[startCode], MAX_INDIVIDUAL_VARIANCE);
            if (variance < bestVariance) {
              bestVariance = variance;
              bestMatch = startCode;
            }
          }
          if (bestMatch >= 0) {
            return new int[]{patternStart, i, bestMatch};
          }
          patternStart += counters[0] + counters[1];
          for (int y = 2; y < patternLength; y++) {
            counters[y - 2] = counters[y];
          }
          counters[patternLength - 2] = 0;
          counters[patternLength - 1] = 0;
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

  private static int decodeCode(BitArray row, int[] counters, int rowOffset) throws ReaderException {
    recordPattern(row, rowOffset, counters);
    int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
    int bestMatch = -1;
    for (int d = 0; d < CODE_PATTERNS.length; d++) {
      int[] pattern = CODE_PATTERNS[d];
      int variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
      if (variance < bestVariance) {
        bestVariance = variance;
        bestMatch = d;
      }
    }
    // TODO We're overlooking the fact that the STOP pattern has 7 values, not 6
    if (bestMatch >= 0) {
      return bestMatch;
    } else {
      throw new ReaderException("Could not match any code pattern");
    }
  }

  public Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws ReaderException {

    int[] startPatternInfo = findStartPattern(row);
    int startCode = startPatternInfo[2];
    int codeSet;
    switch (startCode) {
      case CODE_START_A:
        codeSet = CODE_CODE_A;
        break;
      case CODE_START_B:
        codeSet = CODE_CODE_B;
        break;
      case CODE_START_C:
        codeSet = CODE_CODE_C;
        break;
      default:
        throw new ReaderException("Illegal start code");
    }

    boolean done = false;
    boolean isNextShifted = false;

    StringBuffer result = new StringBuffer();
    int lastStart = startPatternInfo[0];
    int nextStart = startPatternInfo[1];
    int[] counters = new int[6];

    int lastCode = 0;
    int code = 0;
    int checksumTotal = startCode;
    int multiplier = 0;

    while (!done) {

      boolean unshift = isNextShifted;
      isNextShifted = false;

      lastCode = code;
      code = decodeCode(row, counters, nextStart);
      if (code != CODE_STOP) {
        multiplier++;
        checksumTotal += multiplier * code;
      }

      lastStart = nextStart;
      for (int i = 0; i < counters.length; i++) {
        nextStart += counters[i];
      }

      // Take care of illegal start codes
      switch (code) {
        case CODE_START_A:
        case CODE_START_B:
        case CODE_START_C:
          throw new ReaderException("Unexpected start code");
      }

      switch (codeSet) {

        case CODE_CODE_A:
          if (code < 64) {
            result.append((char) (' ' + code));
          } else if (code < 96) {
            result.append((char) (code - 64));
          } else {
            switch (code) {
              case CODE_FNC_1:
              case CODE_FNC_2:
              case CODE_FNC_3:
              case CODE_FNC_4_A:
                // do nothing?
                break;
              case CODE_SHIFT:
                isNextShifted = true;
                codeSet = CODE_CODE_B;
                break;
              case CODE_CODE_B:
                codeSet = CODE_CODE_B;
                break;
              case CODE_CODE_C:
                codeSet = CODE_CODE_C;
                break;
              case CODE_STOP:
                done = true;
                break;
            }
          }
          break;
        case CODE_CODE_B:
          if (code < 96) {
            result.append((char) (' ' + code));
          } else {
            switch (code) {
              case CODE_FNC_1:
              case CODE_FNC_2:
              case CODE_FNC_3:
              case CODE_FNC_4_B:
                // do nothing?
                break;
              case CODE_SHIFT:
                isNextShifted = true;
                codeSet = CODE_CODE_C;
                break;
              case CODE_CODE_A:
                codeSet = CODE_CODE_A;
                break;
              case CODE_CODE_C:
                codeSet = CODE_CODE_C;
                break;
              case CODE_STOP:
                done = true;
                break;
            }
          }
          break;
        case CODE_CODE_C:
          if (code < 100) {
            if (code < 10) {
              result.append('0');
            }
            result.append(code);
          } else {
            switch (code) {
              case CODE_FNC_1:
                // do nothing?
                break;
              case CODE_CODE_A:
                codeSet = CODE_CODE_A;
                break;
              case CODE_CODE_B:
                codeSet = CODE_CODE_B;
                break;
              case CODE_STOP:
                done = true;
                break;
            }
          }
          break;
      }

      if (unshift) {
        switch (codeSet) {
          case CODE_CODE_A:
            codeSet = CODE_CODE_C;
            break;
          case CODE_CODE_B:
            codeSet = CODE_CODE_A;
            break;
          case CODE_CODE_C:
            codeSet = CODE_CODE_B;
            break;
        }
      }

    }

    // Pull out from sum the value of the penultimate check code
    checksumTotal -= multiplier * lastCode;
    if (checksumTotal % 103 != lastCode) {
      throw new ReaderException("Checksum failed");
    }

    // Need to pull out the check digits from string
    int resultLength = result.length();
    if (resultLength > 0) {
      if (codeSet == CODE_CODE_C) {
        result.delete(resultLength - 2, resultLength);
      } else {
        result.delete(resultLength - 1, resultLength);
      }
    }

    String resultString = result.toString();

    if (resultString.length() == 0) {
      // Almost surely a false positive
      throw new ReaderException("Empty barcode found; assuming a false positive");
    }

    float left = (float) (startPatternInfo[1] + startPatternInfo[0]) / 2.0f;
    float right = (float) (nextStart + lastStart) / 2.0f;
    return new Result(
        resultString,
        null,
        new ResultPoint[]{
            new GenericResultPoint(left, (float) rowNumber),
            new GenericResultPoint(right, (float) rowNumber)},
        BarcodeFormat.CODE_128);

  }

}
