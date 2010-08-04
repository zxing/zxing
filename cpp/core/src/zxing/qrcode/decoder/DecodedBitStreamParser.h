#ifndef __DECODED_BIT_STREAM_PARSER_H__
#define __DECODED_BIT_STREAM_PARSER_H__

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

#include <string>
#include <sstream>
#include <zxing/qrcode/decoder/Mode.h>
#include <zxing/common/BitSource.h>
#include <zxing/common/Counted.h>
#include <zxing/common/Array.h>



namespace zxing {
namespace qrcode {

class DecodedBitStreamParser {
private:
  static const char ALPHANUMERIC_CHARS[];

  static const char *ASCII;
  static const char *ISO88591;
  static const char *UTF8;
  static const char *SHIFT_JIS;
  static const char *EUC_JP;

  static void decodeKanjiSegment(Ref<BitSource> bits, std::string &result, int count);
  static void decodeByteSegment(Ref<BitSource> bits, std::string &result, int count);
  static void decodeAlphanumericSegment(Ref<BitSource> bits, std::string &result, int count);
  static void decodeNumericSegment(Ref<BitSource> bits, std::string &result, int count);
  static const char *guessEncoding(unsigned char *bytes, int length);
  static void append(std::string &ost, const unsigned char *bufIn, size_t nIn, const char *src);

public:
  static std::string decode(ArrayRef<unsigned char> bytes, Version *version);
};

}
}

#endif // __DECODED_BIT_STREAM_PARSER_H__
