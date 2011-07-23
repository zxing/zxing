/*
 *  ITFReader.cpp
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

#include "ITFReader.h"
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/common/Array.h>
#include <zxing/ReaderException.h>
#include <math.h>

namespace zxing {
  namespace oned {

    static const int W = 3; // Pixel width of a wide line
    static const int N = 1; // Pixed width of a narrow line

    const int DEFAULT_ALLOWED_LENGTHS_LEN = 10;
    const int DEFAULT_ALLOWED_LENGTHS[DEFAULT_ALLOWED_LENGTHS_LEN] = { 44, 24, 20, 18, 16, 14, 12, 10, 8, 6 };

    /**
     * Start/end guard pattern.
     *
     * Note: The end pattern is reversed because the row is reversed before
     * searching for the END_PATTERN
     */
    static const int START_PATTERN_LEN = 4;
    static const int START_PATTERN[START_PATTERN_LEN] = {N, N, N, N};

    static const int END_PATTERN_REVERSED_LEN = 3;
    static const int END_PATTERN_REVERSED[END_PATTERN_REVERSED_LEN] = {N, N, W};

    /**
     * Patterns of Wide / Narrow lines to indicate each digit
     */
    static const int PATTERNS_LEN = 10;
    static const int PATTERNS[PATTERNS_LEN][5] = {
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


    ITFReader::ITFReader() : narrowLineWidth(-1) {
    }


    Ref<Result> ITFReader::decodeRow(int rowNumber, Ref<BitArray> row) {
      int* startRange = 0;
      int* endRange = 0;
      try {
        // Find out where the Middle section (payload) starts & ends
        startRange = decodeStart(row);
        endRange = decodeEnd(row);

        std::string tmpResult;
        decodeMiddle(row, startRange[1], endRange[0], tmpResult);

        // To avoid false positives with 2D barcodes (and other patterns), make
        // an assumption that the decoded string must be a known length
        int length = tmpResult.length();
        bool lengthOK = false;
        for (int i = 0; i < DEFAULT_ALLOWED_LENGTHS_LEN; i++) {
          if (length == DEFAULT_ALLOWED_LENGTHS[i]) {
            lengthOK = true;
            break;
          }
        }
        if (!lengthOK) {
          throw ReaderException("not enough characters count");
        }

        Ref<String> resultString(new String(tmpResult));

        std::vector< Ref<ResultPoint> > resultPoints(2);
        Ref<OneDResultPoint> resultPoint1(new OneDResultPoint(startRange[1], (float) rowNumber));
        Ref<OneDResultPoint> resultPoint2(new OneDResultPoint(endRange[0], (float) rowNumber));
        resultPoints[0] = resultPoint1;
        resultPoints[1] = resultPoint2;

        delete [] startRange;
        delete [] endRange;
        ArrayRef<unsigned char> resultBytes(1);
        return Ref<Result>(new Result(resultString, resultBytes, resultPoints, BarcodeFormat_ITF));
      } catch (ReaderException re) {
        delete [] startRange;
        delete [] endRange;
        return Ref<Result>();
      }
    }

    /**
     * @param row          row of black/white values to search
     * @param payloadStart offset of start pattern
     * @param resultString {@link StringBuffer} to append decoded chars to
     * @throws ReaderException if decoding could not complete successfully
     */
    void ITFReader::decodeMiddle(Ref<BitArray> row, int payloadStart, int payloadEnd,
        std::string& resultString) {
      // Digits are interleaved in pairs - 5 black lines for one digit, and the
      // 5
      // interleaved white lines for the second digit.
      // Therefore, need to scan 10 lines and then
      // split these into two arrays
      int counterDigitPairLen = 10;
      int counterDigitPair[counterDigitPairLen];
      for (int i=0; i<counterDigitPairLen; i++) {
        counterDigitPair[i] = 0;
      }

      int counterBlack[5];
      int counterWhite[5];
      for (int i=0; i<5; i++) {
        counterBlack[i] = 0;
        counterWhite[i] = 0;
      }

      while (payloadStart < payloadEnd) {
        // Get 10 runs of black/white.
        if (!recordPattern(row, payloadStart, counterDigitPair, counterDigitPairLen)) {
          throw ReaderException("");
        }
        // Split them into each array
        for (int k = 0; k < 5; k++) {
          int twoK = k << 1;
          counterBlack[k] = counterDigitPair[twoK];
          counterWhite[k] = counterDigitPair[twoK + 1];
        }

        int bestMatch = decodeDigit(counterBlack, 5);
        resultString.append(1, (char) ('0' + bestMatch));
        bestMatch = decodeDigit(counterWhite, 5);
        resultString.append(1, (char) ('0' + bestMatch));

        for (int i = 0; i < counterDigitPairLen; i++) {
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
    int* ITFReader::decodeStart(Ref<BitArray> row) {
      int endStart = skipWhiteSpace(row);
      int* startPattern = 0;
      try {
          startPattern = findGuardPattern(row, endStart, START_PATTERN, START_PATTERN_LEN);

          // Determine the width of a narrow line in pixels. We can do this by
          // getting the width of the start pattern and dividing by 4 because its
          // made up of 4 narrow lines.
          narrowLineWidth = (startPattern[1] - startPattern[0]) >> 2;
          validateQuietZone(row, startPattern[0]);
          return startPattern;
      } catch (ReaderException re) {
          delete [] startPattern;
        throw re;
      }
    }

    /**
     * Identify where the end of the middle / payload section ends.
     *
     * @param row row of black/white values to search
     * @return Array, containing index of start of 'end block' and end of 'end
     *         block'
     * @throws ReaderException
     */

    int* ITFReader::decodeEnd(Ref<BitArray> row) {
      // For convenience, reverse the row and then
      // search from 'the start' for the end block
      row->reverse();
                        int* endPattern = 0;
      try {
        int endStart = skipWhiteSpace(row);
        endPattern = findGuardPattern(row, endStart, END_PATTERN_REVERSED, END_PATTERN_REVERSED_LEN);

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

        row->reverse();
        return endPattern;
      } catch (ReaderException re) {
                                delete [] endPattern;
        row->reverse();
        throw re;
      }
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
//#pragma mark needs some corrections
//      int quietCount = narrowLineWidth * 10;  // expect to find this many pixels of quiet zone
//
//      for (int i = startPattern - 1; quietCount > 0 && i >= 0; i--) {
//        if (row->get(i)) {
//          break;
//        }
//        quietCount--;
//      }
//      if (quietCount != 0) {
//        // Unable to find the necessary number of quiet zone pixels.
//        throw ReaderException("Unable to find the necessary number of quiet zone pixels");
//      }
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
      int endStart = 0;
      while (endStart < width) {
        if (row->get(endStart)) {
          break;
        }
        endStart++;
      }
      if (endStart == width) {
        throw ReaderException("");
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
    int* ITFReader::findGuardPattern(Ref<BitArray> row, int rowOffset, const int pattern[],
        int patternLen) {
      // TODO: This is very similar to implementation in UPCEANReader. Consider if they can be
      // merged to a single method.
      int patternLength = patternLen;
      int counters[patternLength];
      for (int i=0; i<patternLength; i++) {
        counters[i] = 0;
      }
      int width = row->getSize();
      bool isWhite = false;

      int counterPosition = 0;
      int patternStart = rowOffset;
      for (int x = rowOffset; x < width; x++) {
        bool pixel = row->get(x);
        if (pixel ^ isWhite) {
          counters[counterPosition]++;
        } else {
          if (counterPosition == patternLength - 1) {
            if (patternMatchVariance(counters, patternLength, pattern,
                MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
              int* resultValue = new int[2];
              resultValue[0] = patternStart;
              resultValue[1] = x;
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
      throw ReaderException("");
    }

    /**
     * Attempts to decode a sequence of ITF black/white lines into single
     * digit.
     *
     * @param counters the counts of runs of observed black/white/black/... values
     * @return The decoded digit
     * @throws ReaderException if digit cannot be decoded
     */
    int ITFReader::decodeDigit(int counters[], int countersLen){
      unsigned int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
      int bestMatch = -1;
      int max = PATTERNS_LEN;
      for (int i = 0; i < max; i++) {
        int pattern[countersLen];
        for(int ind = 0; ind<countersLen; ind++){
          pattern[ind] = PATTERNS[i][ind];
        }
        unsigned int variance = patternMatchVariance(counters, countersLen, pattern,
            MAX_INDIVIDUAL_VARIANCE);
        if (variance < bestVariance) {
          bestVariance = variance;
          bestMatch = i;
        }
      }
      if (bestMatch >= 0) {
        return bestMatch;
      } else {
        throw ReaderException("digit didint found");
      }
    }

    ITFReader::~ITFReader(){
    }
  }
}
