/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.common;

import java.nio.charset.Charset;
import java.util.Map;

import com.google.zxing.DecodeHintType;

import com.google.zxing.CoverageTool2000;

/**
 * Common string-related functions.
 *
 * @author Sean Owen
 * @author Alex Dupre
 */
public final class StringUtils {

  private static final String PLATFORM_DEFAULT_ENCODING = Charset.defaultCharset().name();
  public static final String SHIFT_JIS = "SJIS";
  public static final String GB2312 = "GB2312";
  private static final String EUC_JP = "EUC_JP";
  private static final String UTF8 = "UTF8";
  private static final String ISO88591 = "ISO8859_1";
  private static final boolean ASSUME_SHIFT_JIS =
      SHIFT_JIS.equalsIgnoreCase(PLATFORM_DEFAULT_ENCODING) ||
      EUC_JP.equalsIgnoreCase(PLATFORM_DEFAULT_ENCODING);

  private StringUtils() { }

  /**
   * @param bytes bytes encoding a string, whose encoding should be guessed
   * @param hints decode hints if applicable
   * @return name of guessed encoding; at the moment will only guess one of:
   *  {@link #SHIFT_JIS}, {@link #UTF8}, {@link #ISO88591}, or the platform
   *  default encoding if none of these can possibly be correct
   */
  public static String guessEncoding(byte[] bytes, Map<DecodeHintType,?> hints) {
    if (hints != null && hints.containsKey(DecodeHintType.CHARACTER_SET)) {
      CoverageTool2000.setCoverageMatrix(5, 0);
      return hints.get(DecodeHintType.CHARACTER_SET).toString();
    }
    CoverageTool2000.setCoverageMatrix(5, 1);

    // For now, merely tries to distinguish ISO-8859-1, UTF-8 and Shift_JIS,
    // which should be by far the most common encodings.
    int length = bytes.length;
    boolean canBeISO88591 = true;
    boolean canBeShiftJIS = true;
    boolean canBeUTF8 = true;
    int utf8BytesLeft = 0;
    int utf2BytesChars = 0;
    int utf3BytesChars = 0;
    int utf4BytesChars = 0;
    int sjisBytesLeft = 0;
    int sjisKatakanaChars = 0;
    int sjisCurKatakanaWordLength = 0;
    int sjisCurDoubleBytesWordLength = 0;
    int sjisMaxKatakanaWordLength = 0;
    int sjisMaxDoubleBytesWordLength = 0;
    int isoHighOther = 0;

    boolean utf8bom = bytes.length > 3 &&
        bytes[0] == (byte) 0xEF &&
        bytes[1] == (byte) 0xBB &&
        bytes[2] == (byte) 0xBF;

    for (int i = 0;
         i < length && (canBeISO88591 || canBeShiftJIS || canBeUTF8);
         i++) {

      int value = bytes[i] & 0xFF;

      // UTF-8 stuff
      if (canBeUTF8) {
        CoverageTool2000.setCoverageMatrix(5, 2);
        if (utf8BytesLeft > 0) {
          CoverageTool2000.setCoverageMatrix(5, 3);
          if ((value & 0x80) == 0) {
            CoverageTool2000.setCoverageMatrix(5, 4);
            canBeUTF8 = false;
          } else {
            CoverageTool2000.setCoverageMatrix(5, 5);
            utf8BytesLeft--;
          }
        } else if ((value & 0x80) != 0) {
          CoverageTool2000.setCoverageMatrix(5, 6);
          if ((value & 0x40) == 0) {
            CoverageTool2000.setCoverageMatrix(5, 7);
            canBeUTF8 = false;
          } else {
            CoverageTool2000.setCoverageMatrix(5, 8);
            utf8BytesLeft++;
            if ((value & 0x20) == 0) {
              CoverageTool2000.setCoverageMatrix(5, 9);
              utf2BytesChars++;
            } else {
              CoverageTool2000.setCoverageMatrix(5, 10);
              utf8BytesLeft++;
              if ((value & 0x10) == 0) {
                CoverageTool2000.setCoverageMatrix(5, 11);
                utf3BytesChars++;
              } else {
                CoverageTool2000.setCoverageMatrix(5, 12);
                utf8BytesLeft++;
                if ((value & 0x08) == 0) {
                  CoverageTool2000.setCoverageMatrix(5, 13);
                  utf4BytesChars++;
                } else {
                  CoverageTool2000.setCoverageMatrix(5, 14);
                  canBeUTF8 = false;
                }
              }
            }
          }
        } else {
          CoverageTool2000.setCoverageMatrix(5, 15);
        }
      } else {
        CoverageTool2000.setCoverageMatrix(5, 16);
      }

      // ISO-8859-1 stuff
      if (canBeISO88591) {
        CoverageTool2000.setCoverageMatrix(5, 17);
        if (value > 0x7F && value < 0xA0) {
          CoverageTool2000.setCoverageMatrix(5, 18);
          canBeISO88591 = false;
        } else if (value > 0x9F && (value < 0xC0 || value == 0xD7 || value == 0xF7)) {
          CoverageTool2000.setCoverageMatrix(5, 19);
          isoHighOther++;
        } else {
          CoverageTool2000.setCoverageMatrix(5, 20);
        }
      } else {
        CoverageTool2000.setCoverageMatrix(5, 21);
      }

      // Shift_JIS stuff
      if (canBeShiftJIS) {
        CoverageTool2000.setCoverageMatrix(5, 22);
        if (sjisBytesLeft > 0) {
          CoverageTool2000.setCoverageMatrix(5, 23);
          if (value < 0x40 || value == 0x7F || value > 0xFC) {
            CoverageTool2000.setCoverageMatrix(5, 24);
            canBeShiftJIS = false;
          } else {
            CoverageTool2000.setCoverageMatrix(5, 25);
            sjisBytesLeft--;
          }
        } else if (value == 0x80 || value == 0xA0 || value > 0xEF) {
          CoverageTool2000.setCoverageMatrix(5, 26);
          canBeShiftJIS = false;
        } else if (value > 0xA0 && value < 0xE0) {
          CoverageTool2000.setCoverageMatrix(5, 27);
          sjisKatakanaChars++;
          sjisCurDoubleBytesWordLength = 0;
          sjisCurKatakanaWordLength++;
          if (sjisCurKatakanaWordLength > sjisMaxKatakanaWordLength) {
            CoverageTool2000.setCoverageMatrix(5, 28);
            sjisMaxKatakanaWordLength = sjisCurKatakanaWordLength;
          } else {
            CoverageTool2000.setCoverageMatrix(5, 29);
          }
        } else if (value > 0x7F) {
          CoverageTool2000.setCoverageMatrix(5, 30);
          sjisBytesLeft++;
          //sjisDoubleBytesChars++;
          sjisCurKatakanaWordLength = 0;
          sjisCurDoubleBytesWordLength++;
          if (sjisCurDoubleBytesWordLength > sjisMaxDoubleBytesWordLength) {
            CoverageTool2000.setCoverageMatrix(5, 31);
            sjisMaxDoubleBytesWordLength = sjisCurDoubleBytesWordLength;
          } else {
            CoverageTool2000.setCoverageMatrix(5, 32);
          }
        } else {
          CoverageTool2000.setCoverageMatrix(5, 33);
          //sjisLowChars++;
          sjisCurKatakanaWordLength = 0;
          sjisCurDoubleBytesWordLength = 0;
        }
      } else {
        CoverageTool2000.setCoverageMatrix(5, 34);
      }
    }

    if (canBeUTF8 && utf8BytesLeft > 0) {
      CoverageTool2000.setCoverageMatrix(5, 35);
      canBeUTF8 = false;
    } else {
      CoverageTool2000.setCoverageMatrix(5, 36);
    }
    if (canBeShiftJIS && sjisBytesLeft > 0) {
      CoverageTool2000.setCoverageMatrix(5, 37);
      canBeShiftJIS = false;
    } else {
      CoverageTool2000.setCoverageMatrix(5, 38);
    }

    // Easy -- if there is BOM or at least 1 valid not-single byte character (and no evidence it can't be UTF-8), done
    if (canBeUTF8 && (utf8bom || utf2BytesChars + utf3BytesChars + utf4BytesChars > 0)) {
      CoverageTool2000.setCoverageMatrix(5, 39);
      return UTF8;
    }
    CoverageTool2000.setCoverageMatrix(5, 40);
    // Easy -- if assuming Shift_JIS or >= 3 valid consecutive not-ascii characters (and no evidence it can't be), done
    if (canBeShiftJIS && (ASSUME_SHIFT_JIS || sjisMaxKatakanaWordLength >= 3 || sjisMaxDoubleBytesWordLength >= 3)) {
      CoverageTool2000.setCoverageMatrix(5, 41);
      return SHIFT_JIS;
    }
    CoverageTool2000.setCoverageMatrix(5, 42);
    // Distinguishing Shift_JIS and ISO-8859-1 can be a little tough for short words. The crude heuristic is:
    // - If we saw
    //   - only two consecutive katakana chars in the whole text, or
    //   - at least 10% of bytes that could be "upper" not-alphanumeric Latin1,
    // - then we conclude Shift_JIS, else ISO-8859-1
    if (canBeISO88591 && canBeShiftJIS) {
      CoverageTool2000.setCoverageMatrix(5, 43);
      return (sjisMaxKatakanaWordLength == 2 && sjisKatakanaChars == 2) || isoHighOther * 10 >= length
          ? SHIFT_JIS : ISO88591;
    }
    CoverageTool2000.setCoverageMatrix(5, 44);

    // Otherwise, try in order ISO-8859-1, Shift JIS, UTF-8 and fall back to default platform encoding
    if (canBeISO88591) {
      CoverageTool2000.setCoverageMatrix(5, 45);
      return ISO88591;
    }
    CoverageTool2000.setCoverageMatrix(5, 46);
    if (canBeShiftJIS) {
      CoverageTool2000.setCoverageMatrix(5, 47);
      return SHIFT_JIS;
    }
    CoverageTool2000.setCoverageMatrix(5, 48);
    if (canBeUTF8) {
      CoverageTool2000.setCoverageMatrix(5, 49);
      return UTF8;
    }
    CoverageTool2000.setCoverageMatrix(5, 50);
    // Otherwise, we take a wild guess with platform encoding
    return PLATFORM_DEFAULT_ENCODING;
  }

}
