/*
 *  UPCEANReader.cpp
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

#include "UPCEANReader.h"
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/ReaderException.h>

namespace zxing {
  namespace oned {

    /**
     * Start/end guard pattern.
     */
    static const int START_END_PATTERN[3] = {1, 1, 1};

    /**
     * Pattern marking the middle of a UPC/EAN pattern, separating the two halves.
     */
    static const int MIDDLE_PATTERN_LEN = 5;
    static const int MIDDLE_PATTERN[MIDDLE_PATTERN_LEN] = {1, 1, 1, 1, 1};

    /**
     * "Odd", or "L" patterns used to encode UPC/EAN digits.
     */
    const int L_PATTERNS_LEN = 10;
    const int L_PATTERNS_SUB_LEN = 4;
    const int L_PATTERNS[L_PATTERNS_LEN][L_PATTERNS_SUB_LEN] = {
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

    /**
     * As above but also including the "even", or "G" patterns used to encode UPC/EAN digits.
     */
    const int L_AND_G_PATTERNS_LEN = 20;
    const int L_AND_G_PATTERNS_SUB_LEN = 4;
    const int L_AND_G_PATTERNS[L_AND_G_PATTERNS_LEN][L_AND_G_PATTERNS_SUB_LEN] = {
      {3, 2, 1, 1}, // 0
      {2, 2, 2, 1}, // 1
      {2, 1, 2, 2}, // 2
      {1, 4, 1, 1}, // 3
      {1, 1, 3, 2}, // 4
      {1, 2, 3, 1}, // 5
      {1, 1, 1, 4}, // 6
      {1, 3, 1, 2}, // 7
      {1, 2, 1, 3}, // 8
      {3, 1, 1, 2}, // 9
      {1, 1, 2, 3}, // 10 reversed 0
      {1, 2, 2, 2}, // 11 reversed 1
      {2, 2, 1, 2}, // 12 reversed 2
      {1, 1, 4, 1}, // 13 reversed 3
      {2, 3, 1, 1}, // 14 reversed 4
      {1, 3, 2, 1}, // 15 reversed 5
      {4, 1, 1, 1}, // 16 reversed 6
      {2, 1, 3, 1}, // 17 reversed 7
      {3, 1, 2, 1}, // 18 reversed 8
      {2, 1, 1, 3}  // 19 reversed 9
    };


    const int UPCEANReader::getMIDDLE_PATTERN_LEN() {
      return MIDDLE_PATTERN_LEN;
    }

    const int* UPCEANReader::getMIDDLE_PATTERN() {
      return MIDDLE_PATTERN;
    }

    UPCEANReader::UPCEANReader() {
    }


    Ref<Result> UPCEANReader::decodeRow(int rowNumber, Ref<BitArray> row) {
      int rangeStart;
      int rangeEnd;
			if (findStartGuardPattern(row, &rangeStart, &rangeEnd)) {
        try {
          return decodeRow(rowNumber, row, rangeStart, rangeEnd);
        } catch (ReaderException const& re) {
        }
			}
			return Ref<Result>();
    }

    Ref<Result> UPCEANReader::decodeRow(int rowNumber, Ref<BitArray> row, int startGuardBegin,
        int startGuardEnd) {
      std::string tmpResultString;
      std::string& tmpResultStringRef = tmpResultString;
      int endStart = decodeMiddle(row, startGuardBegin, startGuardEnd, tmpResultStringRef);
      if (endStart < 0) {
        return Ref<Result>();
      }

      int endGuardBegin;
      int endGuardEnd;
      if (!decodeEnd(row, endStart, &endGuardBegin, &endGuardEnd)) {
        return Ref<Result>();
      }

      // Make sure there is a quiet zone at least as big as the end pattern after the barcode.
      // The spec might want more whitespace, but in practice this is the maximum we can count on.
      size_t quietEnd = endGuardEnd + (endGuardEnd - endGuardBegin);
      if (quietEnd >= row->getSize() || !row->isRange(endGuardEnd, quietEnd, false)) {
        return Ref<Result>();
      }

      if (!checkChecksum(tmpResultString)) {
        return Ref<Result>();
      }

      Ref<String> resultString(new String(tmpResultString));
      float left = (float) (startGuardBegin + startGuardEnd) / 2.0f;
      float right = (float) (endGuardBegin + endGuardEnd) / 2.0f;

      std::vector< Ref<ResultPoint> > resultPoints(2);
      Ref<OneDResultPoint> resultPoint1(new OneDResultPoint(left, (float) rowNumber));
      Ref<OneDResultPoint> resultPoint2(new OneDResultPoint(right, (float) rowNumber));
      resultPoints[0] = resultPoint1;
      resultPoints[1] = resultPoint2;

      ArrayRef<unsigned char> resultBytes(1);
      return Ref<Result>(new Result(resultString, resultBytes, resultPoints, getBarcodeFormat()));
    }

    bool UPCEANReader::findStartGuardPattern(Ref<BitArray> row, int* rangeStart, int* rangeEnd) {
      int nextStart = 0;
      while (findGuardPattern(row, nextStart, false, START_END_PATTERN,
          sizeof(START_END_PATTERN) / sizeof(int), rangeStart, rangeEnd)) {
        int start = *rangeStart;
        nextStart = *rangeEnd;
        // Make sure there is a quiet zone at least as big as the start pattern before the barcode.
        // If this check would run off the left edge of the image, do not accept this barcode,
        // as it is very likely to be a false positive.
        int quietStart = start - (nextStart - start);
        if (quietStart >= 0 && row->isRange(quietStart, start, false)) {
          return true;
        }
      }
      return false;
    }

    bool UPCEANReader::findGuardPattern(Ref<BitArray> row, int rowOffset, bool whiteFirst,
        const int pattern[], int patternLen, int* start, int* end) {
      int patternLength = patternLen;
      int counters[patternLength];
      int countersCount = sizeof(counters) / sizeof(int);
      for (int i = 0; i < countersCount; i++) {
        counters[i] = 0;
      }
      int width = row->getSize();
      bool isWhite = false;
      while (rowOffset < width) {
        isWhite = !row->get(rowOffset);
        if (whiteFirst == isWhite) {
          break;
        }
        rowOffset++;
      }

      int counterPosition = 0;
      int patternStart = rowOffset;
      for (int x = rowOffset; x < width; x++) {
        bool pixel = row->get(x);
        if (pixel ^ isWhite) {
          counters[counterPosition]++;
        } else {
          if (counterPosition == patternLength - 1) {
            if (patternMatchVariance(counters, countersCount, pattern,
                MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
              *start = patternStart;
              *end = x;
              return true;
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
      return false;
    }

    bool UPCEANReader::decodeEnd(Ref<BitArray> row, int endStart, int* endGuardBegin,
        int* endGuardEnd) {
      return findGuardPattern(row, endStart, false, START_END_PATTERN,
          sizeof(START_END_PATTERN) / sizeof(int), endGuardBegin, endGuardEnd);
    }

    int UPCEANReader::decodeDigit(Ref<BitArray> row, int counters[], int countersLen, int rowOffset,
        UPC_EAN_PATTERNS patternType) {
      if (!recordPattern(row, rowOffset, counters, countersLen)) {
        return -1;
      }
      unsigned int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
      int bestMatch = -1;

      int max = 0;
      switch (patternType) {
        case UPC_EAN_PATTERNS_L_PATTERNS:
          max = L_PATTERNS_LEN;
          for (int i = 0; i < max; i++) {
            int pattern[countersLen];
            for(int j = 0; j< countersLen; j++){
              pattern[j] = L_PATTERNS[i][j];
            }

            unsigned int variance = patternMatchVariance(counters, countersLen, pattern,
                MAX_INDIVIDUAL_VARIANCE);
            if (variance < bestVariance) {
              bestVariance = variance;
              bestMatch = i;
            }
          }
          break;
        case UPC_EAN_PATTERNS_L_AND_G_PATTERNS:
          max = L_AND_G_PATTERNS_LEN;
          for (int i = 0; i < max; i++) {
            int pattern[countersLen];
            for(int j = 0; j< countersLen; j++){
              pattern[j] = L_AND_G_PATTERNS[i][j];
            }

            unsigned int variance = patternMatchVariance(counters, countersLen, pattern,
                MAX_INDIVIDUAL_VARIANCE);
            if (variance < bestVariance) {
              bestVariance = variance;
              bestMatch = i;
            }
          }
          break;
        default:
          break;
      }
      return bestMatch;
    }

    /**
     * @return {@link #checkStandardUPCEANChecksum(String)}
     */
    bool UPCEANReader::checkChecksum(std::string s) {
      return checkStandardUPCEANChecksum(s);
    }

    /**
     * Computes the UPC/EAN checksum on a string of digits, and reports
     * whether the checksum is correct or not.
     *
     * @param s string of digits to check
     * @return true iff string of digits passes the UPC/EAN checksum algorithm
     */
    bool UPCEANReader::checkStandardUPCEANChecksum(std::string s) {
      int length = s.length();
      if (length == 0) {
        return false;
      }

      int sum = 0;
      for (int i = length - 2; i >= 0; i -= 2) {
        int digit = (int) s[i] - (int) '0';
        if (digit < 0 || digit > 9) {
          return false;
        }
        sum += digit;
      }
      sum *= 3;
      for (int i = length - 1; i >= 0; i -= 2) {
        int digit = (int) s[i] - (int) '0';
        if (digit < 0 || digit > 9) {
          return false;
        }
        sum += digit;
      }
      return sum % 10 == 0;
    }

    UPCEANReader::~UPCEANReader() {
    }
  }
}
