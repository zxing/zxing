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
#include <zxing/oned/CodaBarReader.h>
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/common/Array.h>
#include <zxing/ReaderException.h>
#include <zxing/NotFoundException.h>
#include <zxing/FormatException.h>
#include <zxing/ChecksumException.h>
#include <math.h>
#include <sstream>

using std::vector;
using std::string;
using zxing::NotFoundException;
using zxing::FormatException;
using zxing::ChecksumException;
using zxing::Ref;
using zxing::Result;
using zxing::oned::CodaBarReader;

// VC++
using zxing::BitArray;

namespace {
  char const ALPHABET_STRING[] = "0123456789-$:/.+ABCD";
  char const* const ALPHABET = ALPHABET_STRING;

  /**
   * These represent the encodings of characters, as patterns of wide and narrow bars. The 7 least-significant bits of
   * each int correspond to the pattern of wide and narrow, with 1s representing "wide" and 0s representing narrow.
   */
  const int CHARACTER_ENCODINGS[] = {
    0x003, 0x006, 0x009, 0x060, 0x012, 0x042, 0x021, 0x024, 0x030, 0x048, // 0-9
    0x00c, 0x018, 0x045, 0x051, 0x054, 0x015, 0x01A, 0x029, 0x00B, 0x00E, // -$:/.+ABCD
  };

  // minimal number of characters that should be present (inclusing start and stop characters)
  // under normal circumstances this should be set to 3, but can be set higher
  // as a last-ditch attempt to reduce false positives.
  const int MIN_CHARACTER_LENGTH = 3;

  // official start and end patterns
  const char STARTEND_ENCODING[] = {'A', 'B', 'C', 'D', 0};
  // some codabar generator allow the codabar string to be closed by every
  // character. This will cause lots of false positives!

  // some industries use a checksum standard but this is not part of the original codabar standard
  // for more information see : http://www.mecsw.com/specs/codabar.html
}

// These values are critical for determining how permissive the decoding
// will be. All stripe sizes must be within the window these define, as
// compared to the average stripe size.
const int CodaBarReader::MAX_ACCEPTABLE =
  (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 2.0f);
const int CodaBarReader::PADDING = 
  (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 1.5f);

CodaBarReader::CodaBarReader() 
  : counters(80, 0), counterLength(0) {}

Ref<Result> CodaBarReader::decodeRow(int rowNumber, Ref<BitArray> row) {

  { // Arrays.fill(counters, 0);
    int size = counters.size();
    counters.resize(0);
    counters.resize(size); }

  setCounters(row);
  int startOffset = findStartPattern();
  int nextStart = startOffset;

  decodeRowResult.clear();
  do {
    int charOffset = toNarrowWidePattern(nextStart);
    if (charOffset == -1) {
      throw NotFoundException();
    }
    // Hack: We store the position in the alphabet table into a
    // StringBuilder, so that we can access the decoded patterns in
    // validatePattern. We'll translate to the actual characters later.
    decodeRowResult.append(1, (char)charOffset);
    nextStart += 8;
    // Stop as soon as we see the end character.
    if (decodeRowResult.length() > 1 &&
        arrayContains(STARTEND_ENCODING, ALPHABET[charOffset])) {
      break;
    }
  } while (nextStart < counterLength); // no fixed end pattern so keep on reading while data is available

  // Look for whitespace after pattern:
  int trailingWhitespace = counters[nextStart - 1];
  int lastPatternSize = 0;
  for (int i = -8; i < -1; i++) {
    lastPatternSize += counters[nextStart + i];
  }

  // We need to see whitespace equal to 50% of the last pattern size,
  // otherwise this is probably a false positive. The exception is if we are
  // at the end of the row. (I.e. the barcode barely fits.)
  if (nextStart < counterLength && trailingWhitespace < lastPatternSize / 2) {
    throw NotFoundException();
  }

  validatePattern(startOffset);

  // Translate character table offsets to actual characters.
  for (int i = 0; i < (int)decodeRowResult.length(); i++) {
    decodeRowResult[i] = ALPHABET[(int)decodeRowResult[i]];
  }
  // Ensure a valid start and end character
  char startchar = decodeRowResult[0];
  if (!arrayContains(STARTEND_ENCODING, startchar)) {
    throw NotFoundException();
  }
  char endchar = decodeRowResult[decodeRowResult.length() - 1];
  if (!arrayContains(STARTEND_ENCODING, endchar)) {
    throw NotFoundException();
  }

  // remove stop/start characters character and check if a long enough string is contained
  if ((int)decodeRowResult.length() <= MIN_CHARACTER_LENGTH) {
    // Almost surely a false positive ( start + stop + at least 1 character)
    throw NotFoundException();
  }

  decodeRowResult.erase(decodeRowResult.length() - 1, 1);
  decodeRowResult.erase(0, 1);

  int runningCount = 0;
  for (int i = 0; i < startOffset; i++) {
    runningCount += counters[i];
  }
  float left = (float) runningCount;
  for (int i = startOffset; i < nextStart - 1; i++) {
    runningCount += counters[i];
  }
  float right = (float) runningCount;

  ArrayRef< Ref<ResultPoint> > resultPoints(2);
  resultPoints[0] =
    Ref<OneDResultPoint>(new OneDResultPoint(left, (float) rowNumber));
  resultPoints[1] =
    Ref<OneDResultPoint>(new OneDResultPoint(right, (float) rowNumber));

  return Ref<Result>(new Result(Ref<String>(new String(decodeRowResult)),
                                ArrayRef<char>(),
                                resultPoints,
                                BarcodeFormat::CODABAR));
}

void CodaBarReader::validatePattern(int start)  {
  // First, sum up the total size of our four categories of stripe sizes;
  vector<int> sizes (4, 0);
  vector<int> counts (4, 0);
  int end = decodeRowResult.length() - 1;

  // We break out of this loop in the middle, in order to handle
  // inter-character spaces properly.
  int pos = start;
  for (int i = 0; true; i++) {
    int pattern = CHARACTER_ENCODINGS[(int)decodeRowResult[i]];
    for (int j = 6; j >= 0; j--) {
      // Even j = bars, while odd j = spaces. Categories 2 and 3 are for
      // long stripes, while 0 and 1 are for short stripes.
      int category = (j & 1) + (pattern & 1) * 2;
      sizes[category] += counters[pos + j];
      counts[category]++;
      pattern >>= 1;
    }
    if (i >= end) {
      break;
    }
    // We ignore the inter-character space - it could be of any size.
    pos += 8;
  }

  // Calculate our allowable size thresholds using fixed-point math.
  vector<int> maxes (4, 0);
  vector<int> mins (4, 0);
  // Define the threshold of acceptability to be the midpoint between the
  // average small stripe and the average large stripe. No stripe lengths
  // should be on the "wrong" side of that line.
  for (int i = 0; i < 2; i++) {
    mins[i] = 0;  // Accept arbitrarily small "short" stripes.
    mins[i + 2] = ((sizes[i] << INTEGER_MATH_SHIFT) / counts[i] +
                   (sizes[i + 2] << INTEGER_MATH_SHIFT) / counts[i + 2]) >> 1;
    maxes[i] = mins[i + 2];
    maxes[i + 2] = (sizes[i + 2] * MAX_ACCEPTABLE + PADDING) / counts[i + 2];
  }

  // Now verify that all of the stripes are within the thresholds.
  pos = start;
  for (int i = 0; true; i++) {
    int pattern = CHARACTER_ENCODINGS[(int)decodeRowResult[i]];
    for (int j = 6; j >= 0; j--) {
      // Even j = bars, while odd j = spaces. Categories 2 and 3 are for
      // long stripes, while 0 and 1 are for short stripes.
      int category = (j & 1) + (pattern & 1) * 2;
      int size = counters[pos + j] << INTEGER_MATH_SHIFT;
      if (size < mins[category] || size > maxes[category]) {
        throw NotFoundException();
      }
      pattern >>= 1;
    }
    if (i >= end) {
      break;
    }
    pos += 8;
  }
}

/**
 * Records the size of all runs of white and black pixels, starting with white.
 * This is just like recordPattern, except it records all the counters, and
 * uses our builtin "counters" member for storage.
 * @param row row to count from
 */
void CodaBarReader::setCounters(Ref<BitArray> row)  {
  counterLength = 0;
  // Start from the first white bit.
  int i = row->getNextUnset(0);
  int end = row->getSize();
  if (i >= end) {
    throw NotFoundException();
  }
  bool isWhite = true;
  int count = 0;
  for (; i < end; i++) {
    if (row->get(i) ^ isWhite) { // that is, exactly one is true
      count++;
    } else {
      counterAppend(count);
      count = 1;
      isWhite = !isWhite;
    }
  }
  counterAppend(count);
}

void CodaBarReader::counterAppend(int e) {
  if (counterLength < (int)counters.size()) {
    counters[counterLength] = e;
  } else {
    counters.push_back(e);
  }
  counterLength++;
}

int CodaBarReader::findStartPattern() {
  for (int i = 1; i < counterLength; i += 2) {
    int charOffset = toNarrowWidePattern(i);
    if (charOffset != -1 && arrayContains(STARTEND_ENCODING, ALPHABET[charOffset])) {
      // Look for whitespace before start pattern, >= 50% of width of start pattern
      // We make an exception if the whitespace is the first element.
      int patternSize = 0;
      for (int j = i; j < i + 7; j++) {
        patternSize += counters[j];
      }
      if (i == 1 || counters[i-1] >= patternSize / 2) {
        return i;
      }
    }
  }
  throw NotFoundException();
}

bool CodaBarReader::arrayContains(char const array[], char key) {
  return strchr(array, key) != 0;
}


int CodaBarReader::toNarrowWidePattern(int position) {
  int end = position + 7;
  if (end >= counterLength) {
    return -1;
  }

  vector<int>& theCounters = counters;

  int maxBar = 0;
  int minBar = std::numeric_limits<int>::max();
  for (int j = position; j < end; j += 2) {
    int currentCounter = theCounters[j];
    if (currentCounter < minBar) {
      minBar = currentCounter;
    }
    if (currentCounter > maxBar) {
      maxBar = currentCounter;
    }
  }
  int thresholdBar = (minBar + maxBar) / 2;

  int maxSpace = 0;
  int minSpace = std::numeric_limits<int>::max();
  for (int j = position + 1; j < end; j += 2) {
    int currentCounter = theCounters[j];
    if (currentCounter < minSpace) {
      minSpace = currentCounter;
    }
    if (currentCounter > maxSpace) {
      maxSpace = currentCounter;
    }
  }
  int thresholdSpace = (minSpace + maxSpace) / 2;

  int bitmask = 1 << 7;
  int pattern = 0;
  for (int i = 0; i < 7; i++) {
    int threshold = (i & 1) == 0 ? thresholdBar : thresholdSpace;
    bitmask >>= 1;
    if (theCounters[position + i] > threshold) {
      pattern |= bitmask;
    }
  }

  for (int i = 0; i < ZXING_ARRAY_LEN(CHARACTER_ENCODINGS); i++) {
    if (CHARACTER_ENCODINGS[i] == pattern) {
      return i;
    }
  }
  return -1;
}
