#pragma once

/*
 *  DecodedBitStreamParser.h
 *  zxing
 *
 *  Created by Hartmut Neubauer, 2012-05-23 from Java sources.
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

#include <zxing/common/Array.h>    // for ArrayRef
#include <zxing/common/Counted.h>  // for Ref
#include "zxing/common/Error.hpp"

namespace bigInteger {
class BigInteger;
}  // namespace bigInteger
namespace pping {
class DecoderResult;
class String;
}  // namespace pping


namespace pping {

namespace pdf417 {

using namespace bigInteger;

class DecodedBitStreamParser {
protected:
  enum Mode {
    ALPHA,
    LOWER,
    MIXED,
    PUNCT,
    ALPHA_SHIFT,
    PUNCT_SHIFT
  };

private:
  
  static const int TEXT_COMPACTION_MODE_LATCH;
  static const int BYTE_COMPACTION_MODE_LATCH;
  static const int NUMERIC_COMPACTION_MODE_LATCH;
  static const int BYTE_COMPACTION_MODE_LATCH_6;
  static const int BEGIN_MACRO_PDF417_CONTROL_BLOCK;
  static const int BEGIN_MACRO_PDF417_OPTIONAL_FIELD;
  static const int MACRO_PDF417_TERMINATOR;
  static const int MODE_SHIFT_TO_BYTE_COMPACTION_MODE;
  static const int MAX_NUMERIC_CODEWORDS;

  static const int PL;
  static const int LL;
  static const int AS;
  static const int ML;
  static const int AL;
  static const int PS;
  static const int PAL;
  static const int EXP900_SIZE;

  static const char PUNCT_CHARS[];
  static const char MIXED_CHARS[];
 
  static ArrayRef<BigInteger> AExp900_;
  static void InitExp900();
  
  static int textCompaction(ArrayRef<int> codewords, int codeIndex,
                            Ref<String> result, ArrayRef<unsigned char> rawBytes);
  static void decodeTextCompaction(ArrayRef<int> textCompactionData,
                                           ArrayRef<int> byteCompactionData,
                                           int length,
                                           Ref<String> result,
                                           ArrayRef<unsigned char> rawBytes);
  static int byteCompaction(int mode, ArrayRef<int> codewords, int codeIndex,
                            Ref<String> result, ArrayRef<unsigned char> rawBytes);
  static Fallible<int> numericCompaction(ArrayRef<int> codewords, int codeIndex,
                               Ref<String> result, ArrayRef<unsigned char> rawBytes) MB_NOEXCEPT_EXCEPT_BADALLOC;
  static FallibleRef<pping::String> decodeBase900toBase10(ArrayRef<int> codewords, int count) MB_NOEXCEPT_EXCEPT_BADALLOC;

  //added
  static bool testCompactionModeChange(int code);

public:
  DecodedBitStreamParser();
  static FallibleRef<DecoderResult> decode(ArrayRef<int> codewords) MB_NOEXCEPT_EXCEPT_BADALLOC;
};

} /* namespace pdf417 */
} /* namespace zxing */

