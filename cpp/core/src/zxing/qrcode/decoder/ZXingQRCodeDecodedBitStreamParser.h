// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-

#pragma once

/*
 *  DecodedBitStreamParser.h
 *  zxing
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

#include "zxing/common/Array.h"    // for ArrayRef
#include "zxing/common/Counted.h"  // for Ref
#include "zxing/common/Error.hpp"
#include "zxing/DecodeHints.h"     // for DecodeHintType

#include <map>                     // for map
#include <string>                  // for string
#include <stddef.h>                // for size_t

namespace pping {
class BitSource;
class DecoderResult;
namespace common {
class CharacterSetECI;
}  // namespace common
}  // namespace pping

namespace pping {
namespace qrcode {

class ErrorCorrectionLevel;
class Version;

class DecodedBitStreamParser {
public:
  typedef std::map<DecodeHintType, std::string> Hashtable;

private:
  static Fallible<void> decodingCodeWordsSucceeded_;
  static char const ALPHANUMERIC_CHARS[];
  static Fallible<char> toAlphaNumericChar(size_t value) noexcept;

  static Fallible<void> decodeHanziSegment(Ref<BitSource> bits, std::string &result, int count);
  static Fallible<void> decodeKanjiSegment(Ref<BitSource> bits, std::string &result, int count);
  static void decodeByteSegment(Ref<BitSource> bits, std::string &result, int count);
  static Fallible<void> decodeByteSegment(Ref<BitSource> bits_,
                                std::string& result,
                                int count,
                                pping::common::CharacterSetECI* currentCharacterSetECI,
                                ArrayRef< ArrayRef<unsigned char> >& byteSegments,
                                Hashtable const& hints);
  static Fallible<void> decodeAlphanumericSegment(Ref<BitSource> bits, std::string &result, int count, bool fc1InEffect);
  static Fallible<void> decodeNumericSegment(Ref<BitSource> bits, std::string &result, int count);

  static Fallible<void> append(std::string &ost, const unsigned char *bufIn, size_t nIn, const char *src);
  static Fallible<void> append(std::string &ost, std::string const& in, const char *src);

public:
  static FallibleRef<DecoderResult> decode(ArrayRef<unsigned char> bytes,
                                   Version *version,
                                   ErrorCorrectionLevel const& ecLevel,
                                   Hashtable const& hints);
};

}
}

