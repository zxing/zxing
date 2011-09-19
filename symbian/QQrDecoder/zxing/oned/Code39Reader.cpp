/*
 *  Code39Reader.cpp
 *  ZXing
 *
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
#include <math.h>
#include <limits.h>

namespace zxing {
namespace oned {

  static const char* ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";


  /**
   * These represent the encodings of characters, as patterns of wide and narrow
   * bars.
   * The 9 least-significant bits of each int correspond to the pattern of wide
   * and narrow, with 1s representing "wide" and 0s representing narrow.
   */
  const int CHARACTER_ENCODINGS_LEN = 44;
  static int CHARACTER_ENCODINGS[CHARACTER_ENCODINGS_LEN] = {
    0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064, // 0-9
    0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C, // A-J
    0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016, // K-T
    0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094, // U-*
    0x0A8, 0x0A2, 0x08A, 0x02A // $-%
  };

  static int ASTERISK_ENCODING = 0x094;
  static const char* ALPHABET_STRING =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";


  /**
   * Creates a reader that assumes all encoded data is data, and does not treat
   * the final character as a check digit. It will not decoded "extended
   * Code 39" sequences.
   */
  Code39Reader::Code39Reader() : alphabet_string(ALPHABET_STRING),
                                 usingCheckDigit(false),
                                 extendedMode(false) {
  }

  /**
   * Creates a reader that can be configured to check the last character as a
   * check digit. It will not decoded "extended Code 39" sequences.
   *
   * @param usingCheckDigit if true, treat the last data character as a check
   * digit, not data, and verify that the checksum passes.
   */
  Code39Reader::Code39Reader(bool usingCheckDigit_) :
    alphabet_string(ALPHABET_STRING),
    usingCheckDigit(usingCheckDigit_),
    extendedMode(false) {
  }


  Code39Reader::Code39Reader(bool usingCheckDigit_, bool extendedMode_) :
    alphabet_string(ALPHABET_STRING),
    usingCheckDigit(usingCheckDigit_),
    extendedMode(extendedMode_) {
  }

  Ref<Result> Code39Reader::decodeRow(int rowNumber, Ref<BitArray> row) {
    int* start = NULL;
    try {
      start = findAsteriskPattern(row);
      int nextStart = start[1];
      int end = row->getSize();

      // Read off white space
      while (nextStart < end && !row->get(nextStart)) {
        nextStart++;
      }

      std::string tmpResultString;

      const int countersLen = 9;
      int counters[countersLen];
      for (int i = 0; i < countersLen; i++) {
        counters[i] = 0;
      }
      char decodedChar;
      int lastStart;
      do {
        if (!recordPattern(row, nextStart, counters, countersLen)) {
          throw ReaderException("");
        }
        int pattern = toNarrowWidePattern(counters, countersLen);
        if (pattern < 0) {
          throw ReaderException("pattern < 0");
        }
        decodedChar = patternToChar(pattern);
        tmpResultString.append(1, decodedChar);
        lastStart = nextStart;
        for (int i = 0; i < countersLen; i++) {
          nextStart += counters[i];
        }
        // Read off white space
        while (nextStart < end && !row->get(nextStart)) {
          nextStart++;
        }
      } while (decodedChar != '*');
      tmpResultString.erase(tmpResultString.length()-1, 1);// remove asterisk

      // Look for whitespace after pattern:
      int lastPatternSize = 0;
      for (int i = 0; i < countersLen; i++) {
        lastPatternSize += counters[i];
      }
      int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;
      // If 50% of last pattern size, following last pattern, is not whitespace,
      // fail (but if it's whitespace to the very end of the image, that's OK)
      if (nextStart != end && whiteSpaceAfterEnd / 2 < lastPatternSize) {
        throw ReaderException("too short end white space");
      }

      if (usingCheckDigit) {
        int max = tmpResultString.length() - 1;
        unsigned int total = 0;
        for (int i = 0; i < max; i++) {
          total += alphabet_string.find_first_of(tmpResultString[i], 0);
        }
        if (total % 43 != alphabet_string.find_first_of(tmpResultString[max], 0)) {
          throw ReaderException("");
        }
        tmpResultString.erase(max, 1);
      }

      Ref<String> resultString(new String(tmpResultString));
      if (extendedMode) {
        resultString = decodeExtended(tmpResultString);
      }

      if (tmpResultString.length() == 0) {
        // Almost surely a false positive
        throw ReaderException("");
      }

      float left = (float) (start[1] + start[0]) / 2.0f;
      float right = (float) (nextStart + lastStart) / 2.0f;

      std::vector< Ref<ResultPoint> > resultPoints(2);
      Ref<OneDResultPoint> resultPoint1(
        new OneDResultPoint(left, (float) rowNumber));
      Ref<OneDResultPoint> resultPoint2(
        new OneDResultPoint(right, (float) rowNumber));
      resultPoints[0] = resultPoint1;
      resultPoints[1] = resultPoint2;

      ArrayRef<unsigned char> resultBytes(1);

      Ref<Result> res(new Result(
                        resultString, resultBytes, resultPoints, BarcodeFormat_CODE_39));

      delete [] start;
      return res;
    } catch (ReaderException const& re) {
      delete [] start;
      return Ref<Result>();
    }
  }

  int* Code39Reader::findAsteriskPattern(Ref<BitArray> row){
    int width = row->getSize();
    int rowOffset = 0;
    while (rowOffset < width) {
      if (row->get(rowOffset)) {
        break;
      }
      rowOffset++;
    }

    int counterPosition = 0;
    const int countersLen = 9;
    int counters[countersLen];
    for (int i = 0; i < countersLen; i++) {
      counters[i] = 0;
    }
    int patternStart = rowOffset;
    bool isWhite = false;
    int patternLength = countersLen;

    for (int i = rowOffset; i < width; i++) {
      bool pixel = row->get(i);
      if (pixel ^ isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (toNarrowWidePattern(counters, countersLen) == ASTERISK_ENCODING) {
            // Look for whitespace before start pattern, >= 50% of width of
            // start pattern.
            long double longPatternOffset =
              fmaxl(0, patternStart - (i - patternStart) / 2);
            if (row->isRange(longPatternOffset, patternStart, false)) {
              int* resultValue = new int[2];
              resultValue[0] = patternStart;
              resultValue[1] = i;
              return resultValue;
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
    throw ReaderException("");
  }

  // For efficiency, returns -1 on failure. Not throwing here saved as many as
  // 700 exceptions per image when using some of our blackbox images.
  int Code39Reader::toNarrowWidePattern(int counters[], int countersLen){
    int numCounters = countersLen;
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
} // namespace oned
} // namespace zxing

