/*
 *  Code128Reader.cpp
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

#include "Code128Reader.h"
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/common/Array.h>
#include <zxing/ReaderException.h>
#include <math.h>
#include <string.h>
#include <sstream>

namespace zxing {
	namespace oned {

		const int CODE_PATTERNS_LENGTH = 107;
		const int countersLength = 6;
		static const int CODE_PATTERNS[CODE_PATTERNS_LENGTH][countersLength] = {
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


		Code128Reader::Code128Reader(){
		}

		int* Code128Reader::findStartPattern(Ref<BitArray> row){
			int width = row->getSize();
			int rowOffset = 0;
			while (rowOffset < width) {
				if (row->get(rowOffset)) {
					break;
				}
				rowOffset++;
			}

			int counterPosition = 0;
			int counters[countersLength] = {0,0,0,0,0,0};
			int patternStart = rowOffset;
			bool isWhite = false;
			int patternLength =  sizeof(counters) / sizeof(int);

			for (int i = rowOffset; i < width; i++) {
				bool pixel = row->get(i);
				if (pixel ^ isWhite) {
					counters[counterPosition]++;
				} else {
					if (counterPosition == patternLength - 1) {
						unsigned int bestVariance = MAX_AVG_VARIANCE;
						int bestMatch = -1;
						for (int startCode = CODE_START_A; startCode <= CODE_START_C; startCode++) {
							unsigned int variance = patternMatchVariance(counters, sizeof(counters) / sizeof(int),
							    CODE_PATTERNS[startCode], MAX_INDIVIDUAL_VARIANCE);
							if (variance < bestVariance) {
								bestVariance = variance;
								bestMatch = startCode;
							}
						}
						if (bestMatch >= 0) {
							// Look for whitespace before start pattern, >= 50% of width of start pattern
							if (row->isRange(fmaxl(0, patternStart - (i - patternStart) / 2), patternStart,
							    false)) {
								int* resultValue = new int[3];
								resultValue[0] = patternStart;
								resultValue[1] = i;
								resultValue[2] = bestMatch;
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

		int Code128Reader::decodeCode(Ref<BitArray> row, int counters[], int countersCount,
		    int rowOffset) {
		  if (!recordPattern(row, rowOffset, counters, countersCount)) {
		    throw ReaderException("");
		  }
			unsigned int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
			int bestMatch = -1;
			for (int d = 0; d < CODE_PATTERNS_LENGTH; d++) {
				int pattern[countersLength];

				for(int ind = 0; ind< countersLength; ind++){
					pattern[ind] = CODE_PATTERNS[d][ind];
				}
//				memcpy(pattern, CODE_PATTERNS[d], countersLength);
				unsigned int variance = patternMatchVariance(counters, countersCount, pattern,
				    MAX_INDIVIDUAL_VARIANCE);
				if (variance < bestVariance) {
					bestVariance = variance;
					bestMatch = d;
				}
			}
			// TODO We're overlooking the fact that the STOP pattern has 7 values, not 6.
			if (bestMatch >= 0) {
				return bestMatch;
			} else {
				throw ReaderException("");
			}
		}

		Ref<Result> Code128Reader::decodeRow(int rowNumber, Ref<BitArray> row) {
		  int* startPatternInfo = NULL;
		  try {
        startPatternInfo = findStartPattern(row);
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
            throw ReaderException("");
        }

        bool done = false;
        bool isNextShifted = false;

        std::string tmpResultString;
        std::stringstream tmpResultSStr; // used if its Code 128C

        int lastStart = startPatternInfo[0];
        int nextStart = startPatternInfo[1];
        int counters[countersLength] = {0,0,0,0,0,0};

        int lastCode = 0;
        int code = 0;
        int checksumTotal = startCode;
        int multiplier = 0;
        bool lastCharacterWasPrintable = true;

        while (!done) {
          bool unshift = isNextShifted;
          isNextShifted = false;

          // Save off last code
          lastCode = code;

          // Decode another code from image
          try {
            code = decodeCode(row, counters, sizeof(counters)/sizeof(int), nextStart);
          } catch (ReaderException re) {
            throw re;
          }

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
          int _countersLength = sizeof(counters) / sizeof(int);
          for (int i = 0; i < _countersLength; i++) {
            nextStart += counters[i];
          }

          // Take care of illegal start codes
          switch (code) {
            case CODE_START_A:
            case CODE_START_B:
            case CODE_START_C:
              throw ReaderException("");
          }

          switch (codeSet) {

            case CODE_CODE_A:
              if (code < 64) {
                tmpResultString.append(1, (char) (' ' + code));
              } else if (code < 96) {
                tmpResultString.append(1, (char) (code - 64));
              } else {
                // Don't let CODE_STOP, which always appears, affect whether whether we think the
                // last code was printable or not.
                if (code != CODE_STOP) {
                  lastCharacterWasPrintable = false;
                }
                switch (code) {
                  case CODE_FNC_1:
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
                tmpResultString.append(1, (char) (' ' + code));
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
                    codeSet = CODE_CODE_C;
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
            // the code read in this case is the number encoded directly
              if (code < 100) {
                if (code < 10)
                tmpResultSStr << '0';
              tmpResultSStr << code;
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
            switch (codeSet) {
              case CODE_CODE_A:
                codeSet = CODE_CODE_C;
                break;
              case CODE_CODE_B:
                codeSet = CODE_CODE_A;
                break;
              case CODE_CODE_C:
                codeSet = CODE_CODE_B;
                break;
            }
          }

        }

        // Check for ample whitespace following pattern, but, to do this we first need to remember that
        // we fudged decoding CODE_STOP since it actually has 7 bars, not 6. There is a black bar left
        // to read off. Would be slightly better to properly read. Here we just skip it:
        int width = row->getSize();
        while (nextStart < width && row->get(nextStart)) {
          nextStart++;
        }
        if (!row->isRange(nextStart, fminl(width, nextStart + (nextStart - lastStart) / 2), false)) {
          throw ReaderException("");
        }

        // Pull out from sum the value of the penultimate check code
        checksumTotal -= multiplier * lastCode;
        // lastCode is the checksum then:
        if (checksumTotal % 103 != lastCode) {
          throw ReaderException("");
        }

        if (codeSet == CODE_CODE_C)
          tmpResultString.append(tmpResultSStr.str());

        // Need to pull out the check digits from string
        int resultLength = tmpResultString.length();
        // Only bother if the result had at least one character, and if the checksum digit happened to
        // be a printable character. If it was just interpreted as a control code, nothing to remove.
        if (resultLength > 0 && lastCharacterWasPrintable) {
          if (codeSet == CODE_CODE_C) {
            tmpResultString.erase(resultLength - 2, resultLength);
          } else {
            tmpResultString.erase(resultLength - 1, resultLength);
          }
        }

        Ref<String> resultString(new String(tmpResultString));
        if (tmpResultString.length() == 0) {
          // Almost surely a false positive
          throw ReaderException("");
        }

        float left = (float) (startPatternInfo[1] + startPatternInfo[0]) / 2.0f;
        float right = (float) (nextStart + lastStart) / 2.0f;

        std::vector< Ref<ResultPoint> > resultPoints(2);
        Ref<OneDResultPoint> resultPoint1(new OneDResultPoint(left, (float) rowNumber));
        Ref<OneDResultPoint> resultPoint2(new OneDResultPoint(right, (float) rowNumber));
        resultPoints[0] = resultPoint1;
        resultPoints[1] = resultPoint2;

        delete [] startPatternInfo;
        ArrayRef<unsigned char> resultBytes(1);
        return Ref<Result>(new Result(resultString, resultBytes, resultPoints,
            BarcodeFormat_CODE_128));
			} catch (ReaderException const& re) {
			  delete [] startPatternInfo;
			  return Ref<Result>();
			}
		}

		void Code128Reader::append(char* s, char c){
			int len = strlen(s);
			s[len] = c;
			s[len + 1] = '\0';
		}

		Code128Reader::~Code128Reader(){
		}
	}
}
