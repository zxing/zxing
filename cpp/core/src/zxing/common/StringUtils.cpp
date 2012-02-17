// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-

/*
 * Copyright (C) 2010-2011 ZXing authors
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

#include <zxing/common/StringUtils.h>
#include <zxing/DecodeHints.h>

using namespace std;
using namespace zxing;
using namespace zxing::common;

// N.B.: these are the iconv strings for at least some versions of iconv

char const* const StringUtils::PLATFORM_DEFAULT_ENCODING = "UTF-8";
char const* const StringUtils::ASCII = "ASCII";
char const* const StringUtils::SHIFT_JIS = "SHIFT_JIS";
char const* const StringUtils::GB2312 = "GBK";
char const* const StringUtils::EUC_JP = "EUC-JP";
char const* const StringUtils::UTF8 = "UTF-8";
char const* const StringUtils::ISO88591 = "ISO8859-1";
const bool StringUtils::ASSUME_SHIFT_JIS = false;

string
StringUtils::guessEncoding(unsigned char* bytes, int length,
                           Hashtable const& hints) {
  Hashtable::const_iterator i = hints.find(DecodeHints::CHARACTER_SET);
  if (i != hints.end()) {
    return i->second;
  }
  typedef bool boolean;
  // For now, merely tries to distinguish ISO-8859-1, UTF-8 and Shift_JIS,
  // which should be by far the most common encodings.
  boolean canBeISO88591 = true;
  boolean canBeShiftJIS = true;
  boolean canBeUTF8 = true;
  int utf8BytesLeft = 0;
  //int utf8LowChars = 0;
  int utf2BytesChars = 0;
  int utf3BytesChars = 0;
  int utf4BytesChars = 0;
  int sjisBytesLeft = 0;
  //int sjisLowChars = 0;
  int sjisKatakanaChars = 0;
  //int sjisDoubleBytesChars = 0;
  int sjisCurKatakanaWordLength = 0;
  int sjisCurDoubleBytesWordLength = 0;
  int sjisMaxKatakanaWordLength = 0;
  int sjisMaxDoubleBytesWordLength = 0;
  //int isoLowChars = 0;
  //int isoHighChars = 0;
  int isoHighOther = 0;

  typedef unsigned char byte;
  boolean utf8bom = length > 3 &&
    bytes[0] == (byte) 0xEF &&
    bytes[1] == (byte) 0xBB &&
    bytes[2] == (byte) 0xBF;

  for (int i = 0;
       i < length && (canBeISO88591 || canBeShiftJIS || canBeUTF8);
       i++) {

    int value = bytes[i] & 0xFF;

    // UTF-8 stuff
    if (canBeUTF8) {
      if (utf8BytesLeft > 0) {
        if ((value & 0x80) == 0) {
          canBeUTF8 = false;
        } else {
          utf8BytesLeft--;
        }
      } else if ((value & 0x80) != 0) {
        if ((value & 0x40) == 0) {
          canBeUTF8 = false;
        } else {
          utf8BytesLeft++;
          if ((value & 0x20) == 0) {
            utf2BytesChars++;
          } else {
            utf8BytesLeft++;
            if ((value & 0x10) == 0) {
              utf3BytesChars++;
            } else {
              utf8BytesLeft++;
              if ((value & 0x08) == 0) {
                utf4BytesChars++;
              } else {
                canBeUTF8 = false;
              }
            }
          }
        }
      } //else {
      //utf8LowChars++;
      //}
    }

    // ISO-8859-1 stuff
    if (canBeISO88591) {
      if (value > 0x7F && value < 0xA0) {
        canBeISO88591 = false;
      } else if (value > 0x9F) {
        if (value < 0xC0 || value == 0xD7 || value == 0xF7) {
          isoHighOther++;
        } //else {
        //isoHighChars++;
        //}
      } //else {
      //isoLowChars++;
      //}
    }

    // Shift_JIS stuff
    if (canBeShiftJIS) {
      if (sjisBytesLeft > 0) {
        if (value < 0x40 || value == 0x7F || value > 0xFC) {
          canBeShiftJIS = false;
        } else {
          sjisBytesLeft--;
        }
      } else if (value == 0x80 || value == 0xA0 || value > 0xEF) {
        canBeShiftJIS = false;
      } else if (value > 0xA0 && value < 0xE0) {
        sjisKatakanaChars++;
        sjisCurDoubleBytesWordLength = 0;
        sjisCurKatakanaWordLength++;
        if (sjisCurKatakanaWordLength > sjisMaxKatakanaWordLength) {
          sjisMaxKatakanaWordLength = sjisCurKatakanaWordLength;
        }
      } else if (value > 0x7F) {
        sjisBytesLeft++;
        //sjisDoubleBytesChars++;
        sjisCurKatakanaWordLength = 0;
        sjisCurDoubleBytesWordLength++;
        if (sjisCurDoubleBytesWordLength > sjisMaxDoubleBytesWordLength) {
          sjisMaxDoubleBytesWordLength = sjisCurDoubleBytesWordLength;
        }
      } else {
        //sjisLowChars++;
        sjisCurKatakanaWordLength = 0;
        sjisCurDoubleBytesWordLength = 0;
      }
    }
  }

  if (canBeUTF8 && utf8BytesLeft > 0) {
    canBeUTF8 = false;
  }
  if (canBeShiftJIS && sjisBytesLeft > 0) {
    canBeShiftJIS = false;
  }

  // Easy -- if there is BOM or at least 1 valid not-single byte character (and no evidence it can't be UTF-8), done
  if (canBeUTF8 && (utf8bom || utf2BytesChars + utf3BytesChars + utf4BytesChars > 0)) {
    return UTF8;
  }
  // Easy -- if assuming Shift_JIS or at least 3 valid consecutive not-ascii characters (and no evidence it can't be), done
  if (canBeShiftJIS && (ASSUME_SHIFT_JIS || sjisMaxKatakanaWordLength >= 3 || sjisMaxDoubleBytesWordLength >= 3)) {
    return SHIFT_JIS;
  }
  // Distinguishing Shift_JIS and ISO-8859-1 can be a little tough for short words. The crude heuristic is:
  // - If we saw
  //   - only two consecutive katakana chars in the whole text, or
  //   - at least 10% of bytes that could be "upper" not-alphanumeric Latin1,
  // - then we conclude Shift_JIS, else ISO-8859-1
  if (canBeISO88591 && canBeShiftJIS) {
    return (sjisMaxKatakanaWordLength == 2 && sjisKatakanaChars == 2) || isoHighOther * 10 >= length
      ? SHIFT_JIS : ISO88591;
  }

  // Otherwise, try in order ISO-8859-1, Shift JIS, UTF-8 and fall back to default platform encoding
  if (canBeISO88591) {
    return ISO88591;
  }
  if (canBeShiftJIS) {
    return SHIFT_JIS;
  }
  if (canBeUTF8) {
    return UTF8;
  }
  // Otherwise, we take a wild guess with platform encoding
  return PLATFORM_DEFAULT_ENCODING;
}
