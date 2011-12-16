/*
 *  EAN13Reader.cpp
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

#include "EAN13Reader.h"
#include <zxing/ReaderException.h>

namespace zxing {
  namespace oned {

    static const int FIRST_DIGIT_ENCODINGS[10] = {
      0x00, 0x0B, 0x0D, 0xE, 0x13, 0x19, 0x1C, 0x15, 0x16, 0x1A
    };

    EAN13Reader::EAN13Reader() { }

    int EAN13Reader::decodeMiddle(Ref<BitArray> row, int startGuardBegin, int startGuardEnd,
        std::string& resultString) {
      const int countersLen = 4;
      int counters[countersLen] = { 0, 0, 0, 0 };

      int end = row->getSize();
      int rowOffset = startGuardEnd;
      int lgPatternFound = 0;

      for (int x = 0; x < 6 && rowOffset < end; x++) {
        int bestMatch = decodeDigit(row, counters, countersLen, rowOffset,
            UPC_EAN_PATTERNS_L_AND_G_PATTERNS);
        if (bestMatch < 0) {
          return -1;
        }
        resultString.append(1, (char) ('0' + bestMatch % 10));
        for (int i = 0; i < countersLen; i++) {
          rowOffset += counters[i];
        }
        if (bestMatch >= 10) {
          lgPatternFound |= 1 << (5 - x);
        }
      }

      if (!determineFirstDigit(resultString, lgPatternFound)) {
        return -1;
      }

      int middleRangeStart;
      int middleRangeEnd;
      if (findGuardPattern(row, rowOffset, true, (int*)getMIDDLE_PATTERN(),
            getMIDDLE_PATTERN_LEN(), &middleRangeStart, &middleRangeEnd)) {
        rowOffset = middleRangeEnd;
        for (int x = 0; x < 6 && rowOffset < end; x++) {
          int bestMatch = decodeDigit(row, counters, countersLen, rowOffset,
              UPC_EAN_PATTERNS_L_PATTERNS);
          if (bestMatch < 0) {
            return -1;
          }
          resultString.append(1, (char) ('0' + bestMatch));
          for (int i = 0; i < countersLen; i++) {
            rowOffset += counters[i];
          }
        }
        return rowOffset;
      }
      return -1;
    }

    bool EAN13Reader::determineFirstDigit(std::string& resultString, int lgPatternFound) {
      for (int d = 0; d < 10; d++) {
        if (lgPatternFound == FIRST_DIGIT_ENCODINGS[d]) {
          resultString.insert((size_t)0, (size_t)1, (char) ('0' + d));
          return true;
        }
      }
      return false;
    }

    BarcodeFormat EAN13Reader::getBarcodeFormat(){
      return BarcodeFormat_EAN_13;
    }
  }
}
