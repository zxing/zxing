/*
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
namespace com.google.zxing.oned
{ 
    using System;
    using System.Text;
    using com.google.zxing.common;


    public sealed class Code128Reader : AbstractOneDReader
    { 
            private static  int[][] CODE_PATTERNS = new int[][]{
              new int[]{2, 1, 2, 2, 2, 2}, // 0
              new int[]{2, 2, 2, 1, 2, 2},
              new int[]{2, 2, 2, 2, 2, 1},
              new int[]{1, 2, 1, 2, 2, 3},
              new int[]{1, 2, 1, 3, 2, 2},
              new int[]{1, 3, 1, 2, 2, 2}, // 5
              new int[]{1, 2, 2, 2, 1, 3},
              new int[]{1, 2, 2, 3, 1, 2},
              new int[]{1, 3, 2, 2, 1, 2},
              new int[]{2, 2, 1, 2, 1, 3},
              new int[]{2, 2, 1, 3, 1, 2}, // 10
              new int[]{2, 3, 1, 2, 1, 2},
              new int[]{1, 1, 2, 2, 3, 2},
              new int[]{1, 2, 2, 1, 3, 2},
              new int[]{1, 2, 2, 2, 3, 1},
              new int[]{1, 1, 3, 2, 2, 2}, // 15
              new int[]{1, 2, 3, 1, 2, 2},
              new int[]{1, 2, 3, 2, 2, 1},
              new int[]{2, 2, 3, 2, 1, 1},
              new int[]{2, 2, 1, 1, 3, 2},
              new int[]{2, 2, 1, 2, 3, 1}, // 20
              new int[]{2, 1, 3, 2, 1, 2},
              new int[]{2, 2, 3, 1, 1, 2},
              new int[]{3, 1, 2, 1, 3, 1},
              new int[]{3, 1, 1, 2, 2, 2},
              new int[]{3, 2, 1, 1, 2, 2}, // 25
              new int[]{3, 2, 1, 2, 2, 1},
              new int[]{3, 1, 2, 2, 1, 2},
              new int[]{3, 2, 2, 1, 1, 2},
              new int[]{3, 2, 2, 2, 1, 1},
              new int[]{2, 1, 2, 1, 2, 3}, // 30
              new int[]{2, 1, 2, 3, 2, 1},
              new int[]{2, 3, 2, 1, 2, 1},
              new int[]{1, 1, 1, 3, 2, 3},
              new int[]{1, 3, 1, 1, 2, 3},
              new int[]{1, 3, 1, 3, 2, 1}, // 35
              new int[]{1, 1, 2, 3, 1, 3},
              new int[]{1, 3, 2, 1, 1, 3},
              new int[]{1, 3, 2, 3, 1, 1},
              new int[]{2, 1, 1, 3, 1, 3},
              new int[]{2, 3, 1, 1, 1, 3}, // 40
              new int[]{2, 3, 1, 3, 1, 1},
              new int[]{1, 1, 2, 1, 3, 3},
              new int[]{1, 1, 2, 3, 3, 1},
              new int[]{1, 3, 2, 1, 3, 1},
              new int[]{1, 1, 3, 1, 2, 3}, // 45
              new int[]{1, 1, 3, 3, 2, 1},
              new int[]{1, 3, 3, 1, 2, 1},
              new int[]{3, 1, 3, 1, 2, 1},
              new int[]{2, 1, 1, 3, 3, 1},
              new int[]{2, 3, 1, 1, 3, 1}, // 50
              new int[]{2, 1, 3, 1, 1, 3},
              new int[]{2, 1, 3, 3, 1, 1},
              new int[]{2, 1, 3, 1, 3, 1},
              new int[]{3, 1, 1, 1, 2, 3},
              new int[]{3, 1, 1, 3, 2, 1}, // 55
              new int[]{3, 3, 1, 1, 2, 1},
              new int[]{3, 1, 2, 1, 1, 3},
              new int[]{3, 1, 2, 3, 1, 1},
              new int[]{3, 3, 2, 1, 1, 1},
              new int[]{3, 1, 4, 1, 1, 1}, // 60
              new int[]{2, 2, 1, 4, 1, 1},
              new int[]{4, 3, 1, 1, 1, 1},
              new int[]{1, 1, 1, 2, 2, 4},
              new int[]{1, 1, 1, 4, 2, 2},
              new int[] {1, 2, 1, 1, 2, 4}, // 65
              new int[]{1, 2, 1, 4, 2, 1},
              new int[]{1, 4, 1, 1, 2, 2},
              new int[]{1, 4, 1, 2, 2, 1},
              new int[]{1, 1, 2, 2, 1, 4},
              new int[]{1, 1, 2, 4, 1, 2}, // 70
              new int[]{1, 2, 2, 1, 1, 4},
              new int[]{1, 2, 2, 4, 1, 1},
              new int[]{1, 4, 2, 1, 1, 2},
              new int[]{1, 4, 2, 2, 1, 1},
              new int[]{2, 4, 1, 2, 1, 1}, // 75
              new int[]{2, 2, 1, 1, 1, 4},
              new int[]{4, 1, 3, 1, 1, 1},
              new int[]{2, 4, 1, 1, 1, 2},
              new int[]{1, 3, 4, 1, 1, 1},
              new int[]{1, 1, 1, 2, 4, 2}, // 80
              new int[]{1, 2, 1, 1, 4, 2},
              new int[]{1, 2, 1, 2, 4, 1},
              new int[]{1, 1, 4, 2, 1, 2},
              new int[]{1, 2, 4, 1, 1, 2},
              new int[]{1, 2, 4, 2, 1, 1}, // 85
              new int[]{4, 1, 1, 2, 1, 2},
              new int[]{4, 2, 1, 1, 1, 2},
              new int[]{4, 2, 1, 2, 1, 1},
              new int[]{2, 1, 2, 1, 4, 1},
              new int[]{2, 1, 4, 1, 2, 1}, // 90
              new int[]{4, 1, 2, 1, 2, 1},
              new int[]{1, 1, 1, 1, 4, 3},
              new int[]{1, 1, 1, 3, 4, 1},
              new int[]{1, 3, 1, 1, 4, 1},
              new int[]{1, 1, 4, 1, 1, 3}, // 95
              new int[]{1, 1, 4, 3, 1, 1},
              new int[]{4, 1, 1, 1, 1, 3},
              new int[]{4, 1, 1, 3, 1, 1},
              new int[]{1, 1, 3, 1, 4, 1},
              new int[]{1, 1, 4, 1, 3, 1}, // 100
              new int[]{3, 1, 1, 1, 4, 1},
              new int[]{4, 1, 1, 1, 3, 1},
              new int[]{2, 1, 1, 4, 1, 2},
              new int[]{2, 1, 1, 2, 1, 4},
              new int[]{2, 1, 1, 2, 3, 2}, // 105
              new int[]{2, 3, 3, 1, 1, 1, 2}
          };

      private static  int MAX_AVG_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.25f);
      private static  int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);

      private const int CODE_SHIFT = 98;

      private const int CODE_CODE_C = 99;
      private const int CODE_CODE_B = 100;
      private const int CODE_CODE_A = 101;

      private const int CODE_FNC_1 = 102;
      private const int CODE_FNC_2 = 97;
      private const int CODE_FNC_3 = 96;
      private const int CODE_FNC_4_A = 101;
      private const int CODE_FNC_4_B = 100;

      private const  int CODE_START_A = 103;
      private const int CODE_START_B = 104;
      private const int CODE_START_C = 105;
      private const int CODE_STOP = 106;

      private static int[] findStartPattern(BitArray row) {
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
        bool isWhite = false;
        int patternLength = counters.Length;

        for (int i = rowOffset; i < width; i++) {
          bool pixel = row.get(i);
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
                // Look for whitespace before start pattern, >= 50% of width of start pattern            
                if (row.isRange(Math.Max(0, patternStart - (i - patternStart) / 2), patternStart, false)) {
                  return new int[]{patternStart, i, bestMatch};
                }
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
        throw new ReaderException();
      }

      private static int decodeCode(BitArray row, int[] counters, int rowOffset) {
        recordPattern(row, rowOffset, counters);
        int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
        int bestMatch = -1;
        for (int d = 0; d < CODE_PATTERNS.Length; d++) {
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
          throw new ReaderException();
        }
      }

      public override Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints) {

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
            throw new ReaderException();
        }

        bool done = false;
        bool isNextShifted = false;

        StringBuilder result = new StringBuilder();
        int lastStart = startPatternInfo[0];
        int nextStart = startPatternInfo[1];
        int[] counters = new int[6];

        int lastCode = 0;
        int code = 0;
        int checksumTotal = startCode;
        int multiplier = 0;
        bool lastCharacterWasPrintable = true;

        while (!done) {

          bool unshift = isNextShifted;
          isNextShifted = false;

          // Save off last code
          lastCode = code;

          // Decode another code from image
          code = decodeCode(row, counters, nextStart);

          // Remember whether the last code was printable or not (excluding CODE_STOP)
          if (code != CODE_STOP) {
            lastCharacterWasPrintable = true;
          }

          // Add to checksum computation (if not CODE_STOP of course)
          if (code != CODE_STOP) {
            multiplier++;
            checksumTotal += multiplier * code;
          }

          // Advance to where the next code will to start
          lastStart = nextStart;
          for (int i = 0; i < counters.Length; i++) {
            nextStart += counters[i];
          }

          // Take care of illegal start codes
          switch (code) {
            case CODE_START_A:
            case CODE_START_B:
            case CODE_START_C:
              throw new ReaderException();
          }

          switch (codeSet) {

            case CODE_CODE_A:
              if (code < 64) {
                result.Append((char) (' ' + code));
              } else if (code < 96) {
                result.Append((char) (code - 64));
              } else {
                // Don't let CODE_STOP, which always appears, affect whether whether we think the last code
                // was printable or not
                if (code != CODE_STOP) {
                  lastCharacterWasPrintable = false;
                }
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
                result.Append((char) (' ' + code));
              } else {
                if (code != CODE_STOP) {
                  lastCharacterWasPrintable = false;
                }
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
                  result.Append('0');
                }
                result.Append(code);
              } else {
                if (code != CODE_STOP) {
                  lastCharacterWasPrintable = false;
                }
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

          // Unshift back to another code set if we were shifted
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

        // Check for ample whitespice following pattern, but, to do this we first need to remember that we
        // fudged decoding CODE_STOP since it actually has 7 bars, not 6. There is a black bar left to read off.
        // Would be slightly better to properly read. Here we just skip it:
        while (row.get(nextStart)) {
          nextStart++;
        }
        if (!row.isRange(nextStart, Math.Min(row.getSize(), nextStart + (nextStart - lastStart) / 2), false)) {
          throw new ReaderException();
        }

        // Pull out from sum the value of the penultimate check code
        checksumTotal -= multiplier * lastCode;
        // lastCode is the checksum then:
        if (checksumTotal % 103 != lastCode) {
          throw new ReaderException();
        }

        // Need to pull out the check digits from string
        int resultLength = result.Length;
        // Only bother if, well, the result had at least one character, and if the checksum digit happened
        // to be a printable character. If it was just interpreted as a control code, nothing to remove
        if (resultLength > 0 && lastCharacterWasPrintable) {
          if (codeSet == CODE_CODE_C) {
              result.Remove(resultLength - 2, resultLength);
          } else {
              result.Remove(resultLength - 1, resultLength);
          }
        }

        String resultString = result.ToString();

        if (resultString.Length == 0) {
          // Almost surely a false positive
          throw new ReaderException();
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



}