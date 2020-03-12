#pragma once

/*
 *  DecodedBitStreamParser.h
 *  zxing
 *
 *  Created by Luiz Silva on 09/02/2010.
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

#include <stddef.h>                // for size_t
#include <zxing/common/Array.h>    // for ArrayRef
#include <zxing/common/Counted.h>  // for Ref
#include "zxing/common/Error.hpp"

#include <Utils/stringstreamlite.hpp>

#include <string>
#include <vector>

namespace pping {
class BitSource;
class DecoderResult;
}  // namespace pping

namespace pping {
namespace datamatrix {

class DecodedBitStreamParser {
private:
  static const int PAD_ENCODE;  // Not really an encoding
  static const int ASCII_ENCODE;
  static const int C40_ENCODE;
  static const int TEXT_ENCODE;
  static const int ANSIX12_ENCODE;
  static const int EDIFACT_ENCODE;
  static const int BASE256_ENCODE;
    
  /**
   * See ISO 16022:2006, Annex C Table C.1
   * The C40 Basic Character Set (*'s used for placeholders for the shift values)
   */
  static const char C40_BASIC_SET_CHARS[];
  
  static const char C40_SHIFT2_SET_CHARS[];  
  /**
   * See ISO 16022:2006, Annex C Table C.2
   * The Text Basic Character Set (*'s used for placeholders for the shift values)
   */
  static const char TEXT_BASIC_SET_CHARS[];
  
  static const char TEXT_SHIFT3_SET_CHARS[];  
  /**
   * See ISO 16022:2006, 5.2.3 and Annex C, Table C.2
   */
  Fallible<int> decodeAsciiSegment(Ref<BitSource> bits, mb::stringstreamlite &result, mb::stringstreamlite &resultTrailer) MB_NOEXCEPT_EXCEPT_BADALLOC;
  /**
   * See ISO 16022:2006, 5.2.5 and Annex C, Table C.1
   */
  Fallible<void> decodeC40Segment(Ref<BitSource> bits, mb::stringstreamlite &result) MB_NOEXCEPT_EXCEPT_BADALLOC;
  /**
   * See ISO 16022:2006, 5.2.6 and Annex C, Table C.2
   */
  Fallible<void> decodeTextSegment(Ref<BitSource> bits, mb::stringstreamlite &result) MB_NOEXCEPT_EXCEPT_BADALLOC;
  /**
   * See ISO 16022:2006, 5.2.7
   */
  Fallible<void> decodeAnsiX12Segment(Ref<BitSource> bits, mb::stringstreamlite &result) MB_NOEXCEPT_EXCEPT_BADALLOC;
  /**
   * See ISO 16022:2006, 5.2.8 and Annex C Table C.3
   */
  Fallible<void> decodeEdifactSegment(Ref<BitSource> bits, mb::stringstreamlite &result) MB_NOEXCEPT_EXCEPT_BADALLOC;
  /**
   * See ISO 16022:2006, 5.2.9 and Annex B, B.2
   */
  Fallible<void> decodeBase256Segment(Ref<BitSource> bits, mb::stringstreamlite &result, std::vector<unsigned char> byteSegments) MB_NOEXCEPT_EXCEPT_BADALLOC;

  void parseTwoBytes(int firstByte, int secondByte, int*& result);
  /**
   * See ISO 16022:2006, Annex B, B.2
   */
  unsigned char unrandomize255State(int randomizedBase256Codeword,
                                          int base256CodewordPosition) {
    int pseudoRandomNumber = ((149 * base256CodewordPosition) % 255) + 1;
    int tempVariable = randomizedBase256Codeword - pseudoRandomNumber;
    return (unsigned char) (tempVariable >= 0 ? tempVariable : (tempVariable + 256));
  };

public:
  DecodedBitStreamParser() { };
  FallibleRef<DecoderResult> decode(ArrayRef<unsigned char> bytes) MB_NOEXCEPT_EXCEPT_BADALLOC;
};

}
}

