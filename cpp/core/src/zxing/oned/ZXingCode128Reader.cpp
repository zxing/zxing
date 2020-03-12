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

#include "ZXingCode128Reader.h"

#include <string.h>                           // for strlen, NULL
#include <zxing/ReaderException.h>            // for ReaderException
#include <zxing/common/Array.h>               // for ArrayRef
#include <zxing/oned/ZXingOneDResultPoint.h>  // for OneDResultPoint
#include <algorithm>                          // for max, min
#include "Utils/stringstreamlite.hpp"
#include <vector>                             // for allocator, vector, __vector_base<>::value_type

#include "zxing/BarcodeFormat.h"              // for BarcodeFormat::CODE_128
#include "zxing/Result.h"                     // for Result
#include "zxing/ResultPoint.h"                // for ResultPoint
#include "zxing/common/BitArray.h"            // for BitArray
#include "zxing/common/Str.h"                 // for String

#include <memory>

namespace pping {
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
                        {2, 3, 3, 1, 1, 1}};

Code128Reader::Code128Reader() {
}

Fallible<int*> Code128Reader::findStartPattern(Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC {
    int width = (int) row->getSize();
    int rowOffset = 0;
    while (rowOffset < width) {
        auto const getAt(row->get(rowOffset));
        if(!getAt)
            return getAt.error();
        if (*getAt) {
            break;
        }
        rowOffset++;
    }

    int counterPosition = 0;
    int counters[countersLength] = {0, 0, 0, 0, 0, 0};
    int patternStart = rowOffset;
    bool isWhite = false;
    int patternLength = (int) (sizeof(counters) / sizeof(int));

    for (int i = rowOffset; i < width; i++)
    {
        auto const getAtPixel(row->get(i));
        if(!getAtPixel)
            return getAtPixel.error();
        bool pixel = *getAtPixel;

        if (pixel ^ isWhite) {
            counters[counterPosition]++;
        } else {
            if (counterPosition == patternLength - 1) {
                unsigned int bestVariance = MAX_AVG_VARIANCE;
                int bestMatch = -1;
                for (int startCode = CODE_START_A; startCode <= CODE_START_C; startCode++) {
                    unsigned int variance = patternMatchVariance(counters, (int) (sizeof(counters) / sizeof(int)), CODE_PATTERNS[startCode],
                                                                 MAX_INDIVIDUAL_VARIANCE);
                    if (variance < bestVariance) {
                        bestVariance = variance;
                        bestMatch = startCode;
                    }
                }
                // Look for whitespace before start pattern, >= 50% of width of start pattern
                auto const startRange = std::max(0, patternStart - (i - patternStart) / 2), endRange = patternStart;

                auto const checkRange(row->isRange(startRange, endRange, false));
                if(!checkRange)
                    return checkRange.error();

                if (bestMatch >= 0 && (*checkRange)) {
                    int* resultValue = new int[3];
                    resultValue[0] = patternStart;
                    resultValue[1] = i;
                    resultValue[2] = bestMatch;
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
    return failure<ReaderException>("Can't find start pattern");
}

Fallible<int> Code128Reader::decodeCode(Ref<BitArray> row, int counters[], int countersCount, int rowOffset) noexcept {
    if (!recordPattern(row, rowOffset, counters, countersCount)) {
        return failure<ReaderException>("Cannot record pattern");
    }
    unsigned int bestVariance = MAX_AVG_VARIANCE;  // worst variance we'll accept
    int bestMatch = -1;
    for (int d = 0; d < CODE_PATTERNS_LENGTH; d++) {
        int pattern[countersLength];

        for (int ind = 0; ind < countersLength; ind++) {
            pattern[ind] = CODE_PATTERNS[d][ind];
        }
//				memcpy(pattern, CODE_PATTERNS[d], countersLength);
        unsigned int variance = patternMatchVariance(counters, countersCount, pattern, MAX_INDIVIDUAL_VARIANCE);
        if (variance < bestVariance) {
            bestVariance = variance;
            bestMatch = d;
        }
    }
    // TODO We're overlooking the fact that the STOP pattern has 7 values, not 6.
    if (bestMatch >= 0) {
        return bestMatch;
    } else {
        return failure<ReaderException>("Can't decode");;
    }
}

FallibleRef<Result> Code128Reader::decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC {

        auto const getStartInfo(findStartPattern(row));
        if(!getStartInfo)
            return getStartInfo.error();

        auto const startPatternInfo = std::unique_ptr<int[]>(*getStartInfo);

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
                return failure<ReaderException>("Invalid start code");
        }

        bool done = false;
        bool isNextShifted = false;

        std::string tmpResultString;
        mb::stringstreamlite tmpResultSStr;  // used if its Code 128C

        int lastStart = startPatternInfo[0];
        int nextStart = startPatternInfo[1];
        int counters[countersLength] = {0, 0, 0, 0, 0, 0};

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
            auto const tryDecode(decodeCode(row, counters, (int) (sizeof(counters) / sizeof(int)), nextStart));
            if(!tryDecode)
                return tryDecode.error();

            code = *tryDecode;

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
            int _countersLength = (int) (sizeof(counters) / sizeof(int));
            for (int i = 0; i < _countersLength; i++) {
                nextStart += counters[i];
            }

            // Take care of illegal start codes
            switch (code) {
                case CODE_START_A:
                case CODE_START_B:
                case CODE_START_C:
                    return failure<ReaderException>("Invalid start code");
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
                    tmpResultSStr.str(std::string());
                    // the code read in this case is the number encoded directly
                    if (code < 100) {
                        if (code < 10) {
                            tmpResultSStr << '0';
                        }
                        tmpResultSStr << code;
                        tmpResultString.append(tmpResultSStr.str());
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
        int width = (int) row->getSize();
        auto const getAt(row->get(nextStart));
        if(!getAt)
            return getAt.error();
        while (nextStart < width && *getAt) {
            nextStart++;
        }
        auto const checkRange(row->isRange(nextStart, std::min(width, nextStart + (nextStart - lastStart) / 2), false));
        if(!checkRange)
            return checkRange.error();

        if (!(*checkRange)) {
            return failure<ReaderException>("Invalid range");
        }

        // Pull out from sum the value of the penultimate check code
        checksumTotal -= multiplier * lastCode;
        // lastCode is the checksum then:
        if (checksumTotal % 103 != lastCode) {
            return failure<ReaderException>("Invalid checksum");
        }

        // Need to pull out the check digits from string
        int resultLength = (int) tmpResultString.length();
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
            return failure<ReaderException>("False positive result?");
        }

        float left = (float) (startPatternInfo[1] + startPatternInfo[0]) / 2.0f;
        float right = (float) (nextStart + lastStart) / 2.0f;

        std::vector<Ref<ResultPoint> > resultPoints(2);
        Ref<OneDResultPoint> resultPoint1(new OneDResultPoint(left, (float) rowNumber));
        Ref<OneDResultPoint> resultPoint2(new OneDResultPoint(right, (float) rowNumber));
        resultPoints[0] = resultPoint1;
        resultPoints[1] = resultPoint2;

        std::string tmpString = resultString->getText();
        ArrayRef<unsigned char> resultBytes(tmpString.length());
        for (int j = 0; j < (int) tmpString.size(); ++j) {
            resultBytes[j] = tmpString[j];
        }
//        LOGE("Row num = %d", rowNumber);
//        return Ref<Result>();
        return Ref<Result>(new Result(resultString, resultBytes, resultPoints, BarcodeFormat::CODE_128));
}

void Code128Reader::append(char* s, char c) {
    int len = (int) strlen(s);
    s[len] = c;
    s[len + 1] = '\0';
}

Code128Reader::~Code128Reader() {
}
}
}
