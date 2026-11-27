/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.qrcode.decoder;

import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitSource;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.StringUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>QR Codes can encode text as bits in one of several modes, and can use multiple modes
 * in one QR Code. This class decodes the bits back into text.</p>
 *
 * <p>See ISO 18004:2006, 6.4.3 - 6.4.7</p>
 *
 * @author Sean Owen
 */
final class DecodedBitStreamParser {

  /**
   * See ISO 18004:2006, 6.4.4 Table 5
   */
  private static final char[] ALPHANUMERIC_CHARS =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:".toCharArray();
  private static final int GB2312_SUBSET = 1;

  private DecodedBitStreamParser() {
  }

  static DecoderResult decode(byte[] bytes,
                              Version version,
                              ErrorCorrectionLevel ecLevel,
                              Map<DecodeHintType,?> hints) throws FormatException {
    BitSource bits = new BitSource(bytes);
    StringBuilder result = new StringBuilder(50);
    List<byte[]> byteSegments = new ArrayList<>(1);
    int symbolSequence = -1;
    int parityData = -1;
    int symbologyModifier;

    boolean hasFNC1first = false;
    boolean hasFNC1second = false;

    try {
      CharacterSetECI currentCharacterSetECI = null;
      boolean fc1InEffect = false;
      boolean hasDataSegment = false;
      Mode mode;
      do {
        // While still another segment to read...
        if (bits.available() < 4) {
          // Per ISO/IEC 18004 Section 7.4.9, an abbreviated terminator (< 4 remaining bits) must be all zeros
          if (bits.available() > 0 && bits.readBits(bits.available()) != 0) {
            throw FormatException.getFormatInstance();
          }
          mode = Mode.TERMINATOR;
        } else {
          mode = Mode.forBits(bits.readBits(4)); // mode is encoded by 4 bits
        }
        switch (mode) {
          case TERMINATOR:
            break;
          case FNC1_FIRST_POSITION:
            if (fc1InEffect || hasDataSegment) {
              throw FormatException.getFormatInstance();
            }
            hasFNC1first = true; // symbology detection
            // We do little with FNC1 except alter the parsed result a bit according to the spec
            fc1InEffect = true;
            break;
          case FNC1_SECOND_POSITION:
            if (fc1InEffect || hasDataSegment) {
              throw FormatException.getFormatInstance();
            }
            hasFNC1second = true; // symbology detection
            fc1InEffect = true;
            decodeFNC1SecondPositionApplicationIndicator(bits, result);
            break;
          case STRUCTURED_APPEND:
            if (symbolSequence != -1 || currentCharacterSetECI != null || fc1InEffect || hasDataSegment) {
              throw FormatException.getFormatInstance();
            }
            if (bits.available() < 16) {
              throw FormatException.getFormatInstance();
            }
            // sequence number and parity is added later to the result metadata
            // Read next 8 bits (symbol sequence #) and 8 bits (parity data), then continue
            int sequence = bits.readBits(8);
            int position = sequence >>> 4;
            int totalMinusOne = sequence & 0x0F;
            if (totalMinusOne == 0 || position > totalMinusOne) {
              throw FormatException.getFormatInstance();
            }
            symbolSequence = sequence;
            parityData = bits.readBits(8);
            break;
          case ECI:
            // Count doesn't apply to ECI
            int value = parseECIValue(bits);
            currentCharacterSetECI = CharacterSetECI.getCharacterSetECIByValue(value);
            if (currentCharacterSetECI == null) {
              throw FormatException.getFormatInstance();
            }
            break;
          case HANZI:
            // First handle Hanzi mode which does not start with character count
            // Chinese mode contains a sub set indicator right after mode indicator
            int subset = bits.readBits(4);
            if (subset != GB2312_SUBSET) {
              throw FormatException.getFormatInstance();
            }
            int countHanzi = bits.readBits(mode.getCharacterCountBits(version));
            decodeHanziSegment(bits, result, countHanzi);
            hasDataSegment = true;
            break;
          default:
            // "Normal" QR code modes:
            // How many characters will follow, encoded in this mode?
            int count = bits.readBits(mode.getCharacterCountBits(version));
            switch (mode) {
              case NUMERIC:
                decodeNumericSegment(bits, result, count);
                break;
              case ALPHANUMERIC:
                decodeAlphanumericSegment(bits, result, count, fc1InEffect);
                break;
              case BYTE:
                decodeByteSegment(bits, result, count, currentCharacterSetECI, byteSegments, hints);
                break;
              case KANJI:
                decodeKanjiSegment(bits, result, count);
                break;
              default:
                throw FormatException.getFormatInstance();
            }
            hasDataSegment = true;
            break;
        }
      } while (mode != Mode.TERMINATOR);

      if (currentCharacterSetECI != null) {
        if (hasFNC1first) {
          symbologyModifier = 4;
        } else if (hasFNC1second) {
          symbologyModifier = 6;
        } else {
          symbologyModifier = 2;
        }
      } else {
        if (hasFNC1first) {
          symbologyModifier = 3;
        } else if (hasFNC1second) {
          symbologyModifier = 5;
        } else {
          symbologyModifier = 1;
        }
      }

    } catch (IllegalArgumentException iae) {
      // from readBits() calls
      throw FormatException.getFormatInstance();
    }

    String text = result.toString();
    if (hasFNC1first && hints != null && hints.containsKey(DecodeHintType.ASSUME_GS1)) {
      text = "]Q" + symbologyModifier + text;
    }

    return new DecoderResult(bytes,
                             text,
                             byteSegments.isEmpty() ? null : byteSegments,
                             ecLevel == null ? null : ecLevel.toString(),
                             symbolSequence,
                             parityData,
                             symbologyModifier);
  }

  /**
   * See ISO/IEC 18004 Section 7.4.8.3 (FNC1 in second position).
   * Immediately after the 4-bit mode indicator, an 8-bit Application Indicator encodes either a
   * two-digit number (00..99, stored directly as 0..99) or a single ASCII letter (A..Z or a..z,
   * stored as ASCII value + 100).
   */
  private static void decodeFNC1SecondPositionApplicationIndicator(BitSource bits,
                                                                   StringBuilder result) throws FormatException {
    if (bits.available() < 8) {
      throw FormatException.getFormatInstance();
    }
    int appIndicator = bits.readBits(8);
    if (appIndicator < 100) {
      if (appIndicator < 10) {
        result.append('0');
      }
      result.append(appIndicator);
    } else {
      int letter = appIndicator - 100;
      if ((letter >= 'A' && letter <= 'Z') || (letter >= 'a' && letter <= 'z')) {
        result.append((char) letter);
      } else {
        throw FormatException.getFormatInstance();
      }
    }
  }

  /**
   * See specification GBT 18284-2000
   */
  private static void decodeHanziSegment(BitSource bits,
                                         StringBuilder result,
                                         int count) throws FormatException {
    if (StringUtils.GB2312_CHARSET == null) {
      // Not supported without charset support
      throw FormatException.getFormatInstance();
    }
    // Don't crash trying to read more bits than we have available.
    if (count * 13 > bits.available()) {
      throw FormatException.getFormatInstance();
    }

    // Each character will require 2 bytes. Read the characters as 2-byte pairs
    // and decode as GB2312 afterwards
    byte[] buffer = new byte[2 * count];
    int offset = 0;
    while (count > 0) {
      // Each 13 bits encodes a 2-byte character
      int twoBytes = bits.readBits(13);
      int high = twoBytes / 0x060;
      int low = twoBytes % 0x060;
      // GB 2312 code ranges: 0xA1A1..0xAAFE (high 0x00..0x09) and 0xB0A1..0xFAFE (high 0x0A..0x54),
      // with second byte in 0xA1..0xFE (low 0x00..0x5D).
      if (high > 0x54 || low > 0x5D) {
        throw FormatException.getFormatInstance();
      }
      int assembledTwoBytes = (high << 8) | low;
      if (assembledTwoBytes < 0x00A00) {
        // In the 0xA1A1 to 0xAAFE range
        assembledTwoBytes += 0x0A1A1;
      } else {
        // In the 0xB0A1 to 0xFAFE range
        assembledTwoBytes += 0x0A6A1;
      }
      buffer[offset] = (byte) ((assembledTwoBytes >> 8) & 0xFF);
      buffer[offset + 1] = (byte) (assembledTwoBytes & 0xFF);
      offset += 2;
      count--;
    }

    result.append(new String(buffer, StringUtils.GB2312_CHARSET));
  }

  private static void decodeKanjiSegment(BitSource bits,
                                         StringBuilder result,
                                         int count) throws FormatException {
    if (StringUtils.SHIFT_JIS_CHARSET == null) {
      // Not supported without charset support
      throw FormatException.getFormatInstance();
    }
    // Don't crash trying to read more bits than we have available.
    if (count * 13 > bits.available()) {
      throw FormatException.getFormatInstance();
    }

    // Each character will require 2 bytes. Read the characters as 2-byte pairs
    // and decode as Shift_JIS afterwards
    byte[] buffer = new byte[2 * count];
    int offset = 0;
    while (count > 0) {
      // Each 13 bits encodes a 2-byte character
      int twoBytes = bits.readBits(13);
      int assembledTwoBytes = ((twoBytes / 0x0C0) << 8) | (twoBytes % 0x0C0);
      if (assembledTwoBytes < 0x01F00) {
        // In the 0x8140 to 0x9FFC range
        assembledTwoBytes += 0x08140;
      } else {
        // In the 0xE040 to 0xEBBF range
        assembledTwoBytes += 0x0C140;
      }
      int lowByte = assembledTwoBytes & 0xFF;
      // Shift_JIS trail bytes are 0x40..0x7E and 0x80..0xFC (ISO/IEC 18004 Section 7.4.6)
      if (lowByte == 0x7F || lowByte > 0xFC) {
        throw FormatException.getFormatInstance();
      }
      buffer[offset] = (byte) (assembledTwoBytes >> 8);
      buffer[offset + 1] = (byte) lowByte;
      offset += 2;
      count--;
    }
    result.append(new String(buffer, StringUtils.SHIFT_JIS_CHARSET));
  }

  private static void decodeByteSegment(BitSource bits,
                                        StringBuilder result,
                                        int count,
                                        CharacterSetECI currentCharacterSetECI,
                                        Collection<byte[]> byteSegments,
                                        Map<DecodeHintType,?> hints) throws FormatException {
    // Don't crash trying to read more bits than we have available.
    if (8 * count > bits.available()) {
      throw FormatException.getFormatInstance();
    }

    byte[] readBytes = new byte[count];
    for (int i = 0; i < count; i++) {
      readBytes[i] = (byte) bits.readBits(8);
    }
    Charset encoding;
    if (currentCharacterSetECI == null) {
      // The spec isn't clear on this mode; see
      // section 6.4.5: t does not say which encoding to assuming
      // upon decoding. I have seen ISO-8859-1 used as well as
      // Shift_JIS -- without anything like an ECI designator to
      // give a hint.
      encoding = StringUtils.guessCharset(readBytes, hints);
    } else {
      encoding = currentCharacterSetECI.getCharset();
    }
    result.append(new String(readBytes, encoding));
    byteSegments.add(readBytes);
  }

  private static char toAlphaNumericChar(int value) throws FormatException {
    if (value >= ALPHANUMERIC_CHARS.length) {
      throw FormatException.getFormatInstance();
    }
    return ALPHANUMERIC_CHARS[value];
  }

  private static void decodeAlphanumericSegment(BitSource bits,
                                                StringBuilder result,
                                                int count,
                                                boolean fc1InEffect) throws FormatException {
    // Read two characters at a time
    int start = result.length();
    while (count > 1) {
      if (bits.available() < 11) {
        throw FormatException.getFormatInstance();
      }
      int nextTwoCharsBits = bits.readBits(11);
      result.append(toAlphaNumericChar(nextTwoCharsBits / 45));
      result.append(toAlphaNumericChar(nextTwoCharsBits % 45));
      count -= 2;
    }
    if (count == 1) {
      // special case: one character left
      if (bits.available() < 6) {
        throw FormatException.getFormatInstance();
      }
      result.append(toAlphaNumericChar(bits.readBits(6)));
    }
    // See section 6.4.8.1, 6.4.8.2
    if (fc1InEffect) {
      // We need to massage the result a bit if in an FNC1 mode:
      for (int i = start; i < result.length(); i++) {
        if (result.charAt(i) == '%') {
          if (i < result.length() - 1 && result.charAt(i + 1) == '%') {
            // %% is rendered as %
            result.deleteCharAt(i + 1);
          } else {
            // In alpha mode, % should be converted to FNC1 separator 0x1D
            result.setCharAt(i, (char) 0x1D);
          }
        }
      }
    }
  }

  private static void decodeNumericSegment(BitSource bits,
                                           StringBuilder result,
                                           int count) throws FormatException {
    // Read three digits at a time
    while (count >= 3) {
      // Each 10 bits encodes three digits
      if (bits.available() < 10) {
        throw FormatException.getFormatInstance();
      }
      int threeDigitsBits = bits.readBits(10);
      if (threeDigitsBits >= 1000) {
        throw FormatException.getFormatInstance();
      }
      result.append(toAlphaNumericChar(threeDigitsBits / 100));
      result.append(toAlphaNumericChar((threeDigitsBits / 10) % 10));
      result.append(toAlphaNumericChar(threeDigitsBits % 10));
      count -= 3;
    }
    if (count == 2) {
      // Two digits left over to read, encoded in 7 bits
      if (bits.available() < 7) {
        throw FormatException.getFormatInstance();
      }
      int twoDigitsBits = bits.readBits(7);
      if (twoDigitsBits >= 100) {
        throw FormatException.getFormatInstance();
      }
      result.append(toAlphaNumericChar(twoDigitsBits / 10));
      result.append(toAlphaNumericChar(twoDigitsBits % 10));
    } else if (count == 1) {
      // One digit left over to read
      if (bits.available() < 4) {
        throw FormatException.getFormatInstance();
      }
      int digitBits = bits.readBits(4);
      if (digitBits >= 10) {
        throw FormatException.getFormatInstance();
      }
      result.append(toAlphaNumericChar(digitBits));
    }
  }

  private static int parseECIValue(BitSource bits) throws FormatException {
    int firstByte = bits.readBits(8);
    if ((firstByte & 0x80) == 0) {
      // just one byte (0..127)
      return firstByte & 0x7F;
    }
    if ((firstByte & 0xC0) == 0x80) {
      // two bytes (128..16383)
      int secondByte = bits.readBits(8);
      int value = ((firstByte & 0x3F) << 8) | secondByte;
      if (value < 128) {
        throw FormatException.getFormatInstance();
      }
      return value;
    }
    if ((firstByte & 0xE0) == 0xC0) {
      // three bytes (16384..999999)
      int secondThirdBytes = bits.readBits(16);
      int value = ((firstByte & 0x1F) << 16) | secondThirdBytes;
      if (value < 16384 || value > 999999) {
        throw FormatException.getFormatInstance();
      }
      return value;
    }
    throw FormatException.getFormatInstance();
  }

}
