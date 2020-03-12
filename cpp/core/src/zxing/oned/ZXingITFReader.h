// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#pragma once

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

#include <zxing/oned/ZXingOneDReader.h>  // for OneDReader::PATTERN_MATCH_RESULT_SCALE_FACTOR, OneDReader
#include <string>

#include "zxing/common/Counted.h"        // for Ref

#include <array>

namespace pping {
class BitArray;
class Result;
}  // namespace pping

namespace pping {
    namespace oned {
        class ITFReader : public OneDReader {
            
        private:
      enum {MAX_AVG_VARIANCE = (unsigned int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 420/1000)};
            enum {MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 800/1000)};
            // Stores the actual narrow line width of the image being decoded.
            int narrowLineWidth;
            
            Fallible<std::array<int, 2>> decodeStart(Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;
            Fallible<std::array<int, 2>> decodeEnd(Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;

            static Fallible<void> decodeMiddle(Ref<BitArray> row, int payloadStart, int payloadEnd, std::string& resultString);
            void validateQuietZone(Ref<BitArray> row, int startPattern);
            static Fallible<int> skipWhiteSpace(Ref<BitArray> row) noexcept;
            
            static Fallible<std::array<int, 2>> findGuardPattern(Ref<BitArray> row, int rowOffset, const int pattern[], int patternLen) MB_NOEXCEPT_EXCEPT_BADALLOC;
            static Fallible<int> decodeDigit(int counters[], int countersLen) MB_NOEXCEPT_EXCEPT_BADALLOC;
            
            void append(char* s, char c);
        public:
            FallibleRef<Result> decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;

            ITFReader();
            ~ITFReader();
        };
    }
}

