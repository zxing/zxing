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

#include "zxing/BarcodeFormat.h"         // for BarcodeFormat
#include "zxing/common/Counted.h"        // for Ref

namespace pping {
class BitArray;
class Result;
}  // namespace pping

typedef enum UPC_EAN_PATTERNS {
    UPC_EAN_PATTERNS_L_PATTERNS = 0,
    UPC_EAN_PATTERNS_L_AND_G_PATTERNS
} UPC_EAN_PATTERNS;

namespace pping {
    namespace oned {
        class UPCEANReader : public OneDReader {

        private:
            enum {MAX_AVG_VARIANCE = (unsigned int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 420/1000)};
            enum {MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 700/1000)};

            static Fallible<bool> findStartGuardPattern(Ref<BitArray> row, int* rangeStart, int* rangeEnd);

            virtual bool decodeEnd(Ref<BitArray> row, int endStart, int* endGuardBegin, int* endGuardEnd);

            static bool checkStandardUPCEANChecksum(std::string s);

        protected:
            static bool findGuardPattern(Ref<BitArray> row, int rowOffset, bool whiteFirst,
                                          const int pattern[], int patternLen, int* start, int* end);

            virtual int getMIDDLE_PATTERN_LEN();
            virtual const int* getMIDDLE_PATTERN();

        public:
            UPCEANReader();

            // Returns < 0 on failure, >= 0 on success.
            virtual int decodeMiddle(Ref<BitArray> row, int startGuardBegin, int startGuardEnd,
                                      std::string& resultString) = 0;

            FallibleRef<Result> decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;

            // TODO(dswitkin): Should this be virtual so that UPCAReader can override it?
            FallibleRef<Result> decodeRow(int rowNumber, Ref<BitArray> row, int startGuardBegin,
                                  int startGuardEnd);

            // Returns < 0 on failure, >= 0 on success.
            static int decodeDigit(Ref<BitArray> row, int counters[], int countersLen, int rowOffset,
                                    UPC_EAN_PATTERNS patternType);

            virtual bool checkChecksum(std::string s);

            virtual BarcodeFormat getBarcodeFormat() = 0;
            virtual ~UPCEANReader();
        };
    }
}

