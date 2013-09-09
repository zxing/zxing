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

#include "Code39Reader.h"
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/common/Array.h>
#include <zxing/ReaderException.h>
#include <zxing/NotFoundException.h>
#include <zxing/ChecksumException.h>
#include <math.h>
#include <limits.h>

using std::vector;
using zxing::Ref;
using zxing::Result;
using zxing::String;
using zxing::NotFoundException;
using zxing::ChecksumException;
using zxing::oned::Code39Reader;

// VC++
using zxing::BitArray;

namespace {
  const char* ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";

  /**
   * These represent the encodings of characters, as patterns of wide and narrow
   * bars.
   * The 9 least-significant bits of each int correspond to the pattern of wide
   * and narrow, with 1s representing "wide" and 0s representing narrow.
   */
  const int CHARACTER_ENCODINGS_LEN = 44;
  int CHARACTER_ENCODINGS[CHARACTER_ENCODINGS_LEN] = {
    0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064, // 0-9
    0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C, // A-J
    0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016, // K-T
    0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094, // U-*
    0x0A8, 0x0A2, 0x08A, 0x02A // $-%
  };

  int ASTERISK_ENCODING = 0x094;
  const char* ALPHABET_STRING =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";

  std::string alphabet_string (ALPHABET_STRING);
}

void Code39Reader::init(bool usingCheckDigit_, bool extendedMode_) {
  usingCheckDigit = usingCheckDigit_;
  extendedMode = extendedMode_;
  decodeRowResult.reserve(20);
  counters.resize(9);
}

/**
 * Creates a reader that assumes all encoded data is data, and does not treat
 * the final character as a check digit. It will not decoded "extended
 * Code 39" sequences.
 */
Code39Reader::Code39Reader() {
  init();
}

/**
 * Creates a reader that can be configured to check the last character as a
 * check digit. It will not decoded "extended Code 39" sequences.
 *
 * @param usingCheckDigit if true, treat the last data character as a check
 * digit, not data, and verify that the checksum passes.
 */
Code39Reader::Code39Reader(bool usingCheckDigit_) {
  init(usingCheckDigit_);
}

Code39Reader::Code39Reader(bool usingCheckDigit_, bool extendedMode_) {
  init(usingCheckDigit_, extendedMode_);
}

Ref<Result> Code39Reader::decodeRow(int rowNumber, Ref<BitArray> row) {
  std::vector<int>& theCounters (counters);
  { // Arrays.fill(counters, 0);
    int size = theCounters.size();
    theCounters.resize(0);
    theCounters.resize(size); }
  std::string& result (decodeRowResult);
  result.clear();

  vector<int> start (findAsteriskPattern(row, theCounters));
  // Read off white space
  int nextStart = row->getNextSet(start[1]);
  int end = row->getSize();

  char decodedChar;
  int lastStart;
  do {
    recordPattern(row, nextStart, theCounters);
    int pattern = toNarrowWidePattern(theCounters);
    if (pattern < 0) {
      throw NotFoundException();;
    }
    decodedChar = patternToChar(pattern);
    result.append(1, decodedChar);
    lastStart = nextStart;
    for (int i = 0, end=theCounters.size(); i < end; i++) {
      nextStart += theCounters[i];
    }
    // Read off white space
    nextStart = row->getNextSet(nextStart);
  } while (decodedChar != '*');
  result.resize(decodeRowResult.length()-1);// remove asterisk

    // Look for whitespace after pattern:
  int lastPatternSize = 0;
  for (int i = 0, e = theCounters.size(); i < e; i++) {
    lastPatternSize += theCounters[i];
  }
  int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;
  // If 50% of last pattern size, following last pattern, is not whitespace,
  // fail (but if it's whitespace to the very end of the image, that's OK)
  if (nextStart != end && (whiteSpaceAfterEnd >> 1) < lastPatternSize) {
    throw NotFoundException();
  }

  if (usingCheckDigit) {
    int max = result.length() - 1;
    int total = 0;
    for (int i = 0; i < max; i++) {
      total += alphabet_string.find_first_of(decodeRowResult[i], 0);
    }
    if (result[max] != ALPHABET[total % 43]) {
      throw ChecksumException();
    }
    result.resize(max);
  }
  
  if (result.length() == 0) {
    // Almost false positive
    throw NotFoundException();
  }
  
  Ref<String> resultString;
  if (extendedMode) {
    resultString = decodeExtended(result);
  } else {
    resultString = Ref<String>(new String(result));
  }

  float left = (float) (start[1] + start[0]) / 2.0f;
  float right = lastStart + lastPatternSize / 2.0f;

  ArrayRef< Ref<ResultPoint> > resultPoints (2);
  resultPoints[0] = 
    Ref<OneDResultPoint>(new OneDResultPoint(left, (float) rowNumber));
  resultPoints[1] =
    Ref<OneDResultPoint>(new OneDResultPoint(right, (float) rowNumber));
  
  return Ref<Result>(
    new Result(resultString, ArrayRef<char>(), resultPoints, BarcodeFormat::CODE_39)
    );
}

vector<int> Code39Reader::findAsteriskPattern(Ref<BitArray> row, vector<int>& counters){
  int width = row->getSize();
  int rowOffset = row->getNextSet(0);

  int counterPosition = 0;
  int patternStart = rowOffset;
  bool isWhite = false;
  int patternLength = counters.size();

  for (int i = rowOffset; i < width; i++) {
    if (row->get(i) ^ isWhite) {
      counters[counterPosition]++;
    } else {
      if (counterPosition == patternLength - 1) {
        // Look for whitespace before start pattern, >= 50% of width of
        // start pattern.
        if (toNarrowWidePattern(counters) == ASTERISK_ENCODING &&
            row->isRange(std::max(0, patternStart - ((i - patternStart) >> 1)), patternStart, false)) {
          vector<int> resultValue (2, 0);
          resultValue[0] = patternStart;
          resultValue[1] = i;
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

// For efficiency, returns -1 on failure. Not throwing here saved as many as
// 700 exceptions per image when using some of our blackbox images.
int Code39Reader::toNarrowWidePattern(vector<int>& counters){
  int numCounters = counters.size();
  int maxNarrowCounter = 0;
  int wideCounters;
  do {
    int minCounter = INT_MAX;
    for (int i = 0; i < numCounters; i++) {
      int counter = counters[i];
      if (counter < minCounter && counter > maxNarrowCounter) {
        minCounter = counter;
      }
    }
    maxNarrowCounter = minCounter;
    wideCounters = 0;
    int totalWideCountersWidth = 0;
    int pattern = 0;
    for (int i = 0; i < numCounters; i++) {
      int counter = counters[i];
      if (counters[i] > maxNarrowCounter) {
        pattern |= 1 << (numCounters - 1 - i);
        wideCounters++;
        totalWideCountersWidth += counter;
      }
    }
    if (wideCounters == 3) {
      // Found 3 wide counters, but are they close enough in width?
      // We can perform a cheap, conservative check to see if any individual
      // counter is more than 1.5 times the average:
      for (int i = 0; i < numCounters && wideCounters > 0; i++) {
        int counter = counters[i];
        if (counters[i] > maxNarrowCounter) {
          wideCounters--;
          // totalWideCountersWidth = 3 * average, so this checks if
          // counter >= 3/2 * average.
          if ((counter << 1) >= totalWideCountersWidth) {
            return -1;
          }
        }
      }
      return pattern;
    }
  } while (wideCounters > 3);
  return -1;
}

char Code39Reader::patternToChar(int pattern){
  for (int i = 0; i < CHARACTER_ENCODINGS_LEN; i++) {
    if (CHARACTER_ENCODINGS[i] == pattern) {
      return ALPHABET[i];
    }
  }
  throw ReaderException("");
}

Ref<String> Code39Reader::decodeExtended(std::string encoded){
  int length = encoded.length();
  std::string tmpDecoded;
  for (int i = 0; i < length; i++) {
    char c = encoded[i];
    if (c == '+' || c == '$' || c == '%' || c == '/') {
      char next = encoded[i + 1];
      char decodedChar = '\0';
      switch (c) {
      case '+':
        // +A to +Z map to a to z
        if (next >= 'A' && next <= 'Z') {
          decodedChar = (char) (next + 32);
        } else {
          throw ReaderException("");
        }
        break;
      case '$':
        // $A to $Z map to control codes SH to SB
        if (next >= 'A' && next <= 'Z') {
          decodedChar = (char) (next - 64);
        } else {
          throw ReaderException("");
        }
        break;
      case '%':
        // %A to %E map to control codes ESC to US
        if (next >= 'A' && next <= 'E') {
          decodedChar = (char) (next - 38);
        } else if (next >= 'F' && next <= 'W') {
          decodedChar = (char) (next - 11);
        } else {
          throw ReaderException("");
        }
        break;
      case '/':
        // /A to /O map to ! to , and /Z maps to :
        if (next >= 'A' && next <= 'O') {
          decodedChar = (char) (next - 32);
        } else if (next == 'Z') {
          decodedChar = ':';
        } else {
          throw ReaderException("");
        }
        break;
      }
      tmpDecoded.append(1, decodedChar);
      // bump up i again since we read two characters
      i++;
    } else {
      tmpDecoded.append(1, c);
    }
  }
  Ref<String> decoded(new String(tmpDecoded));
  return decoded;
}
