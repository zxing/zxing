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

#include <zxing/qrcode/decoder/DecodedBitStreamParser.h>
#include <iostream>
#ifndef NO_ICONV
#include <iconv.h>
#endif

// Required for compatibility. TODO: test on Symbian
#ifdef ZXING_ICONV_CONST
#undef ICONV_CONST
#define ICONV_CONST const
#endif

#ifndef ICONV_CONST
#define ICONV_CONST /**/
#endif

using namespace zxing;

namespace zxing {
namespace qrcode {

using namespace std;

const char DecodedBitStreamParser::ALPHANUMERIC_CHARS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
    'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
    'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':'
                                                          };

const char *DecodedBitStreamParser::ASCII = "ASCII";
const char *DecodedBitStreamParser::ISO88591 = "ISO-8859-1";
const char *DecodedBitStreamParser::UTF8 = "UTF-8";
const char *DecodedBitStreamParser::SHIFT_JIS = "SHIFT_JIS";
const char *DecodedBitStreamParser::EUC_JP = "EUC-JP";

void DecodedBitStreamParser::append(std::string &result, const unsigned char *bufIn, size_t nIn, const char *src) {
#ifndef NO_ICONV
  if (nIn == 0) {
    return;
  }

  iconv_t cd = iconv_open(UTF8, src);
  const int maxOut = 4 * nIn + 1;
  unsigned char* bufOut = new unsigned char[maxOut];

  ICONV_CONST char *fromPtr = (ICONV_CONST char *)bufIn;
  size_t nFrom = nIn;
  char *toPtr = (char *)bufOut;
  size_t nTo = maxOut;

  while (nFrom > 0) {
    size_t oneway = iconv(cd, &fromPtr, &nFrom, &toPtr, &nTo);
    if (oneway == (size_t)(-1)) {
      iconv_close(cd);
      delete[] bufOut;
      throw ReaderException("error converting characters");
    }
  }
  iconv_close(cd);

  int nResult = maxOut - nTo;
  bufOut[nResult] = '\0';
  result.append((const char *)bufOut);
  delete[] bufOut;
 #else
  result.append((const char *)bufIn, nIn);
 #endif
}

void DecodedBitStreamParser::decodeKanjiSegment(Ref<BitSource> bits, std::string &result, int count) {
  // Each character will require 2 bytes. Read the characters as 2-byte pairs
  // and decode as Shift_JIS afterwards
  size_t nBytes = 2 * count;
  unsigned char* buffer = new unsigned char[nBytes];
  int offset = 0;
  while (count > 0) {
    // Each 13 bits encodes a 2-byte character

    int twoBytes = bits->readBits(13);
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

  append(result, buffer, nBytes, SHIFT_JIS);
  delete[] buffer;
}

void DecodedBitStreamParser::decodeByteSegment(Ref<BitSource> bits, std::string &result, int count) {
  int nBytes = count;
  unsigned char* readBytes = new unsigned char[nBytes];
  if (count << 3 > bits->available()) {
    ostringstream s;
    s << "Count too large: " << count;
    delete[] readBytes;
    throw ReaderException(s.str().c_str());
  }
  for (int i = 0; i < count; i++) {
    readBytes[i] = (unsigned char)bits->readBits(8);
  }
  // The spec isn't clear on this mode; see
  // section 6.4.5: t does not say which encoding to assuming
  // upon decoding. I have seen ISO-8859-1 used as well as
  // Shift_JIS -- without anything like an ECI designator to
  // give a hint.
  const char *encoding = guessEncoding(readBytes, nBytes);
  append(result, readBytes, nBytes, encoding);
  delete[] readBytes;
}

void DecodedBitStreamParser::decodeNumericSegment(Ref<BitSource> bits, std::string &result, int count) {
  int nBytes = count;
  unsigned char* bytes = new unsigned char[nBytes];
  int i = 0;
  // Read three digits at a time
  while (count >= 3) {
    // Each 10 bits encodes three digits
    int threeDigitsBits = bits->readBits(10);
    if (threeDigitsBits >= 1000) {
      ostringstream s;
      s << "Illegal value for 3-digit unit: " << threeDigitsBits;
      delete[] bytes;
      throw ReaderException(s.str().c_str());
    }
    bytes[i++] = ALPHANUMERIC_CHARS[threeDigitsBits / 100];
    bytes[i++] = ALPHANUMERIC_CHARS[(threeDigitsBits / 10) % 10];
    bytes[i++] = ALPHANUMERIC_CHARS[threeDigitsBits % 10];
    count -= 3;
  }
  if (count == 2) {
    // Two digits left over to read, encoded in 7 bits
    int twoDigitsBits = bits->readBits(7);
    if (twoDigitsBits >= 100) {
      ostringstream s;
      s << "Illegal value for 2-digit unit: " << twoDigitsBits;
      delete[] bytes;
      throw ReaderException(s.str().c_str());
    }
    bytes[i++] = ALPHANUMERIC_CHARS[twoDigitsBits / 10];
    bytes[i++] = ALPHANUMERIC_CHARS[twoDigitsBits % 10];
  } else if (count == 1) {
    // One digit left over to read
    int digitBits = bits->readBits(4);
    if (digitBits >= 10) {
      ostringstream s;
      s << "Illegal value for digit unit: " << digitBits;
      delete[] bytes;
      throw ReaderException(s.str().c_str());
    }
    bytes[i++] = ALPHANUMERIC_CHARS[digitBits];
  }
  append(result, bytes, nBytes, ASCII);
  delete[] bytes;
}

void DecodedBitStreamParser::decodeAlphanumericSegment(Ref<BitSource> bits, std::string &result, int count) {
  int nBytes = count;
  unsigned char* bytes = new unsigned char[nBytes];
  int i = 0;
  // Read two characters at a time
  while (count > 1) {
    int nextTwoCharsBits = bits->readBits(11);
    bytes[i++] = ALPHANUMERIC_CHARS[nextTwoCharsBits / 45];
    bytes[i++] = ALPHANUMERIC_CHARS[nextTwoCharsBits % 45];
    count -= 2;
  }
  if (count == 1) {
    bytes[i++] = ALPHANUMERIC_CHARS[bits->readBits(6)];
  }
  append(result, bytes, nBytes, ASCII);
  delete[] bytes;
}

const char *
DecodedBitStreamParser::guessEncoding(unsigned char *bytes, int length) {
  const bool ASSUME_SHIFT_JIS = false;
  char const* const PLATFORM_DEFAULT_ENCODING="UTF-8";

  // Does it start with the UTF-8 byte order mark? then guess it's UTF-8
  if (length > 3 && bytes[0] == (unsigned char)0xEF && bytes[1] == (unsigned char)0xBB && bytes[2]
      == (unsigned char)0xBF) {
    return UTF8;
  }
  // For now, merely tries to distinguish ISO-8859-1, UTF-8 and Shift_JIS,
  // which should be by far the most common encodings. ISO-8859-1
  // should not have bytes in the 0x80 - 0x9F range, while Shift_JIS
  // uses this as a first byte of a two-byte character. If we see this
  // followed by a valid second byte in Shift_JIS, assume it is Shift_JIS.
  // If we see something else in that second byte, we'll make the risky guess
  // that it's UTF-8.
  bool canBeISO88591 = true;
  bool canBeShiftJIS = true;
  bool canBeUTF8 = true;
  int utf8BytesLeft = 0;
  int maybeDoubleByteCount = 0;
  int maybeSingleByteKatakanaCount = 0;
  bool sawLatin1Supplement = false;
  bool sawUTF8Start = false;
  bool lastWasPossibleDoubleByteStart = false;
  for (int i = 0;
       i < length && (canBeISO88591 || canBeShiftJIS || canBeUTF8);
       i++) {
    int value = bytes[i] & 0xFF;

    // UTF-8 stuff
    if (value >= 0x80 && value <= 0xBF) {
      if (utf8BytesLeft > 0) {
        utf8BytesLeft--;
      }
    } else {
      if (utf8BytesLeft > 0) {
        canBeUTF8 = false;
      }
      if (value >= 0xC0 && value <= 0xFD) {
        sawUTF8Start = true;
        int valueCopy = value;
        while ((valueCopy & 0x40) != 0) {
          utf8BytesLeft++;
          valueCopy <<= 1;
        }
      }
    }

    // Shift_JIS stuff

    if (value >= 0xA1 && value <= 0xDF) {
      // count the number of characters that might be a Shift_JIS single-byte Katakana character
      if (!lastWasPossibleDoubleByteStart) {
        maybeSingleByteKatakanaCount++;
      }
    }
    if (!lastWasPossibleDoubleByteStart &&
        ((value >= 0xF0 && value <= 0xFF) || value == 0x80 || value == 0xA0)) {
      canBeShiftJIS = false;
    }
    if (((value >= 0x81 && value <= 0x9F) || (value >= 0xE0 && value <= 0xEF))) {
      // These start double-byte characters in Shift_JIS. Let's see if it's followed by a valid
      // second byte.
      if (lastWasPossibleDoubleByteStart) {
        // If we just checked this and the last byte for being a valid double-byte
        // char, don't check starting on this byte. If this and the last byte
        // formed a valid pair, then this shouldn't be checked to see if it starts
        // a double byte pair of course.
        lastWasPossibleDoubleByteStart = false;
      } else {
        // ... otherwise do check to see if this plus the next byte form a valid
        // double byte pair encoding a character.
        lastWasPossibleDoubleByteStart = true;
        if (i >= length - 1) {
          canBeShiftJIS = false;
        } else {
          int nextValue = bytes[i + 1] & 0xFF;
          if (nextValue < 0x40 || nextValue > 0xFC) {
            canBeShiftJIS = false;
          } else {
            maybeDoubleByteCount++;
          }
          // There is some conflicting information out there about which bytes can follow which in
          // double-byte Shift_JIS characters. The rule above seems to be the one that matches practice.
        }
      }
    } else {
      lastWasPossibleDoubleByteStart = false;
    }
  }
  if (utf8BytesLeft > 0) {
    canBeUTF8 = false;
  }

  // Easy -- if assuming Shift_JIS and no evidence it can't be, done
  if (canBeShiftJIS && ASSUME_SHIFT_JIS) {
    return SHIFT_JIS;
  }
  if (canBeUTF8 && sawUTF8Start) {
    return UTF8;
  }
  // Distinguishing Shift_JIS and ISO-8859-1 can be a little tough. The crude heuristic is:
  // - If we saw
  //   - at least 3 bytes that starts a double-byte value (bytes that are rare in ISO-8859-1), or
  //   - over 5% of bytes could be single-byte Katakana (also rare in ISO-8859-1),
  // - and, saw no sequences that are invalid in Shift_JIS, then we conclude Shift_JIS
  if (canBeShiftJIS && (maybeDoubleByteCount >= 3 || 20 * maybeSingleByteKatakanaCount > length)) {
    return SHIFT_JIS;
  }
  // Otherwise, we default to ISO-8859-1 unless we know it can't be
  if (!sawLatin1Supplement && canBeISO88591) {
    return ISO88591;
  }
  // Otherwise, we take a wild guess with platform encoding
  return PLATFORM_DEFAULT_ENCODING;
}

string DecodedBitStreamParser::decode(ArrayRef<unsigned char> bytes, Version *version) {
  string result;
  Ref<BitSource> bits(new BitSource(bytes));
  Mode *mode = &Mode::TERMINATOR;
  do {
    // While still another segment to read...
    if (bits->available() < 4) {
      // OK, assume we're done. Really, a TERMINATOR mode should have been recorded here
      mode = &Mode::TERMINATOR;
    } else {
      mode = &Mode::forBits(bits->readBits(4)); // mode is encoded by 4 bits
    }
    if (mode != &Mode::TERMINATOR) {
      // How many characters will follow, encoded in this mode?
      int count = bits->readBits(mode->getCharacterCountBits(version));
      if (mode == &Mode::NUMERIC) {
        decodeNumericSegment(bits, result, count);
      } else if (mode == &Mode::ALPHANUMERIC) {
        decodeAlphanumericSegment(bits, result, count);
      } else if (mode == &Mode::BYTE) {
        decodeByteSegment(bits, result, count);
      } else if (mode == &Mode::KANJI) {
        decodeKanjiSegment(bits, result, count);
      } else {
        throw ReaderException("Unsupported mode indicator");
      }
    }
  } while (mode != &Mode::TERMINATOR);
  return result;
}

}
}
