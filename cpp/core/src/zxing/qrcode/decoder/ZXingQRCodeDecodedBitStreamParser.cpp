// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  DecodedBitStreamParser.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 20/05/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include <zxing/FormatException.h>                  // for FormatException
#include <zxing/common/CharacterSetECI.h>           // for CharacterSetECI
#include <zxing/common/StringUtils.h>               // for StringUtils, pping::common::StringUtils::ASCII, pping::common::StringUtils::GB2312, pping::common::StringUtils::SHIFT_JIS, pping::common::StringUtils::UTF8
#include <zxing/qrcode/decoder/ZXingQRCodeDecodedBitStreamParser.h>
#include <string>                                   // for basic_string, char_traits, allocator
#include <vector>                                   // for vector

#ifndef NO_ICONV
#   include "Wrappers/Iconv.hpp"                    // for iconv_close, iconv, iconv_open, iconv_t
#   ifdef __APPLE__
#       include <TargetConditionals.h>
#   endif
#endif
#include "zxing/ReaderException.h"                  // for ReaderException
#include "zxing/common/Array.h"                     // for ArrayRef, Array
#include "zxing/common/BitSource.h"                 // for BitSource
#include "zxing/common/Counted.h"                   // for Ref
#include "zxing/common/DecoderResult.h"             // for DecoderResult
#include "zxing/common/IllegalArgumentException.h"  // for IllegalArgumentException
#include "zxing/common/Str.h"                       // for String
#include "zxing/qrcode/ErrorCorrectionLevel.h"      // for ErrorCorrectionLevel
#include "zxing/qrcode/decoder/Mode.h"              // for Mode, Mode::TERMINATOR, Mode::ALPHANUMERIC, Mode::BYTE, Mode::ECI, Mode::FNC1_FIRST_POSITION, Mode::FNC1_SECOND_POSITION, Mode::HANZI

#include <Utils/Macros.h>
#include <Utils/stringstreamlite.hpp>

namespace pping {
namespace qrcode {
class Version;
}  // namespace qrcode
}  // namespace pping


#ifndef NO_ICONV
#   if TARGET_OS_IPHONE || defined( __EMSCRIPTEN__ )
#       define ICONV_CONST
#   else
#       define ICONV_CONST const
#   endif
#endif

using namespace std;
using namespace pping;
using namespace pping::qrcode;
using namespace pping::common;

const char DecodedBitStreamParser::ALPHANUMERIC_CHARS[] =
{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
  'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
  'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
  'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':'
};

namespace {int GB2312_SUBSET = 1;}

Fallible<void> DecodedBitStreamParser::append(std::string &result,
                                    string const& in,
                                    const char *src) {
  return append(result, (unsigned char const*)in.c_str(), in.length(), src);
}

Fallible<void> DecodedBitStreamParser::append(std::string &result,
                                    const unsigned char *bufIn,
                                    size_t nIn,
                                    [[maybe_unused]] const char *src) {
#ifndef NO_ICONV
  if (nIn == 0) {
    return success(); // ???
  }

  iconv_t cd = iconv_open(pping::common::StringUtils::UTF8, src);
  if (cd == (iconv_t)-1) {
    result.append((const char *)bufIn, nIn);
    return success();
  }

  const int maxOut = 4 * (int)nIn + 1;
  unsigned char* bufOut = new unsigned char[maxOut];

  size_t nFrom = nIn;
  char *toPtr = (char *)bufOut;
  size_t nTo = maxOut;

  while (nFrom > 0) {
    ICONV_CONST char *fromPtr = (ICONV_CONST char *)bufIn;
    size_t oneway = iconv(cd, &fromPtr, &nFrom, &toPtr, &nTo);
    if (oneway == (size_t)(-1)) {
      iconv_close(cd);
      delete[] bufOut;
      return failure<ReaderException>("Error converting characters");
    }
  }
  iconv_close(cd);

  int nResult = maxOut - (int)nTo;
  bufOut[nResult] = '\0';
  result.append((const char *)bufOut);
  delete[] bufOut;
#else
  result.append((const char *)bufIn, nIn);
#endif

  return success();
}

Fallible<void> DecodedBitStreamParser::decodeHanziSegment(Ref<BitSource> bits_,
                                                string& result,
                                                int count) {
    BitSource& bits (*bits_);
    // Don't crash trying to read more bits than we have available.
    if (count * 13 > bits.available()) {
      return failure<FormatException>("Attempting to read more bits than available");
    }

    // Each character will require 2 bytes. Read the characters as 2-byte pairs
    // and decode as GB2312 afterwards
    size_t nBytes = 2 * count;
    unsigned char* buffer = new unsigned char[nBytes];
    int offset = 0;
    while (count > 0) {
      // Each 13 bits encodes a 2-byte character
        auto const tryRead13(bits.readBits(13));
        if(!tryRead13)
            return tryRead13.error();

      int twoBytes = *tryRead13;
      int assembledTwoBytes = ((twoBytes / 0x060) << 8) | (twoBytes % 0x060);
      if (assembledTwoBytes < 0x003BF) {
        // In the 0xA1A1 to 0xAAFE range
        assembledTwoBytes += 0x0A1A1;
      } else {
        // In the 0xB0A1 to 0xFAFE range
        assembledTwoBytes += 0x0A6A1;
      }
      buffer[offset] = (unsigned char) ((assembledTwoBytes >> 8) & 0xFF);
      buffer[offset + 1] = (unsigned char) (assembledTwoBytes & 0xFF);
      offset += 2;
      count--;
    }

    auto const attemptAppend(append(result, buffer, nBytes, pping::common::StringUtils::GB2312));

    delete [] buffer;

    if(!attemptAppend)
        return attemptAppend.error();

    return success();
}

Fallible<void> DecodedBitStreamParser::decodeKanjiSegment(Ref<BitSource> bits, std::string &result, int count) {
  // Each character will require 2 bytes. Read the characters as 2-byte pairs
  // and decode as Shift_JIS afterwards
  size_t nBytes = 2 * count;
  auto const buffer = new unsigned char[nBytes];
  int offset = 0;
  while (count > 0) {
    // Each 13 bits encodes a 2-byte character
      auto const tryRead13(bits->readBits(13));
      if(!tryRead13)
          return tryRead13.error();

    int twoBytes = *tryRead13;
    int assembledTwoBytes = ((twoBytes / 0x0C0) << 8) | (twoBytes % 0x0C0);
    if (assembledTwoBytes < 0x01F00) {
      // In the 0x8140 to 0x9FFC range
      assembledTwoBytes += 0x08140;
    } else {
      // In the 0xE040 to 0xEBBF range
      assembledTwoBytes += 0x0C140;
    }
    buffer[offset] = (unsigned char)(assembledTwoBytes >> 8);
    buffer[offset + 1] = (unsigned char)assembledTwoBytes;
    offset += 2;
    count--;
  }

  auto const attemptAppend(append(result, buffer, nBytes, pping::common::StringUtils::SHIFT_JIS));
  delete[] buffer;
  if(!attemptAppend)
      return attemptAppend.error();

  return success();
}

Fallible<void> DecodedBitStreamParser::decodeByteSegment(Ref<BitSource> bits_,
                                               string& result,
                                               int count,
                                               CharacterSetECI* currentCharacterSetECI,
                                               ArrayRef< ArrayRef<unsigned char> >& byteSegments,
                                               Hashtable const& hints) {
  int nBytes = count;
  BitSource& bits (*bits_);
  // Don't crash trying to read more bits than we have available.
  if (count << 3 > bits.available()) {
    return failure<FormatException>("Attempting to read more bits than available");
  }

  ArrayRef<unsigned char> bytes_ (count);
  unsigned char* readBytes = bytes_->values_.data();
  for (int i = 0; i < count; i++) {

      auto const tryReadByte(bits.readBits(8));
      if(!tryReadByte)
          return tryReadByte.error();

      readBytes[i] = (unsigned char) (*tryReadByte);
  }
  string encoding;
  if (currentCharacterSetECI == 0) {
    // The spec isn't clear on this mode; see
    // section 6.4.5: t does not say which encoding to assuming
    // upon decoding. I have seen ISO-8859-1 used as well as
    // Shift_JIS -- without anything like an ECI designator to
    // give a hint.
    encoding = pping::common::StringUtils::guessEncoding(readBytes, count, hints);
  } else {
    encoding = currentCharacterSetECI->name();
  }

  auto const attemptAppend(append(result, readBytes, nBytes, encoding.c_str()));
  if(!attemptAppend)
      return attemptAppend.error();

  byteSegments->values().push_back(bytes_);

  return success();
}

Fallible<void> DecodedBitStreamParser::decodeNumericSegment(Ref<BitSource> bits, std::string &result, int count) {
  int nBytes = count;
  unsigned char* bytes = new unsigned char[nBytes];
  int i = 0;
  // Read three digits at a time
  while (count >= 3) {
    // Each 10 bits encodes three digits

    if (bits->available() < 10) {
        delete[] bytes;
        return failure<FormatException>("Requires 10 bits for 3 digits");
    }
    auto const tryRead10(bits->readBits(10));
    if(!tryRead10)
        return tryRead10.error();

    int threeDigitsBits = *tryRead10;
    if (threeDigitsBits >= 1000) {
      std::string s("Illegal value for 3-digit unit: " + std::to_string(threeDigitsBits));
      delete[] bytes;

      return failure<ReaderException>(s.c_str());
    }
    bytes[i++] = ALPHANUMERIC_CHARS[threeDigitsBits / 100];
    bytes[i++] = ALPHANUMERIC_CHARS[(threeDigitsBits / 10) % 10];
    bytes[i++] = ALPHANUMERIC_CHARS[threeDigitsBits % 10];
    count -= 3;
  }
  if (count == 2) {

    if (bits->available() < 7) {

        delete[] bytes;
        return failure<FormatException>("Requires 7 bits for 2 digits");
    }
    // Two digits left over to read, encoded in 7 bits
    auto const tryRead7(bits->readBits(7));
    if(!tryRead7)
        return tryRead7.error();

    int twoDigitsBits = *tryRead7;

    if (twoDigitsBits >= 100) {
      std::string s("Illegal value for 2-digit unit: " + std::to_string(twoDigitsBits));
      delete[] bytes;

      return failure<ReaderException>(s.c_str());
    }
    bytes[i++] = ALPHANUMERIC_CHARS[twoDigitsBits / 10];
    bytes[i++] = ALPHANUMERIC_CHARS[twoDigitsBits % 10];

  } else if (count == 1) {

    if (bits->available() < 4) {

        delete[] bytes;
        return failure<FormatException>("Requires 4 bits for digit");
    }
    // One digit left over to read

    auto const tryRead4(bits->readBits(4));
    if(!tryRead4)
        return tryRead4.error();

    int digitBits = *tryRead4;
    if (digitBits >= 10) {
      std::string s("Illegal value for digit unit: " + std::to_string(digitBits));

      delete[] bytes;
      return failure<ReaderException>(s.c_str());
    }
    bytes[i++] = ALPHANUMERIC_CHARS[digitBits];
  }
  auto const attemptAppend(append(result, bytes, nBytes, pping::common::StringUtils::ASCII));
  delete[] bytes;

  if(!attemptAppend)
      return attemptAppend.error();

  return success();
}

Fallible<char> DecodedBitStreamParser::toAlphaNumericChar(size_t value) noexcept {

  if (value >= sizeof(DecodedBitStreamParser::ALPHANUMERIC_CHARS))
      return failure<FormatException>("Invalid code for alphanumeric char");

  return ALPHANUMERIC_CHARS[value];
}

Fallible<void> DecodedBitStreamParser::decodeAlphanumericSegment(Ref<BitSource> bits_,
                                                       string& result,
                                                       int count,
                                                       bool fc1InEffect) {
  BitSource& bits (*bits_);
  mb::stringstreamlite bytes;
  // Read two characters at a time
  while (count > 1) {
    if (bits.available() < 11) {
      return failure<FormatException>("Two character group requires 11 bits");
    }
    auto const tryRead11(bits.readBits(11));
    if(!tryRead11)
        return tryRead11.error();

    int nextTwoCharsBits = *tryRead11;

    auto const readFirst(toAlphaNumericChar(nextTwoCharsBits / 45));
    auto const readSecond(toAlphaNumericChar(nextTwoCharsBits % 45));

    if(!readFirst)
        return readFirst.error();
    if(!readSecond)
        return readSecond.error();

    bytes << *readFirst;
    bytes << *readSecond;
    count -= 2;
  }
  if (count == 1) {
    // special case: one character left
    if (bits.available() < 6) {
      return failure<FormatException>("One character group requires 6 bits");
    }
    auto const tryRead6(bits.readBits(6));
    if(!tryRead6)
        return tryRead6.error();

    auto const readChar(toAlphaNumericChar(*tryRead6));
    if(!readChar)
        return readChar.error();

    bytes << *readChar;
  }
  // See section 6.4.8.1, 6.4.8.2
  string s = bytes.str();
  if (fc1InEffect) {
    // We need to massage the result a bit if in an FNC1 mode:
    std::string r = "";
    for (size_t i = 0; i < s.length(); i++) {
      if (s[i] != '%') {
        r += s[i];
      } else {
        if (i < s.length() - 1 && s[i + 1] == '%') {
          // %% is rendered as %
          r += s[i++];
        } else {
          // In alpha mode, % should be converted to FNC1 separator 0x1D
          r += (char)0x1D;
        }
      }
    }
    s = r;
  }
  return append(result, s, pping::common::StringUtils::ASCII);
}

namespace {
  Fallible<int> parseECIValue(BitSource& bits) {
    auto const tryReadFirstByte(bits.readBits(8));
    if(!tryReadFirstByte)
        return tryReadFirstByte.error();

    int firstByte = *tryReadFirstByte;
    if ((firstByte & 0x80) == 0) {
      // just one byte
      return firstByte & 0x7F;
    }
    if ((firstByte & 0xC0) == 0x80) {
      // two bytes
        auto const tryReadSecondByte(bits.readBits(8));
        if(!tryReadSecondByte)
            return tryReadSecondByte.error();

      int secondByte = *tryReadSecondByte;
      return ((firstByte & 0x3F) << 8) | secondByte;
    }
    if ((firstByte & 0xE0) == 0xC0) {
      // three bytes
        auto const tryReadThreeBytes(bits.readBits(16));
        if(!tryReadThreeBytes)
            return tryReadThreeBytes.error();

      int secondThirdBytes = *tryReadThreeBytes;
      return ((firstByte & 0x1F) << 16) | secondThirdBytes;
    }
    return failure<FormatException>("Invalid bits for ECI value");
  }
}

FallibleRef<DecoderResult>
DecodedBitStreamParser::decode(ArrayRef<unsigned char> bytes,
                               Version* version,
                               ErrorCorrectionLevel const& ecLevel,
                               Hashtable const& hints) {
  Ref<BitSource> bits_ (new BitSource(bytes));
  BitSource& bits (*bits_);
  string result;
  CharacterSetECI* currentCharacterSetECI = 0;
  bool fc1InEffect = false;
  ArrayRef< ArrayRef<unsigned char> > byteSegments (size_t(0));
  Mode* mode = 0;
  do {
    // While still another segment to read...
    if (bits.available() < 4) {
      // OK, assume we're done. Really, a TERMINATOR mode should have been recorded here
      mode = &Mode::TERMINATOR;
    } else {
        auto const tryRead4(bits.readBits(4));
        if(!tryRead4)
            return tryRead4.error();

        mode = &Mode::forBits(*tryRead4); // mode is encoded by 4 bits
        if(mode == &Mode::INVALID)
            return failure<ReaderException>("Invalid mode");
    }
    if (mode != &Mode::TERMINATOR) {
      if ((mode == &Mode::FNC1_FIRST_POSITION) || (mode == &Mode::FNC1_SECOND_POSITION)) {
        // We do little with FNC1 except alter the parsed result a bit according to the spec
        fc1InEffect = true;
      } else if (mode == &Mode::STRUCTURED_APPEND) {
        // not really supported; all we do is ignore it
        // Read next 8 bits (symbol sequence #) and 8 bits (parity data), then continue
        auto const tryRead(bits.readBits(16));
        if(!tryRead)
            return tryRead.error();

      } else if (mode == &Mode::ECI) {
        // Count doesn't apply to ECI
          auto const parseEci(parseECIValue(bits));
          if(!parseEci)
              return parseEci.error();

        int value = *parseEci;
        auto const tryGetCharset(CharacterSetECI::getCharacterSetECIByValue(value));
        if(!tryGetCharset)
            return tryGetCharset.error();

        currentCharacterSetECI = *tryGetCharset;
        if (currentCharacterSetECI == 0) {
          return failure<FormatException>("Unable to determine character set, can't read data");
        }
      } else {
        // First handle Hanzi mode which does not start with character count
        if (mode == &Mode::HANZI) {
          //chinese mode contains a sub set indicator right after mode indicator
            auto const tryRead4(bits.readBits(4));
            if(!tryRead4)
                return tryRead4.error();

          int subset = *tryRead4;

          auto const tryRead(bits.readBits(mode->getCharacterCountBits(version)));
          if(!tryRead)
              return tryRead.error();

          int countHanzi = *tryRead;

          if (subset == GB2312_SUBSET) {

            auto const attemptHanzi(decodeHanziSegment(bits_, result, countHanzi));
            if(!attemptHanzi)
                return attemptHanzi.error();
          }
        } else {
          // "Normal" QR code modes:
          // How many characters will follow, encoded in this mode?
            auto const tryRead(bits.readBits(mode->getCharacterCountBits(version)));
            if(!tryRead)
                return tryRead.error();

            int count = *tryRead;

          if (mode == &Mode::NUMERIC) {

            auto const attemptDecode(decodeNumericSegment(bits_, result, count));
            if(!attemptDecode)
                return attemptDecode.error();

          } else if (mode == &Mode::ALPHANUMERIC) {

            auto const attemptDecode(decodeAlphanumericSegment(bits_, result, count, fc1InEffect));
            if(!attemptDecode)
                return attemptDecode.error();

          } else if (mode == &Mode::BYTE) {

            auto const attemptDecode(decodeByteSegment(bits_, result, count, currentCharacterSetECI, byteSegments, hints));
            if(!attemptDecode)
                return attemptDecode.error();

          } else if (mode == &Mode::KANJI) {

            auto const attemptDecode(decodeKanjiSegment(bits_, result, count));
            if(!attemptDecode)
                return attemptDecode.error();

          } else {
            MB_ASSERTM(false, "%s", "No such mode");
          }
        }
      }
    }
  } while (mode != &Mode::TERMINATOR);

      ArrayRef<unsigned char> resultBytes(result.length());
      for (int j = 0; j < (int) result.size(); ++j) {
          resultBytes[j] = (unsigned char)result[j];
      }
  return Ref<DecoderResult>(new DecoderResult(resultBytes, Ref<String>(new String(result)), byteSegments, (string)ecLevel));
}

