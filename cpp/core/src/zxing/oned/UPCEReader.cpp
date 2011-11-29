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

#include "UPCEReader.h"
#include <zxing/ReaderException.h>

namespace zxing {
  namespace oned {

    /**
     * The pattern that marks the middle, and end, of a UPC-E pattern.
     * There is no "second half" to a UPC-E barcode.
     */
    static const int MIDDLE_END_PATTERN[6] = {1, 1, 1, 1, 1, 1};

    /**
     * See {@link #L_AND_G_PATTERNS}; these values similarly represent patterns of
     * even-odd parity encodings of digits that imply both the number system (0 or 1)
     * used, and the check digit.
     */
    static const int NUMSYS_AND_CHECK_DIGIT_PATTERNS[2][10] = {
      {0x38, 0x34, 0x32, 0x31, 0x2C, 0x26, 0x23, 0x2A, 0x29, 0x25},
      {0x07, 0x0B, 0x0D, 0x0E, 0x13, 0x19, 0x1C, 0x15, 0x16, 0x1A}
    };

    UPCEReader::UPCEReader() {
    }

    int UPCEReader::decodeMiddle(Ref<BitArray> row, int startGuardBegin, int startGuardEnd,
        std::string& resultString) {
      (void)startGuardBegin;
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

      if (!determineNumSysAndCheckDigit(resultString, lgPatternFound)) {
        return -1;
      }
      return rowOffset;
    }

    bool UPCEReader::decodeEnd(Ref<BitArray> row, int endStart, int* endGuardBegin,
        int* endGuardEnd) {
      return findGuardPattern(row, endStart, true, MIDDLE_END_PATTERN,
          sizeof(MIDDLE_END_PATTERN) / sizeof(int), endGuardBegin, endGuardEnd);
    }

    bool UPCEReader::checkChecksum(std::string s){
      return UPCEANReader::checkChecksum(convertUPCEtoUPCA(s));
    }


    bool UPCEReader::determineNumSysAndCheckDigit(std::string& resultString, int lgPatternFound) {
      for (int numSys = 0; numSys <= 1; numSys++) {
        for (int d = 0; d < 10; d++) {
          if (lgPatternFound == NUMSYS_AND_CHECK_DIGIT_PATTERNS[numSys][d]) {
            resultString.insert(0, 1, (char) ('0' + numSys));
            resultString.append(1, (char) ('0' + d));
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Expands a UPC-E value back into its full, equivalent UPC-A code value.
     *
     * @param upce UPC-E code as string of digits
     * @return equivalent UPC-A code as string of digits
     */
    std::string UPCEReader::convertUPCEtoUPCA(std::string upce) {
      std::string result;
      result.append(1, upce[0]);
      char lastChar = upce[6];
      switch (lastChar) {
        case '0':
        case '1':
        case '2':
          result.append(upce.substr(1,2));
          result.append(1, lastChar);
          result.append("0000");
          result.append(upce.substr(3,3));
          break;
        case '3':
          result.append(upce.substr(1,3));
          result.append("00000");
          result.append(upce.substr(4,2));
          break;
        case '4':
          result.append(upce.substr(1,4));
          result.append("00000");
          result.append(1, upce[5]);
          break;
        default:
          result.append(upce.substr(1,5));
          result.append("0000");
          result.append(1, lastChar);
          break;
      }
      result.append(1, upce[7]);
      return result;
    }


    BarcodeFormat UPCEReader::getBarcodeFormat() {
      return BarcodeFormat_UPC_E;
    }
  }
}
