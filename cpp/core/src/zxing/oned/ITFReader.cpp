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
#include <zxing/oned/ITFReader.h>
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/common/Array.h>
#include <zxing/ReaderException.h>
#include <zxing/FormatException.h>
#include <zxing/NotFoundException.h>
#include <math.h>

using std::vector;
using zxing::Ref;
using zxing::ArrayRef;
using zxing::Array;
using zxing::Result;
using zxing::FormatException;
using zxing::NotFoundException;
using zxing::oned::ITFReader;

// VC++
using zxing::BitArray;

#define VECTOR_INIT(v) v, v + sizeof(v)/sizeof(v[0])

namespace {

const int W = 3; // Pixel width of a wide line
const int N = 1; // Pixed width of a narrow line

const int DEFAULT_ALLOWED_LENGTHS_[] =
{ 48, 44, 24, 20, 18, 16, 14, 12, 10, 8, 6 };
const ArrayRef<int> DEFAULT_ALLOWED_LENGTHS (new Array<int>(VECTOR_INIT(DEFAULT_ALLOWED_LENGTHS_)));

/**
 * Start/end guard pattern.
 *
 * Note: The end pattern is reversed because the row is reversed before
 * searching for the END_PATTERN
 */
const int START_PATTERN_[] = {N, N, N, N};
const vector<int> START_PATTERN (VECTOR_INIT(START_PATTERN_));

const int END_PATTERN_REVERSED_[] = {N, N, W};
const vector<int> END_PATTERN_REVERSED (VECTOR_INIT(END_PATTERN_REVERSED_));

/**
 * Patterns of Wide / Narrow lines to indicate each digit
 */
const int PATTERNS[][5] = {
  {N, N, W, W, N}, // 0
  {W, N, N, N, W}, // 1
  {N, W, N, N, W}, // 2
  {W, W, N, N, N}, // 3
  {N, N, W, N, W}, // 4
  {W, N, W, N, N}, // 5
  {N, W, W, N, N}, // 6
  {N, N, N, W, W}, // 7
  {W, N, N, W, N}, // 8
  {N, W, N, W, N}  // 9
};

}

ITFReader::ITFReader() : narrowLineWidth(-1) {
}


Ref<Result> ITFReader::decodeRow(int rowNumber, Ref<BitArray> row) {
  // Find out where the Middle section (payload) starts & ends

  Range startRange = decodeStart(row);
  Range endRange = decodeEnd(row);

  std::string result;
  decodeMiddle(row, startRange[1], endRange[0], result);
  Ref<String> resultString(new String(result));

  ArrayRef<int> allowedLengths;
  // Java hints stuff missing
  if (!allowedLengths) {
    allowedLengths = DEFAULT_ALLOWED_LENGTHS;
  }

  // To avoid false positives with 2D barcodes (and other patterns), make
  // an assumption that the decoded string must be 6, 10 or 14 digits.
  int length = resultString->size();
  bool lengthOK = false;
  for (int i = 0, e = allowedLengths->size(); i < e; i++) {
    if (length == allowedLengths[i]) {
      lengthOK = true;
      break;
    }
  }

  if (!lengthOK) {
    throw FormatException();
  }

  ArrayRef< Ref<ResultPoint> > resultPoints(2);
  resultPoints[0] =
      Ref<OneDResultPoint>(new OneDResultPoint(float(startRange[1]), float(rowNumber)));
  resultPoints[1] =
      Ref<OneDResultPoint>(new OneDResultPoint(float(endRange[0]), float(rowNumber)));
  return Ref<Result>(new Result(resultString, ArrayRef<char>(), resultPoints, BarcodeFormat::ITF));
}

/**
 * @param row          row of black/white values to search
 * @param payloadStart offset of start pattern
 * @param resultString {@link StringBuffer} to append decoded chars to
 * @throws ReaderException if decoding could not complete successfully
 */
void ITFReader::decodeMiddle(Ref<BitArray> row,
                             int payloadStart,
                             int payloadEnd,
                             std::string& resultString) {
  // Digits are interleaved in pairs - 5 black lines for one digit, and the
  // 5
  // interleaved white lines for the second digit.
  // Therefore, need to scan 10 lines and then
  // split these into two arrays
  vector<int> counterDigitPair(10, 0);
  vector<int> counterBlack(5, 0);
  vector<int> counterWhite(5, 0);

  while (payloadStart < payloadEnd) {

    // Get 10 runs of black/white.
    recordPattern(row, payloadStart, counterDigitPair);
    // Split them into each array
    for (int k = 0; k < 5; k++) {
      int twoK = k << 1;
      counterBlack[k] = counterDigitPair[twoK];
      counterWhite[k] = counterDigitPair[twoK + 1];
    }

    int bestMatch = decodeDigit(counterBlack);
    resultString.append(1, (char) ('0' + bestMatch));
    bestMatch = decodeDigit(counterWhite);
    resultString.append(1, (char) ('0' + bestMatch));

    for (int i = 0, e = counterDigitPair.size(); i < e; i++) {
      payloadStart += counterDigitPair[i];
    }
  }
}

/**
 * Identify where the start of the middle / payload section starts.
 *
 * @param row row of black/white values to search
 * @return Array, containing index of start of 'start block' and end of
 *         'start block'
 * @throws ReaderException
 */
ITFReader::Range ITFReader::decodeStart(Ref<BitArray> row) {
  int endStart = skipWhiteSpace(row);
  Range startPattern = findGuardPattern(row, endStart, START_PATTERN);

  // Determine the width of a narrow line in pixels. We can do this by
  // getting the width of the start pattern and dividing by 4 because its
  // made up of 4 narrow lines.
  narrowLineWidth = (startPattern[1] - startPattern[0]) >> 2;

  validateQuietZone(row, startPattern[0]);
  return startPattern;
}

/**
 * Identify where the end of the middle / payload section ends.
 *
 * @param row row of black/white values to search
 * @return Array, containing index of start of 'end block' and end of 'end
 *         block'
 * @throws ReaderException
 */

ITFReader::Range ITFReader::decodeEnd(Ref<BitArray> row) {
  // For convenience, reverse the row and then
  // search from 'the start' for the end block
  BitArray::Reverse r (row);

  int endStart = skipWhiteSpace(row);
  Range endPattern = findGuardPattern(row, endStart, END_PATTERN_REVERSED);

  // The start & end patterns must be pre/post fixed by a quiet zone. This
  // zone must be at least 10 times the width of a narrow line.
  // ref: http://www.barcode-1.net/i25code.html
  validateQuietZone(row, endPattern[0]);

  // Now recalculate the indices of where the 'endblock' starts & stops to
  // accommodate
  // the reversed nature of the search
  int temp = endPattern[0];
  endPattern[0] = row->getSize() - endPattern[1];
  endPattern[1] = row->getSize() - temp;
  
  return endPattern;
}

/**
 * The start & end patterns must be pre/post fixed by a quiet zone. This
 * zone must be at least 10 times the width of a narrow line.  Scan back until
 * we either get to the start of the barcode or match the necessary number of
 * quiet zone pixels.
 *
 * Note: Its assumed the row is reversed when using this method to find
 * quiet zone after the end pattern.
 *
 * ref: http://www.barcode-1.net/i25code.html
 *
 * @param row bit array representing the scanned barcode.
 * @param startPattern index into row of the start or end pattern.
 * @throws ReaderException if the quiet zone cannot be found, a ReaderException is thrown.
 */
void ITFReader::validateQuietZone(Ref<BitArray> row, int startPattern) {
  int quietCount = this->narrowLineWidth * 10;  // expect to find this many pixels of quiet zone

  for (int i = startPattern - 1; quietCount > 0 && i >= 0; i--) {
    if (row->get(i)) {
      break;
    }
    quietCount--;
  }
  if (quietCount != 0) {
    // Unable to find the necessary number of quiet zone pixels.
    throw NotFoundException();
  }
}

/**
 * Skip all whitespace until we get to the first black line.
 *
 * @param row row of black/white values to search
 * @return index of the first black line.
 * @throws ReaderException Throws exception if no black lines are found in the row
 */
int ITFReader::skipWhiteSpace(Ref<BitArray> row) {
  int width = row->getSize();
  int endStart = row->getNextSet(0);
  if (endStart == width) {
    throw NotFoundException();
  }
  return endStart;
}

/**
 * @param row       row of black/white values to search
 * @param rowOffset position to start search
 * @param pattern   pattern of counts of number of black and white pixels that are
 *                  being searched for as a pattern
 * @return start/end horizontal offset of guard pattern, as an array of two
 *         ints
 * @throws ReaderException if pattern is not found
 */
ITFReader::Range ITFReader::findGuardPattern(Ref<BitArray> row,
                                             int rowOffset,
                                             vector<int> const& pattern) {
  // TODO: This is very similar to implementation in UPCEANReader. Consider if they can be
  // merged to a single method.
  int patternLength = pattern.size();
  vector<int> counters(patternLength);
  int width = row->getSize();
  bool isWhite = false;

  int counterPosition = 0;
  int patternStart = rowOffset;
  for (int x = rowOffset; x < width; x++) {
    if (row->get(x) ^ isWhite) {
      counters[counterPosition]++;
    } else {
      if (counterPosition == patternLength - 1) {
        if (patternMatchVariance(counters, &pattern[0], MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
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

/**
 * Attempts to decode a sequence of ITF black/white lines into single
 * digit.
 *
 * @param counters the counts of runs of observed black/white/black/... values
 * @return The decoded digit
 * @throws ReaderException if digit cannot be decoded
 */
int ITFReader::decodeDigit(vector<int>& counters){

  int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
  int bestMatch = -1;
  int max = sizeof(PATTERNS)/sizeof(PATTERNS[0]);
  for (int i = 0; i < max; i++) {
    int const* pattern = PATTERNS[i];
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

ITFReader::~ITFReader(){}
