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
package com.google.zxing.qrcode.decoder
{
	import com.google.zxing.FormatException;
	import com.google.zxing.common.BitSource;
	import com.google.zxing.common.CharacterSetECI;
	import com.google.zxing.common.DecoderResult;
	import com.google.zxing.common.flexdatatypes.ArrayList;
	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.common.flexdatatypes.StringBuilder;


	
	/// <summary> <p>QR Codes can encode text as bits in one of several modes, and can use multiple modes
    /// in one QR Code. This class decodes the bits back into text.</p>
    /// 
    /// <p>See ISO 18004:2006, 6.4.3 - 6.4.7</p>
    /// 
    /// </summary>
    /// <author>  srowen@google.com (Sean Owen)
    /// </author>
    public class DecodedBitStreamParser
    {
		import com.google.zxing.common.BitSource;
		import com.google.zxing.common.flexdatatypes.StringBuilder;
		import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
		import com.google.zxing.ReaderException;
		import com.google.zxing.common.CharacterSetECI;
		import com.google.zxing.common.DecoderResult;
		import com.google.zxing.common.flexdatatypes.ArrayList;

	/**
 * <p>QR Codes can encode text as bits in one of several modes, and can use multiple modes
 * in one QR Code. This class decodes the bits back into text.</p>
 *
 * <p>See ISO 18004:2006, 6.4.3 - 6.4.7</p>
 *
 * @author Sean Owen
 */
      private static var   ALPHANUMERIC_CHARS:Array = [ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':' ];
  		private static const GB2312_SUBSET:int = 1;


        public function DecodedBitStreamParser()
        {
          }


 public static function decode(bytes:Array, version:Version, ecLevel:ErrorCorrectionLevel, hints:HashTable):DecoderResult
  {
    var bits:BitSource = new BitSource(bytes);
    var result:StringBuilder  = new StringBuilder(50);
    var currentCharacterSetECI:CharacterSetECI = null;
    var fc1InEffect:Boolean = false;
    var byteSegments:ArrayList = new ArrayList(1);
    var mode:Mode;
    do {
      // While still another segment to read...
      if (bits.available() < 4) {
        // OK, assume we're done. Really, a TERMINATOR mode should have been recorded here
        mode = Mode.TERMINATOR;
      } else {
        try {
          mode = Mode.forBits(bits.readBits(4)); // mode is encoded by 4 bits
        } catch (iae:IllegalArgumentException) {
          throw FormatException.getFormatInstance();
        }
      }
      if (!mode.Equals(Mode.TERMINATOR)) {
        if (mode.Equals(Mode.FNC1_FIRST_POSITION) || mode.Equals(Mode.FNC1_SECOND_POSITION)) {
          // We do little with FNC1 except alter the parsed result a bit according to the spec
          fc1InEffect = true;
        } else if (mode.Equals(Mode.STRUCTURED_APPEND)) {
          // not really supported; all we do is ignore it
          // Read next 8 bits (symbol sequence #) and 8 bits (parity data), then continue
          bits.readBits(16);
        } else if (mode.Equals(Mode.ECI)) {
          // Count doesn't apply to ECI
          var value:int = parseECIValue(bits);
          currentCharacterSetECI = CharacterSetECI.getCharacterSetECIByValue(value);
          if (currentCharacterSetECI == null) {
            throw FormatException.getFormatInstance();
          }
        } else {
          // First handle Hanzi mode which does not start with character count
          if (mode.Equals(Mode.HANZI)) {
                        //chinese mode contains a sub set indicator right after mode indicator
                        var subset:int = bits.readBits(4);
                        var countHanzi:int = bits.readBits(mode.getCharacterCountBits(version));
                        if (subset == GB2312_SUBSET) {
                                decodeHanziSegment(bits, result, countHanzi);
            }
          } else {
            // "Normal" QR code modes:
            // How many characters will follow, encoded in this mode?
            var count:int = bits.readBits(mode.getCharacterCountBits(version));
            if (mode.Equals(Mode.NUMERIC)) {
              decodeNumericSegment(bits, result, count);
            } else if (mode.Equals(Mode.ALPHANUMERIC)) {
              decodeAlphanumericSegment(bits, result, count, fc1InEffect);
            } else if (mode.Equals(Mode.BYTE)) {
              decodeByteSegment(bits, result, count, currentCharacterSetECI, byteSegments, hints);
            } else if (mode.Equals(Mode.KANJI)) {
              decodeKanjiSegment(bits, result, count);
            } else {
              throw FormatException.getFormatInstance();
            }
          }
        }
      }
    } while (!mode.Equals(Mode.TERMINATOR));

    return new DecoderResult(bytes,
                             result.toString(),
                             byteSegments.isEmpty() ? null : byteSegments,
                             ecLevel == null ? null : ecLevel.toString());
  }


  /**
   * See specification GBT 18284-2000
   */
  private static function decodeHanziSegment(bits:BitSource,
                                          result:StringBuilder,
                                         count:int):void {
    // Don't crash trying to read more bits than we have available.
    if (count * 13 > bits.available()) {
      throw FormatException.getFormatInstance();
    }

    // Each character will require 2 bytes. Read the characters as 2-byte pairs
    // and decode as GB2312 afterwards
    var buffer:Array = new Array(2 * count);
    var offset:int = 0;
    while (count > 0) {
      // Each 13 bits encodes a 2-byte character
      var twoBytes:int = bits.readBits(13);
      var assembledTwoBytes:int = (int(twoBytes / 0x060) << 8) | int(twoBytes % 0x060);
      if (assembledTwoBytes < 0x003BF) {
        // In the 0xA1A1 to 0xAAFE range
        assembledTwoBytes += 0x0A1A1;
      } else {
        // In the 0xB0A1 to 0xFAFE range
        assembledTwoBytes += 0x0A6A1;
      }
      buffer[offset] = int(((assembledTwoBytes >> 8) & 0xFF));
      buffer[offset + 1] = int(assembledTwoBytes & 0xFF);
      offset += 2;
      count--;
    }

    //try {
      //result.Append(new String(buffer, StringUtils.GB2312));
      result.Append(buffer);
    //} catch (uee:UnsupportedEncodingException) {
     // throw FormatException.getFormatInstance();
   // }
  }

  private static function decodeKanjiSegment(bits:BitSource,
                                         result:StringBuilder,
                                         count:int):void {
    // Don't crash trying to read more bits than we have available.
    if (count * 13 > bits.available()) {
      throw FormatException.getFormatInstance();
    }

    // Each character will require 2 bytes. Read the characters as 2-byte pairs
    // and decode as Shift_JIS afterwards
    var buffer:Array = new Array(2 * count);
    var offset:int = 0;
    while (count > 0) {
      // Each 13 bits encodes a 2-byte character
      var twoBytes:int = bits.readBits(13);
      var assembledTwoBytes:int = (int(twoBytes / 0x0C0) << 8) | int(twoBytes % 0x0C0);
      if (assembledTwoBytes < 0x01F00) {
        // In the 0x8140 to 0x9FFC range
        assembledTwoBytes += 0x08140;
      } else {
        // In the 0xE040 to 0xEBBF range
        assembledTwoBytes += 0x0C140;
      }
      buffer[offset] = int(assembledTwoBytes >> 8);
      buffer[offset + 1] = int( assembledTwoBytes);
      offset += 2;
      count--;
    }
    // Shift_JIS may not be supported in some environments:
    //try {
      ///result.Append(new String(buffer, StringUtils.SHIFT_JIS));
      result.Append(buffer);
    //} catch (uee:UnsupportedEncodingException) {
    //  throw FormatException.getFormatInstance();
    //}
  }

  private static function decodeByteSegment(bits:BitSource,
                                         result:StringBuilder,
                                        count:int,
                                        currentCharacterSetECI:CharacterSetECI,
                                        byteSegments:ArrayList,
                                        hints:HashTable):void {
    // Don't crash trying to read more bits than we have available.
    if (count << 3 > bits.available()) {
      throw FormatException.getFormatInstance();
    }

    var readBytes:Array = new Array(count);
    for (var i:int = 0; i < count; i++) {
      readBytes[i] = String.fromCharCode(bits.readBits(8));
    }
    var encoding:String;
    //if (currentCharacterSetECI == null) {
    // The spec isn't clear on this mode; see
    // section 6.4.5: t does not say which encoding to assuming
    // upon decoding. I have seen ISO-8859-1 used as well as
    // Shift_JIS -- without anything like an ECI designator to
    // give a hint.
      //encoding = StringUtils.guessEncoding(readBytes, hints);
    //} else {
     // encoding = currentCharacterSetECI.getEncodingName();
   // }
    //try {
      //result.Append(new String(readBytes, encoding));
 
      result.Append(readBytes);
    //} catch (uce:UnsupportedEncodingException) {
    //  throw FormatException.getFormatInstance();
    //}
    byteSegments.addElement(readBytes);
  }

  private static function toAlphaNumericChar(value:int):String {
    if (value >= ALPHANUMERIC_CHARS.length) {
      throw FormatException.getFormatInstance();
    }
    return ALPHANUMERIC_CHARS[value];
  }

  private static function decodeAlphanumericSegment(bits:BitSource,
                                                result:StringBuilder,
                                                count:int,
                                                fc1InEffect:Boolean):void {
    // Read two characters at a time
    var start:int = result.length;
    while (count > 1) {
      var nextTwoCharsBits:int = bits.readBits(11);
      result.Append(toAlphaNumericChar(int(nextTwoCharsBits / 45)));
      result.Append(toAlphaNumericChar(int(nextTwoCharsBits % 45)));
      count -= 2;
    }
    if (count == 1) {
      // special case: one character left
      result.Append(toAlphaNumericChar(bits.readBits(6)));
    }
    // See section 6.4.8.1, 6.4.8.2
    if (fc1InEffect) {
      // We need to massage the result a bit if in an FNC1 mode:
      for (var i:int = start; i < result.length; i++) {
        if (result.charAt(i) == '%') {
          if (i < result.length - 1 && result.charAt(i + 1) == '%') {
            // %% is rendered as %
            result.deleteCharAt(i + 1);
          } else {
            // In alpha mode, % should be converted to FNC1 separator 0x1D
            result.setCharAt(i, String.fromCharCode(0x1D));
          }
        }
      }
    }
  }

  private static function decodeNumericSegment(bits:BitSource,
                                           result:StringBuilder,
                                           count:int):void {
    // Read three digits at a time
    while (count >= 3) {
      // Each 10 bits encodes three digits
      if (bits.available() < 10) {
        throw FormatException.getFormatInstance();
      }
      var threeDigitsBits:int = bits.readBits(10);
      if (threeDigitsBits >= 1000) {
        throw FormatException.getFormatInstance();
      }
      result.Append(toAlphaNumericChar(int(threeDigitsBits / 100)));
      result.Append(toAlphaNumericChar(int(int(threeDigitsBits / 10) % 10)));
      result.Append(toAlphaNumericChar(int(threeDigitsBits % 10)));
      count -= 3;
    }
    if (count == 2) {
      // Two digits left over to read, encoded in 7 bits
      if (bits.available() < 7) {
        throw FormatException.getFormatInstance();
      }
      var twoDigitsBits:int = bits.readBits(7);
      if (twoDigitsBits >= 100) {
        throw FormatException.getFormatInstance();
      }
      result.Append(toAlphaNumericChar(int(twoDigitsBits / 10)));
      result.Append(toAlphaNumericChar(int(twoDigitsBits % 10)));
    } else if (count == 1) {
      // One digit left over to read
      if (bits.available() < 4) {
        throw FormatException.getFormatInstance();
      }
      var digitBits:int = bits.readBits(4);
      if (digitBits >= 10) {
        throw FormatException.getFormatInstance();
      }
      result.Append(toAlphaNumericChar(digitBits));
    }
  }

  private static function parseECIValue(bits:BitSource):int {
    var firstByte:int = bits.readBits(8);
    if ((firstByte & 0x80) == 0) {
      // just one byte
      return firstByte & 0x7F;
    }
    if ((firstByte & 0xC0) == 0x80) {
      // two bytes
      var secondByte:int = bits.readBits(8);
      return ((firstByte & 0x3F) << 8) | secondByte;
    }
    if ((firstByte & 0xE0) == 0xC0) {
      // three bytes
      var secondThirdBytes:int = bits.readBits(16);
      return ((firstByte & 0x1F) << 16) | secondThirdBytes;
    }
    throw new IllegalArgumentException("Bad ECI bits starting with byte " + firstByte);
  }

}
}