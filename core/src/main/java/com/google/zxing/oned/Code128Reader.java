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
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;

import java.util.Arrays;
import java.util.Map;

/**
 * <p>Decodes Code 128 barcodes.</p>
 *
 * @author Sean Owen
 */
public final class Code128Reader extends OneDReader {

  static final int[][] CODE_PATTERNS = {
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

  private static final float MAX_AVG_VARIANCE = 0.25f;
  private static final float MAX_INDIVIDUAL_VARIANCE = 0.7f;

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

  private static int[] findStartPattern(BitArray row) throws NotFoundException {
    int width = row.getSize();
    int rowOffset = row.getNextSet(0);

    int counterPosition = 0;
    int[] counters = new int[6];
    int patternStart = rowOffset;
    boolean isWhite = false;
    int patternLength = counters.length;

    int i = rowOffset;
    while (i < width) {
      int next = isWhite ? row.getNextSet(i) : row.getNextUnset(i);
      counters[counterPosition] = next - i;
      if (next == width) {
        break;
      }
      i = next;
      if (counterPosition == patternLength - 1) {
        float bestVariance = MAX_AVG_VARIANCE;
        int bestMatch = -1;
        for (int startCode = CODE_START_A; startCode <= CODE_START_C; startCode++) {
          float variance = patternMatchVariance(counters, CODE_PATTERNS[startCode],
              MAX_INDIVIDUAL_VARIANCE);
          if (variance < bestVariance) {
            bestVariance = variance;
            bestMatch = startCode;
          }
        }
        // Look for whitespace before start pattern, >= 50% of width of start pattern
        if (bestMatch >= 0 &&
            row.isRange(Math.max(0, patternStart - (i - patternStart) / 2), patternStart, false)) {
          return new int[]{patternStart, i, bestMatch};
        }
        patternStart += counters[0] + counters[1];
        System.arraycopy(counters, 2, counters, 0, counterPosition - 1);
        counterPosition--;
      } else {
        counterPosition++;
      }
      isWhite = !isWhite;
    }
    throw NotFoundException.getNotFoundInstance();
  }

  private static int decodeCode(BitArray row, int[] counters, int rowOffset)
      throws NotFoundException {
    recordPattern(row, rowOffset, counters);
    float bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
    int bestMatch = -1;
    for (int d = 0; d < CODE_PATTERNS.length; d++) {
      int[] pattern = CODE_PATTERNS[d];
      float variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
      if (variance < bestVariance) {
        bestVariance = variance;
        bestMatch = d;
      }
    }
    // TODO We're overlooking the fact that the STOP pattern has 7 values, not 6.
    if (bestMatch >= 0) {
      return bestMatch;
    } else {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  @Override
  public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType,?> hints)
      throws NotFoundException, FormatException, ChecksumException {

    boolean convertFNC1 = hints != null && hints.containsKey(DecodeHintType.ASSUME_GS1);

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
        throw FormatException.getFormatInstance();
    }

    byte[] rawCodes = new byte[20];
    rawCodes[0] = (byte) startCode;
    int rawCodesSize = 1;

    int lastStart = startPatternInfo[0];
    int nextStart = startPatternInfo[1];
    int[] counters = new int[6];

    // Pass 1: Read all codewords up to CODE_STOP without speculatively mutating output state.
    while (true) {
      int code = decodeCode(row, counters, nextStart);
      if (rawCodesSize == rawCodes.length) {
        rawCodes = Arrays.copyOf(rawCodes, rawCodesSize * 2);
      }
      rawCodes[rawCodesSize++] = (byte) code;

      // Advance to where the next code will start
      lastStart = nextStart;
      for (int counter : counters) {
        nextStart += counter;
      }

      if (code == CODE_STOP) {
        break;
      }

      // Take care of illegal start codes
      switch (code) {
        case CODE_START_A:
        case CODE_START_B:
        case CODE_START_C:
          throw FormatException.getFormatInstance();
      }
    }

    int lastPatternSize = nextStart - lastStart;

    // Check for ample whitespace following pattern, but, to do this we first need to remember that
    // we fudged decoding CODE_STOP since it actually has 7 bars, not 6. There is a black bar left
    // to read off. Would be slightly better to properly read. Here we just skip it:
    nextStart = row.getNextUnset(nextStart);
    if (!row.isRange(nextStart,
                     Math.min(row.getSize(), nextStart + (nextStart - lastStart) / 2),
                     false)) {
      throw NotFoundException.getNotFoundInstance();
    }

    // A valid symbol requires at least Start, one payload codeword, Check, and Stop.
    if (rawCodesSize < 4) {
      throw NotFoundException.getNotFoundInstance();
    }

    // Verify Mod-103 checksum over start + payload codewords before decoding payload.
    int payloadEnd = rawCodesSize - 2; // exclusive; index of check code
    int checksumTotal = startCode;
    for (int i = 1; i < payloadEnd; i++) {
      checksumTotal = (checksumTotal + i * (rawCodes[i] & 0xFF)) % 103;
    }
    int checkCode = rawCodes[payloadEnd] & 0xFF;
    if (checksumTotal != checkCode) {
      throw ChecksumException.getChecksumInstance();
    }

    // Pass 2: Decode only the verified payload codewords (excluding Start, Check, and Stop).
    StringBuilder result = new StringBuilder(20);
    int symbologyModifier = 0;
    boolean hasLeadingFNC1 = false;
    boolean hasSecondFNC1 = false;
    boolean readerInit = false;
    boolean isNextShifted = false;
    boolean upperMode = false;
    boolean shiftUpperMode = false;

    for (int i = 1; i < payloadEnd; i++) {
      int code = rawCodes[i] & 0xFF;

      int currentCodeSet = codeSet;
      if (isNextShifted) {
        currentCodeSet = codeSet == CODE_CODE_A ? CODE_CODE_B : CODE_CODE_A;
        isNextShifted = false;
      }

      if (code == CODE_FNC_1) {
        if (result.length() == 0 && !hasLeadingFNC1) {
          // GS1 specification 5.4.3.7. and 5.4.6.4. If the first char after the start code
          // is FNC1 then this is GS1-128.
          hasLeadingFNC1 = true;
          symbologyModifier = 1;
          if (convertFNC1) {
            result.append("]C1");
          }
        } else if (result.length() == 1 && !hasLeadingFNC1 && !hasSecondFNC1) {
          hasSecondFNC1 = true;
          symbologyModifier = 2;
          if (convertFNC1) {
            result.append((char) 29);
          }
        } else {
          // ISO/IEC 15417 4.3.4.3 / GS1 5.4.7.5: every subsequent FNC1 is returned as ASCII 29 (GS).
          result.append((char) 29);
        }
        continue;
      }

      switch (currentCodeSet) {
        case CODE_CODE_A:
          if (code < 64) {
            if (shiftUpperMode == upperMode) {
              result.append((char) (' ' + code));
            } else {
              result.append((char) (' ' + code + 128));
            }
            shiftUpperMode = false;
          } else if (code < 96) {
            if (shiftUpperMode == upperMode) {
              result.append((char) (code - 64));
            } else {
              result.append((char) (code + 64));
            }
            shiftUpperMode = false;
          } else {
            switch (code) {
              case CODE_FNC_2:
                symbologyModifier = 4;
                break;
              case CODE_FNC_3:
                readerInit = true;
                break;
              case CODE_FNC_4_A:
                if (shiftUpperMode) {
                  upperMode = !upperMode;
                  shiftUpperMode = false;
                } else {
                  shiftUpperMode = true;
                }
                break;
              case CODE_SHIFT:
                isNextShifted = true;
                break;
              case CODE_CODE_B:
                codeSet = CODE_CODE_B;
                break;
              case CODE_CODE_C:
                codeSet = CODE_CODE_C;
                break;
              default:
                break;
            }
          }
          break;
        case CODE_CODE_B:
          if (code < 96) {
            if (shiftUpperMode == upperMode) {
              result.append((char) (' ' + code));
            } else {
              result.append((char) (' ' + code + 128));
            }
            shiftUpperMode = false;
          } else {
            switch (code) {
              case CODE_FNC_2:
                symbologyModifier = 4;
                break;
              case CODE_FNC_3:
                readerInit = true;
                break;
              case CODE_FNC_4_B:
                if (shiftUpperMode) {
                  upperMode = !upperMode;
                  shiftUpperMode = false;
                } else {
                  shiftUpperMode = true;
                }
                break;
              case CODE_SHIFT:
                isNextShifted = true;
                break;
              case CODE_CODE_A:
                codeSet = CODE_CODE_A;
                break;
              case CODE_CODE_C:
                codeSet = CODE_CODE_C;
                break;
              default:
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
              case CODE_CODE_A:
                codeSet = CODE_CODE_A;
                break;
              case CODE_CODE_B:
                codeSet = CODE_CODE_B;
                break;
              default:
                break;
            }
          }
          break;
        default:
          break;
      }
    }

    if (result.length() == 0 && !readerInit) {
      // false positive
      throw NotFoundException.getNotFoundInstance();
    }

    float left = (startPatternInfo[1] + startPatternInfo[0]) / 2.0f;
    float right = lastStart + lastPatternSize / 2.0f;

    byte[] rawBytes = Arrays.copyOf(rawCodes, rawCodesSize);

    Result resultObject = new Result(
        result.toString(),
        rawBytes,
        new ResultPoint[]{
            new ResultPoint(left, rowNumber),
            new ResultPoint(right, rowNumber)},
        BarcodeFormat.CODE_128);

    resultObject.putMetadata(ResultMetadataType.SYMBOLOGY_IDENTIFIER, "]C" + symbologyModifier);
    resultObject.putMetadata(ResultMetadataType.READER_INIT, readerInit);

    return resultObject;
  }

}
