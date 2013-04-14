// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Copyright 2010 ZXing authors All rights reserved.
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

#include <zxing/ZXing.h>
#include <zxing/oned/Code128Reader.h>
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/common/Array.h>
#include <zxing/ReaderException.h>
#include <zxing/NotFoundException.h>
#include <zxing/FormatException.h>
#include <zxing/ChecksumException.h>
#include <math.h>
#include <string.h>
#include <sstream>

using std::vector;
using std::string;
using zxing::NotFoundException;
using zxing::FormatException;
using zxing::ChecksumException;
using zxing::Ref;
using zxing::Result;
using zxing::oned::Code128Reader;

// VC++
using zxing::BitArray;

const int Code128Reader::MAX_AVG_VARIANCE = int(PATTERN_MATCH_RESULT_SCALE_FACTOR * 250/1000);
const int Code128Reader::MAX_INDIVIDUAL_VARIANCE = int(PATTERN_MATCH_RESULT_SCALE_FACTOR * 700/1000);

namespace {

const int CODE_SHIFT = 98;
	
const int CODE_CODE_C = 99;
const int CODE_CODE_B = 100;
const int CODE_CODE_A = 101;
	
const int CODE_FNC_1 = 102;
const int CODE_FNC_2 = 97;
const int CODE_FNC_3 = 96;
const int CODE_FNC_4_A = 101;
const int CODE_FNC_4_B = 100;
	
const int CODE_START_A = 103;
const int CODE_START_B = 104;
const int CODE_START_C = 105;
const int CODE_STOP = 106;
			
const int CODE_PATTERNS_LENGTH = 107;
const int CODE_PATTERNS[CODE_PATTERNS_LENGTH][6] = {
  {2, 1, 2, 2, 2, 2}, /* 0 */
  {2, 2, 2, 1, 2, 2},
  {2, 2, 2, 2, 2, 1},
  {1, 2, 1, 2, 2, 3},
  {1, 2, 1, 3, 2, 2},
  {1, 3, 1, 2, 2, 2}, /* 5 */
  {1, 2, 2, 2, 1, 3},
  {1, 2, 2, 3, 1, 2},
  {1, 3, 2, 2, 1, 2},
  {2, 2, 1, 2, 1, 3},
  {2, 2, 1, 3, 1, 2}, /* 10 */
  {2, 3, 1, 2, 1, 2},
  {1, 1, 2, 2, 3, 2},
  {1, 2, 2, 1, 3, 2},
  {1, 2, 2, 2, 3, 1},
  {1, 1, 3, 2, 2, 2}, /* 15 */
  {1, 2, 3, 1, 2, 2},
  {1, 2, 3, 2, 2, 1},
  {2, 2, 3, 2, 1, 1},
  {2, 2, 1, 1, 3, 2},
  {2, 2, 1, 2, 3, 1}, /* 20 */
  {2, 1, 3, 2, 1, 2},
  {2, 2, 3, 1, 1, 2},
  {3, 1, 2, 1, 3, 1},
  {3, 1, 1, 2, 2, 2},
  {3, 2, 1, 1, 2, 2}, /* 25 */
  {3, 2, 1, 2, 2, 1},
  {3, 1, 2, 2, 1, 2},
  {3, 2, 2, 1, 1, 2},
  {3, 2, 2, 2, 1, 1},
  {2, 1, 2, 1, 2, 3}, /* 30 */
  {2, 1, 2, 3, 2, 1},
  {2, 3, 2, 1, 2, 1},
  {1, 1, 1, 3, 2, 3},
  {1, 3, 1, 1, 2, 3},
  {1, 3, 1, 3, 2, 1}, /* 35 */
  {1, 1, 2, 3, 1, 3},
  {1, 3, 2, 1, 1, 3},
  {1, 3, 2, 3, 1, 1},
  {2, 1, 1, 3, 1, 3},
  {2, 3, 1, 1, 1, 3}, /* 40 */
  {2, 3, 1, 3, 1, 1},
  {1, 1, 2, 1, 3, 3},
  {1, 1, 2, 3, 3, 1},
  {1, 3, 2, 1, 3, 1},
  {1, 1, 3, 1, 2, 3}, /* 45 */
  {1, 1, 3, 3, 2, 1},
  {1, 3, 3, 1, 2, 1},
  {3, 1, 3, 1, 2, 1},
  {2, 1, 1, 3, 3, 1},
  {2, 3, 1, 1, 3, 1}, /* 50 */
  {2, 1, 3, 1, 1, 3},
  {2, 1, 3, 3, 1, 1},
  {2, 1, 3, 1, 3, 1},
  {3, 1, 1, 1, 2, 3},
  {3, 1, 1, 3, 2, 1}, /* 55 */
  {3, 3, 1, 1, 2, 1},
  {3, 1, 2, 1, 1, 3},
  {3, 1, 2, 3, 1, 1},
  {3, 3, 2, 1, 1, 1},
  {3, 1, 4, 1, 1, 1}, /* 60 */
  {2, 2, 1, 4, 1, 1},
  {4, 3, 1, 1, 1, 1},
  {1, 1, 1, 2, 2, 4},
  {1, 1, 1, 4, 2, 2},
  {1, 2, 1, 1, 2, 4}, /* 65 */
  {1, 2, 1, 4, 2, 1},
  {1, 4, 1, 1, 2, 2},
  {1, 4, 1, 2, 2, 1},
  {1, 1, 2, 2, 1, 4},
  {1, 1, 2, 4, 1, 2}, /* 70 */
  {1, 2, 2, 1, 1, 4},
  {1, 2, 2, 4, 1, 1},
  {1, 4, 2, 1, 1, 2},
  {1, 4, 2, 2, 1, 1},
  {2, 4, 1, 2, 1, 1}, /* 75 */
  {2, 2, 1, 1, 1, 4},
  {4, 1, 3, 1, 1, 1},
  {2, 4, 1, 1, 1, 2},
  {1, 3, 4, 1, 1, 1},
  {1, 1, 1, 2, 4, 2}, /* 80 */
  {1, 2, 1, 1, 4, 2},
  {1, 2, 1, 2, 4, 1},
  {1, 1, 4, 2, 1, 2},
  {1, 2, 4, 1, 1, 2},
  {1, 2, 4, 2, 1, 1}, /* 85 */
  {4, 1, 1, 2, 1, 2},
  {4, 2, 1, 1, 1, 2},
  {4, 2, 1, 2, 1, 1},
  {2, 1, 2, 1, 4, 1},
  {2, 1, 4, 1, 2, 1}, /* 90 */
  {4, 1, 2, 1, 2, 1},
  {1, 1, 1, 1, 4, 3},
  {1, 1, 1, 3, 4, 1},
  {1, 3, 1, 1, 4, 1},
  {1, 1, 4, 1, 1, 3}, /* 95 */
  {1, 1, 4, 3, 1, 1},
  {4, 1, 1, 1, 1, 3},
  {4, 1, 1, 3, 1, 1},
  {1, 1, 3, 1, 4, 1},
  {1, 1, 4, 1, 3, 1}, /* 100 */
  {3, 1, 1, 1, 4, 1},
  {4, 1, 1, 1, 3, 1},
  {2, 1, 1, 4, 1, 2},
  {2, 1, 1, 2, 1, 4},
  {2, 1, 1, 2, 3, 2}, /* 105 */
  {2, 3, 3, 1, 1, 1}
};

}

Code128Reader::Code128Reader(){}

vector<int> Code128Reader::findStartPattern(Ref<BitArray> row){
  int width = row->getSize();
  int rowOffset = row->getNextSet(0);

  int counterPosition = 0;
  vector<int> counters (6, 0);
  int patternStart = rowOffset;
  bool isWhite = false;
  int patternLength =  counters.size();

  for (int i = rowOffset; i < width; i++) {
    if (row->get(i) ^ isWhite) {
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
        // Look for whitespace before start pattern, >= 50% of width of start pattern
        if (bestMatch >= 0 &&
            row->isRange(std::max(0, patternStart - (i - patternStart) / 2), patternStart, false)) {
          vector<int> resultValue (3, 0);
          resultValue[0] = patternStart;
          resultValue[1] = i;
          resultValue[2] = bestMatch;
          return resultValue;
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
  throw NotFoundException();
}

int Code128Reader::decodeCode(Ref<BitArray> row, vector<int>& counters, int rowOffset) {
  recordPattern(row, rowOffset, counters);
  int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
  int bestMatch = -1;
  for (int d = 0; d < CODE_PATTERNS_LENGTH; d++) {
    int const* const pattern = CODE_PATTERNS[d];
    int variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
    if (variance < bestVariance) {
      bestVariance = variance;
      bestMatch = d;
    }
  }
  // TODO We're overlooking the fact that the STOP pattern has 7 values, not 6.
  if (bestMatch >= 0) {
    return bestMatch;
  } else {
    throw NotFoundException();
  }
}

Ref<Result> Code128Reader::decodeRow(int rowNumber, Ref<BitArray> row) {
  // boolean convertFNC1 = hints != null && hints.containsKey(DecodeHintType.ASSUME_GS1);
  boolean convertFNC1 = false;
  vector<int> startPatternInfo (findStartPattern(row));
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
      throw FormatException();
  }

  bool done = false;
  bool isNextShifted = false;

  string result;
  vector<char> rawCodes(20, 0);

  int lastStart = startPatternInfo[0];
  int nextStart = startPatternInfo[1];
  vector<int> counters (6, 0);

  int lastCode = 0;
  int code = 0;
  int checksumTotal = startCode;
  int multiplier = 0;
  bool lastCharacterWasPrintable = true;

  std::ostringstream oss;

  while (!done) {

    bool unshift = isNextShifted;
    isNextShifted = false;

    // Save off last code
    lastCode = code;

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
    for (int i = 0, e = counters.size(); i < e; i++) {
      nextStart += counters[i];
    }

    // Take care of illegal start codes
    switch (code) {
      case CODE_START_A:
      case CODE_START_B:
      case CODE_START_C:
        throw FormatException();
    }

    switch (codeSet) {

      case CODE_CODE_A:
        if (code < 64) {
          result.append(1, (char) (' ' + code));
        } else if (code < 96) {
          result.append(1, (char) (code - 64));
        } else {
          // Don't let CODE_STOP, which always appears, affect whether whether we think the
          // last code was printable or not.
          if (code != CODE_STOP) {
            lastCharacterWasPrintable = false;
          }
          switch (code) {
            case CODE_FNC_1:
              if (convertFNC1) {
                if (result.length() == 0){
                  // GS1 specification 5.4.3.7. and 5.4.6.4. If the first char after the start code
                  // is FNC1 then this is GS1-128. We add the symbology identifier.
                  result.append("]C1");
                } else {
                  // GS1 specification 5.4.7.5. Every subsequent FNC1 is returned as ASCII 29 (GS)
                  result.append(1, (char) 29);
                }
              }
              break;
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
          result.append(1, (char) (' ' + code));
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
              codeSet = CODE_CODE_A;
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
            result.append(1, '0');
          }
          oss.clear();
          oss.str("");
          oss << code;
          result.append(oss.str());
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
      codeSet = codeSet == CODE_CODE_A ? CODE_CODE_B : CODE_CODE_A;
    }
    
  }

  // Check for ample whitespace following pattern, but, to do this we first need to remember that
  // we fudged decoding CODE_STOP since it actually has 7 bars, not 6. There is a black bar left
  // to read off. Would be slightly better to properly read. Here we just skip it:
  nextStart = row->getNextUnset(nextStart);
  if (!row->isRange(nextStart,
                    std::min(row->getSize(), nextStart + (nextStart - lastStart) / 2),
                    false)) {
    throw NotFoundException();
  }

  // Pull out from sum the value of the penultimate check code
  checksumTotal -= multiplier * lastCode;
  // lastCode is the checksum then:
  if (checksumTotal % 103 != lastCode) {
    throw ChecksumException();
  }

  // Need to pull out the check digits from string
  int resultLength = result.length();
  if (resultLength == 0) {
    // false positive
    throw NotFoundException();
  }

  // Only bother if the result had at least one character, and if the checksum digit happened to
  // be a printable character. If it was just interpreted as a control code, nothing to remove.
  if (resultLength > 0 && lastCharacterWasPrintable) {
    if (codeSet == CODE_CODE_C) {
      result.erase(resultLength - 2, resultLength);
    } else {
      result.erase(resultLength - 1, resultLength);
    }
  }

  float left = (float) (startPatternInfo[1] + startPatternInfo[0]) / 2.0f;
  float right = (float) (nextStart + lastStart) / 2.0f;

  int rawCodesSize = rawCodes.size();
  ArrayRef<char> rawBytes (rawCodesSize);
  for (int i = 0; i < rawCodesSize; i++) {
    rawBytes[i] = rawCodes[i];
  }

  ArrayRef< Ref<ResultPoint> > resultPoints(2);
  resultPoints[0] =
      Ref<OneDResultPoint>(new OneDResultPoint(left, (float) rowNumber));
  resultPoints[1] =
      Ref<OneDResultPoint>(new OneDResultPoint(right, (float) rowNumber));

  return Ref<Result>(new Result(Ref<String>(new String(result)), rawBytes, resultPoints,
                                BarcodeFormat::CODE_128));
}

Code128Reader::~Code128Reader(){}

zxing::BarcodeFormat Code128Reader::getBarcodeFormat(){
  return BarcodeFormat::CODE_128;
}
