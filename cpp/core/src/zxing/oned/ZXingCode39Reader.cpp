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

#include "ZXingCode39Reader.h"

#include <limits.h>                           // for INT_MAX
#include <stddef.h>                           // for NULL
#include <zxing/ReaderException.h>            // for ReaderException
#include <zxing/common/Array.h>               // for ArrayRef
#include <zxing/oned/ZXingOneDResultPoint.h>  // for OneDResultPoint
#include <algorithm>                          // for max
#include <vector>                             // for allocator, vector, __vector_base<>::value_type

#include "zxing/BarcodeFormat.h"              // for BarcodeFormat::CODE_39
#include "zxing/Result.h"                     // for Result
#include "zxing/ResultPoint.h"                // for ResultPoint
#include "zxing/common/BitArray.h"            // for BitArray
#include "zxing/common/Str.h"                 // for String

namespace pping {
namespace oned {

static const char* ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";

/**
 * These represent the encodings of characters, as patterns of wide
 * and narrow bars.
 * The 9 least-significant bits of each int correspond to the pattern of wide
 * and narrow, with 1s representing "wide" and 0s representing narrow.
 */
const int CHARACTER_ENCODINGS_LEN = 44;
static int CHARACTER_ENCODINGS[CHARACTER_ENCODINGS_LEN] = {0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064,  // 0-9
                                                           0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C,  // A-J
                                                           0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016,  // K-T
                                                           0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094,  // U-*
                                                           0x0A8, 0x0A2, 0x08A, 0x02A  // $-%
                                                           };

static int ASTERISK_ENCODING = 0x094;
static const char* ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";

/**
 * Creates a reader that assumes all encoded data is data, and does not treat
 * the final character as a check digit. It will not decoded "extended
 * Code 39" sequences.
 */
Code39Reader::Code39Reader() :
        alphabet_string(ALPHABET_STRING), usingCheckDigit(false) {
}

/**
 * Creates a reader that can be configured to check the last character as a
 * check digit. It will not decoded "extended Code 39" sequences.
 *
 * @param usingCheckDigit_ if true, treat the last data character as a check
 * digit, not data, and verify that the checksum passes.
 */
Code39Reader::Code39Reader(bool usingCheckDigit_) :
        alphabet_string(ALPHABET_STRING), usingCheckDigit(usingCheckDigit_) {
}

FallibleRef<Result> Code39Reader::decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC {
        auto const tryFindStart(findAsteriskPattern(row));
        if(!tryFindStart)
            return tryFindStart.error();

        auto const start = *tryFindStart;

        int nextStart = start[1];
        if( nextStart < 0 )
        {
            return failure<ReaderException>( "nextStart < 0" );
        }
        int end = (int) row->getSize();

        // Read off white space
        auto const getAt(row->get(nextStart));
        if(!getAt)
            return getAt.error();
        while (nextStart < end && getAt && !(*getAt)) {
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
            if (!recordPattern(row, nextStart, counters, countersLen))
                return failure<ReaderException>("Recording pattern failed");

            int pattern = toNarrowWidePattern(counters, countersLen);
            if (pattern < 0)
                return failure<ReaderException>("Conversion to narrow pattern < 0");

            auto const getChar(patternToChar(pattern));
            if(!getChar)
                return getChar.error();

            decodedChar = *getChar;

            tmpResultString.append(1, decodedChar);
            lastStart = nextStart;
            for (int i = 0; i < countersLen; i++) {
                nextStart += counters[i];
            }
            // Read off white space
            auto const getAtNextStart(row->get(nextStart));
            if(!getAtNextStart)
                return getAtNextStart.error();

            while (nextStart < end && !(*getAtNextStart)) {
                nextStart++;
            }
        } while (decodedChar != '*');
        tmpResultString.erase(tmpResultString.length() - 1, 1);  // remove asterisk

        // Look for whitespace after pattern:
        int lastPatternSize = 0;
        for (int i = 0; i < countersLen; i++) {
            lastPatternSize += counters[i];
        }
        int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;
        // If 50% of last pattern size, following last pattern, is not whitespace,
        // fail (but if it's whitespace to the very end of the image, that's OK)
        if (nextStart != end && whiteSpaceAfterEnd < lastPatternSize / 2)
            return failure<ReaderException>("too short end white space");

        if (usingCheckDigit) {
            int max = (int) tmpResultString.length() - 1;
            unsigned int total = 0;
            for (int i = 0; i < max; i++) {
                total += (unsigned int) alphabet_string.find_first_of(tmpResultString[i], 0);
            }
            if (total % 43 != alphabet_string.find_first_of(tmpResultString[max], 0))
                return failure<ReaderException>("Check digit failed?");

            tmpResultString.erase(max, 1);
        }

        if (tmpResultString.length() == 0) {
            // Almost surely a false positive
            return failure<ReaderException>("False positive?");
        }

        float left = (float) (start[1] + start[0]) / 2.0f;
        float right = (float) (lastStart + (lastPatternSize >> 1));

        std::vector<Ref<ResultPoint> > resultPoints(2);
        Ref<OneDResultPoint> resultPoint1(new OneDResultPoint(left, (float) rowNumber));
        Ref<OneDResultPoint> resultPoint2(new OneDResultPoint(right, (float) rowNumber));
        resultPoints[0] = resultPoint1;
        resultPoints[1] = resultPoint2;

        Ref<String> resultString(new String(tmpResultString));
        /*
         * Always treat code39 barcode as not in extended mode. Recognizer are
         * now responsible for interpreting data as extended in case code39 was read.
         */
//        if (extendedMode) {
//            // if fails throws exception
//            resultString = decodeExtended(tmpResultString);
//        }

        std::string tmpString = resultString->getText();
        ArrayRef<unsigned char> resultBytes(tmpString.length());
        for (int j = 0; j < (int) tmpString.size(); ++j) {
            resultBytes[j] = tmpString[j];
        }

        Ref<Result> res(new Result(resultString, resultBytes, resultPoints, BarcodeFormat::CODE_39));
        return res;
}

Fallible<std::array<int, 2>> Code39Reader::findAsteriskPattern(Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC {
    int width = (int) row->getSize();
    int rowOffset = 0;
    while (rowOffset < width) {

        auto const getAtOffset(row->get(rowOffset));
        if(!getAtOffset)
            return getAtOffset.error();

        if(*getAtOffset) {
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
        auto const getAt(row->get(i));
        if(!getAt)
            return getAt.error();

        bool pixel = *getAt;
        if (pixel ^ isWhite) {
            counters[counterPosition]++;
        } else {
            if (counterPosition == patternLength - 1) {
                // Look for whitespace before start pattern, >= 50% of width of
                // start pattern.
                auto const startRange = std::max(0, patternStart - ((i - patternStart) >> 1)), endRange = patternStart;

                auto const checkRange(row->isRange(startRange, endRange, false));
                if(!checkRange)
                    return checkRange.error();

                if (toNarrowWidePattern(counters, countersLen) == ASTERISK_ENCODING
                        && (*checkRange)) {
                    std::array<int, 2> resultValue{ { patternStart, i } };
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
    return failure<ReaderException>("Cannot find asterisk pattern");
}

// For efficiency, returns -1 on failure. Not throwing here saved as many as
// 700 exceptions per image when using some of our blackbox images.
int Code39Reader::toNarrowWidePattern(int counters[], int countersLen) {
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

Fallible<char> Code39Reader::patternToChar(int pattern) {
    for (int i = 0; i < CHARACTER_ENCODINGS_LEN; i++) {
        if (CHARACTER_ENCODINGS[i] == pattern) {
            return ALPHABET[i];
        }
    }
    return failure<ReaderException>("Pattern -> char failed");
}

}  // namespace oned
}  // namespace zxing

