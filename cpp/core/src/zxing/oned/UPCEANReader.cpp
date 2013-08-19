// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
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

#include <zxing/ZXing.h>
#include <zxing/oned/UPCEANReader.h>
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/ReaderException.h>
#include <zxing/NotFoundException.h>
#include <zxing/FormatException.h>
#include <zxing/ChecksumException.h>

using std::vector;
using std::string;

using zxing::Ref;
using zxing::Result;
using zxing::NotFoundException;
using zxing::FormatException;
using zxing::ChecksumException;
using zxing::oned::UPCEANReader;

// VC++
using zxing::BitArray;
using zxing::String;

#define LEN(v) ((int)(sizeof(v)/sizeof(v[0])))

namespace {

  /**
   * Start/end guard pattern.
   */
  const int START_END_PATTERN_[] = {1, 1, 1};
  const int START_END_PATTERN_LEN = LEN(START_END_PATTERN_);

  /**
   * Pattern marking the middle of a UPC/EAN pattern, separating the two halves.
   */
  const int MIDDLE_PATTERN_[] = {1, 1, 1, 1, 1};
  const int MIDDLE_PATTERN_LEN = LEN(MIDDLE_PATTERN_);

  /**
   * "Odd", or "L" patterns used to encode UPC/EAN digits.
   */
  const int L_PATTERNS_[][4] = {
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
  const int L_PATTERNS_LEN = LEN(L_PATTERNS_);

  /**
   * As above but also including the "even", or "G" patterns used to encode UPC/EAN digits.
   */
  const int L_AND_G_PATTERNS_[][4] = {
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
  const int L_AND_G_PATTERNS_LEN = LEN(L_AND_G_PATTERNS_);
}

const int UPCEANReader::MAX_AVG_VARIANCE = (int)(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.48f);
const int UPCEANReader::MAX_INDIVIDUAL_VARIANCE = (int)(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);

#define VECTOR_INIT(v) v, v + sizeof(v)/sizeof(v[0])

const vector<int>
UPCEANReader::START_END_PATTERN (VECTOR_INIT(START_END_PATTERN_));

const vector<int>
UPCEANReader::MIDDLE_PATTERN (VECTOR_INIT(MIDDLE_PATTERN_));
const vector<int const*>
UPCEANReader::L_PATTERNS (VECTOR_INIT(L_PATTERNS_));
const vector<int const*>
UPCEANReader::L_AND_G_PATTERNS (VECTOR_INIT(L_AND_G_PATTERNS_));

UPCEANReader::UPCEANReader() {}

Ref<Result> UPCEANReader::decodeRow(int rowNumber, Ref<BitArray> row) {
  return decodeRow(rowNumber, row, findStartGuardPattern(row));
}

Ref<Result> UPCEANReader::decodeRow(int rowNumber,
                                    Ref<BitArray> row,
                                    Range const& startGuardRange) {
  string& result = decodeRowStringBuffer;
  result.clear();
  int endStart = decodeMiddle(row, startGuardRange, result);

  Range endRange = decodeEnd(row, endStart);

  // Make sure there is a quiet zone at least as big as the end pattern after the barcode.
  // The spec might want more whitespace, but in practice this is the maximum we can count on.

  int end = endRange[1];
  int quietEnd = end + (end - endRange[0]);
  if (quietEnd >= row->getSize() || !row->isRange(end, quietEnd, false)) {
    throw NotFoundException();
  }

  // UPC/EAN should never be less than 8 chars anyway
  if (result.length() < 8) {
    throw FormatException();
  }

  Ref<String> resultString (new String(result));
  if (!checkChecksum(resultString)) {
    throw ChecksumException();
  }
  
  float left = (float) (startGuardRange[1] + startGuardRange[0]) / 2.0f;
  float right = (float) (endRange[1] + endRange[0]) / 2.0f;
  BarcodeFormat format = getBarcodeFormat();
  ArrayRef< Ref<ResultPoint> > resultPoints(2);
  resultPoints[0] = Ref<ResultPoint>(new OneDResultPoint(left, (float) rowNumber));
  resultPoints[1] = Ref<ResultPoint>(new OneDResultPoint(right, (float) rowNumber));
  Ref<Result> decodeResult (new Result(resultString, ArrayRef<char>(), resultPoints, format));
  // Java extension and man stuff
  return decodeResult;
}

UPCEANReader::Range UPCEANReader::findStartGuardPattern(Ref<BitArray> row) {
  bool foundStart = false;
  Range startRange;
  int nextStart = 0;
  vector<int> counters(START_END_PATTERN.size(), 0);
  // std::cerr << "fsgp " << *row << std::endl;
  while (!foundStart) {
    for(int i=0; i < (int)START_END_PATTERN.size(); ++i) {
      counters[i] = 0;
    }
    startRange = findGuardPattern(row, nextStart, false, START_END_PATTERN, counters);
    // std::cerr << "sr " << startRange[0] << " " << startRange[1] << std::endl;
    int start = startRange[0];
    nextStart = startRange[1];
    // Make sure there is a quiet zone at least as big as the start pattern before the barcode.
    // If this check would run off the left edge of the image, do not accept this barcode,
    // as it is very likely to be a false positive.
    int quietStart = start - (nextStart - start);
    if (quietStart >= 0) {
      foundStart = row->isRange(quietStart, start, false);
    }
  }
  return startRange;
}

UPCEANReader::Range UPCEANReader::findGuardPattern(Ref<BitArray> row,
                                                   int rowOffset,
                                                   bool whiteFirst,
                                                   vector<int> const& pattern) {
  vector<int> counters (pattern.size(), 0);
  return findGuardPattern(row, rowOffset, whiteFirst, pattern, counters);
}

UPCEANReader::Range UPCEANReader::findGuardPattern(Ref<BitArray> row,
                                                   int rowOffset,
                                                   bool whiteFirst,
                                                   vector<int> const& pattern,
                                                   vector<int>& counters) {
  // cerr << "fGP " << rowOffset  << " " << whiteFirst << endl;
  if (false) {
    for(int i=0; i < (int)pattern.size(); ++i) {
      std::cerr << pattern[i];
    }
    std::cerr << std::endl;
  }
  int patternLength = pattern.size();
  int width = row->getSize();
  bool isWhite = whiteFirst;
  rowOffset = whiteFirst ? row->getNextUnset(rowOffset) : row->getNextSet(rowOffset);
  int counterPosition = 0;
  int patternStart = rowOffset;
  for (int x = rowOffset; x < width; x++) {
    // std::cerr << "rg " << x << " " << row->get(x) << std::endl;
    if (row->get(x) ^ isWhite) {
      counters[counterPosition]++;
    } else {
      if (counterPosition == patternLength - 1) {
        if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
          return Range(patternStart, x);
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

UPCEANReader::Range UPCEANReader::decodeEnd(Ref<BitArray> row, int endStart) {
  return findGuardPattern(row, endStart, false, START_END_PATTERN);
}

int UPCEANReader::decodeDigit(Ref<BitArray> row,
                              vector<int> & counters,
                              int rowOffset,
                              vector<int const*> const& patterns) {
  recordPattern(row, rowOffset, counters);
  int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
  int bestMatch = -1;
  int max = patterns.size();
  for (int i = 0; i < max; i++) {
    int const* pattern (patterns[i]);
    int variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
    if (variance < bestVariance) {
      bestVariance = variance;
      bestMatch = i;
    }
  }
  if (bestMatch >= 0) {
    return bestMatch;
  } else {
    throw NotFoundException();
  }
}

/**
 * @return {@link #checkStandardUPCEANChecksum(String)}
 */
bool UPCEANReader::checkChecksum(Ref<String> const& s) {
  return checkStandardUPCEANChecksum(s);
}

/**
 * Computes the UPC/EAN checksum on a string of digits, and reports
 * whether the checksum is correct or not.
 *
 * @param s string of digits to check
 * @return true iff string of digits passes the UPC/EAN checksum algorithm
 */
bool UPCEANReader::checkStandardUPCEANChecksum(Ref<String> const& s_) {
  std::string const& s (s_->getText());
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
