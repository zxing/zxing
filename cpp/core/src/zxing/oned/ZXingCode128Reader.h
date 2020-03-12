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

#include "zxing/common/Counted.h"        // for Ref
#include <Utils/Macros.h>

namespace pping {
class BitArray;
class Result;
}  // namespace pping

namespace pping {
    namespace oned {
        class Code128Reader : public OneDReader {
            
        private:
      enum {MAX_AVG_VARIANCE = (unsigned int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 250/1000)};
      enum {MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 700/1000)};
            static const int CODE_SHIFT = 98;
            
            static const int CODE_CODE_C = 99;
            static const int CODE_CODE_B = 100;
            static const int CODE_CODE_A = 101;
            
            static const int CODE_FNC_1 = 102;
            static const int CODE_FNC_2 = 97;
            static const int CODE_FNC_3 = 96;
            static const int CODE_FNC_4_A = 101;
            static const int CODE_FNC_4_B = 100;
            
            static const int CODE_START_A = 103;
            static const int CODE_START_B = 104;
            static const int CODE_START_C = 105;
            static const int CODE_STOP = 106;
            
            static Fallible<int*> findStartPattern(Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;
            static Fallible<int> decodeCode(Ref<BitArray> row, int counters[], int countersCount, int rowOffset) noexcept;
            
            void append(char* s, char c);
        public:
            FallibleRef<Result> decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;
            Code128Reader();
            ~Code128Reader();
        };
    }
}

