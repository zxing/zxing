#pragma once
/*
 *  Code39Reader.h
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

#include <zxing/oned/ZXingOneDReader.h>  // for OneDReader
#include <string>                        // for string

#include "zxing/common/Counted.h"        // for Ref

#include <array>

namespace pping {
class BitArray;
class Result;
class String;
}  // namespace pping

namespace pping {
namespace oned {

/**
 * <p>Decodes Code 39 barcodes. This does not support "Full ASCII Code 39" yet.</p>
 * Ported form Java (author Sean Owen)
 * @author Lukasz Warchol
 */
class Code39Reader : public OneDReader {

private:
    std::string alphabet_string;

    bool usingCheckDigit;
    /*
     * Always treat code39 barcode as not in extended mode. Recognizer are
     * now responsible for interpreting data as extended in case code39 was read.
     */
//    bool extendedMode;

    static Fallible<std::array<int, 2>> findAsteriskPattern(Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;
    static int toNarrowWidePattern(int counters[], int countersLen);
    static Fallible<char> patternToChar(int pattern);
    static Ref<String> decodeExtended(const std::string encoded);

    void append(char* s, char c);
public:
    Code39Reader();
    Code39Reader(bool usingCheckDigit_);

    FallibleRef<Result> decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;
};
}
}

