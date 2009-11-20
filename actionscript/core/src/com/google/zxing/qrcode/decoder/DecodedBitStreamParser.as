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
	import com.google.zxing.common.zxingByteArray;
	import flash.utils.ByteArray;
	import com.google.zxing.common.flexdatatypes.ArrayList;
	import com.google.zxing.common.BitSource;
	import com.google.zxing.common.CharacterSetECI;
	import com.google.zxing.common.DecoderResult;
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
        private static const SHIFT_JIS:String = "Shift_JIS";
        private static var   ASSUME_SHIFT_JIS:Boolean=true;
        private static const EUC_JP:String = "EUC_JP";
  		private static const UTF8:String = "UTF8";
  		private static const ISO88591:String = "ISO8859_1";


        public function DecodedBitStreamParser()
        {
        	// var platformDefault:String = System.getProperty("file.encoding");
	    	//ASSUME_SHIFT_JIS = SHIFT_JIS.EqualsIgnoreCase(platformDefault) || EUC_JP.EqualsIgnoreCase(platformDefault);
	    	ASSUME_SHIFT_JIS = true;// BAS : actionscript has no build in method to check the encoding 
 

        }

        public static function decode(bytes:Array, version:Version, ecLevel:ErrorCorrectionLevel):DecoderResult
        {
            var bits:BitSource  = new BitSource(bytes);
            var result:StringBuilder = new StringBuilder();
            var currentCharacterSetECI:CharacterSetECI = null;
    		var fc1InEffect:Boolean = false;
    		var byteSegments:ArrayList = new ArrayList(1);
            var mode:Mode;
            do
            {
                // While still another segment to read...
      			if (bits.available() < 4) 
      			{
        			// OK, assume we're done. Really, a TERMINATOR mode should have been recorded here
        			mode = Mode.TERMINATOR;
      			} else 
      			{
        			try 
        			{
          				mode = Mode.forBits(bits.readBits(4)); // mode is encoded by 4 bits
			        } 
			        catch (e:IllegalArgumentException) 
			        {
			          throw new IllegalArgumentException();
			        }
			    }
                if (!mode.Equals(Mode.TERMINATOR)) 
                {
        			if (mode.Equals(Mode.FNC1_FIRST_POSITION) || mode.Equals(Mode.FNC1_SECOND_POSITION)) 
        			{
			          // We do little with FNC1 except alter the parsed result a bit according to the spec
			          fc1InEffect = true;
			        } 
			        else if (mode.Equals(Mode.STRUCTURED_APPEND)) 
			        {
			          // not really supported; all we do is ignore it
			          // Read next 8 bits (symbol sequence #) and 8 bits (parity data), then continue
			          bits.readBits(16);
			        }
			        else if (mode.Equals(Mode.ECI)) 
			        {
			          // Count doesn't apply to ECI
			          var value:int = parseECIValue(bits);
			          currentCharacterSetECI = CharacterSetECI.getCharacterSetECIByValue(value);
			          if (currentCharacterSetECI == null) 
			          {
			            throw new ReaderException();
          			  }
        			} 
        			else 
        			{
			          // How many characters will follow, encoded in this mode?
			          var count:int = bits.readBits(mode.getCharacterCountBits(version));
			          if (mode.Equals(Mode.NUMERIC)) 
			          {
			            decodeNumericSegment(bits, result, count);
			          } 
			          else if (mode.Equals(Mode.ALPHANUMERIC)) 
			          {
			            decodeAlphanumericSegment(bits, result, count, fc1InEffect);
			          } 
			          else if (mode.Equals(Mode.BYTE)) 
			          {
			            decodeByteSegment(bits, result, count, currentCharacterSetECI, byteSegments);
			          } 
			          else if (mode.Equals(Mode.KANJI)) 
			          {
			            decodeKanjiSegment(bits, result, count);
			          } 
			          else 
			          {
			            throw new ReaderException();
			          }
        			}		
      			}
    		} while (!mode.Equals(Mode.TERMINATOR));

    		return new DecoderResult(bytes, result.toString(), byteSegments.isEmpty() ? null : byteSegments, ecLevel);
 
         }

        private static function decodeKanjiSegment(bits:BitSource,  result:StringBuilder, count:int):void
        {
            // Each character will require 2 bytes. Read the characters as 2-byte pairs
            // and decode as Shift_JIS afterwards
            var buffer:Array = new Array(2 * count);
            var offset:int = 0;
            while (count > 0)
            {
                // Each 13 bits encodes a 2-byte character
                var twoBytes:int = bits.readBits(13);
                var assembledTwoBytes:int = ((twoBytes / 0x0C0) << 8) | (twoBytes % 0x0C0);
                if (assembledTwoBytes < 0x01F00)
                {
                    // In the 0x8140 to 0x9FFC range
                    assembledTwoBytes += 0x08140;
                }
                else
                {
                    // In the 0xE040 to 0xEBBF range
                    assembledTwoBytes += 0x0C140;
                }
                buffer[offset] = assembledTwoBytes >> 8;
                buffer[offset + 1] = assembledTwoBytes;
                offset += 2;
                count--;
            }
            // Shift_JIS may not be supported in some environments:
            try
            {
               // var bytes:Array = SupportClass.ToByteArray(buffer);
               //var bytes:Array = new Array(buffer.length); 
               //for (var i:int=0;i<buffer.length;i++) { bytes[i] = buffer[i]; }
                //UPGRADE_TODO: The differences in the Format  of parameters for constructor 'java.lang.String.String'  may cause compilation errors.  "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1092'"
                //result.Append(System.Text.Encoding.GetEncoding("Shift_JIS").GetString(bytes, 0, bytes.length));
               var ba:ByteArray = new ByteArray();
      		   for (var k:int=0;k<buffer.length;k++) { ba.writeByte(buffer[k]); }
      		   ba.position = 0;
      		   result.Append(ba.readMultiByte(ba.length,"shift-jis")); 
      				
            }
            catch (uee:Error)
            {
                throw new ReaderException("SHIFT_JIS encoding is not supported on this device");
            }
        }

        private static function decodeByteSegment(bits:BitSource ,
                                        result:StringBuilder ,
                                        count:int ,
                                        currentCharacterSetECI:CharacterSetECI ,
                                        byteSegments:ArrayList) :void
        {
            var readBytes:Array = new Array(count);
            if (count << 3 > bits.available())
            {
                throw new ReaderException("Count too large: " + count);
            }
            for (var i:int = 0; i < count; i++)
            {
                readBytes[i] = bits.readBits(8);
            }
            // The spec isn't clear on this mode; see
            // section 6.4.5: t does not say which encoding to assuming
            // upon decoding. I have seen ISO-8859-1 used as well as
            // Shift_JIS -- without anything like an ECI designator to
            // give a hint.
            var encoding:String;
		    if (currentCharacterSetECI == null) 
		    {
		    	// The spec isn't clear on this mode; see
		    	// section 6.4.5: t does not say which encoding to assuming
		    	// upon decoding. I have seen ISO-8859-1 used as well as
		    	// Shift_JIS -- without anything like an ECI designator to
		    	// give a hint.
		      encoding = guessEncoding(readBytes);
		    } 
		    else 
		    {
      			encoding = currentCharacterSetECI.getEncodingName();
    		}
   			
   			try 
   			{
      			//result.Append(new String(readBytes, encoding));
      			// BAS : todo : encoding not supported in AS
      			// convert bytes to string
      			//private static const SHIFT_JIS:String = "Shift_JIS";
        		//private static var   ASSUME_SHIFT_JIS:Boolean=true;
        		//private static const EUC_JP:String = "EUC_JP";
  				//private static const UTF8:String = "UTF8";
  				//private static const ISO88591:String = "ISO8859_1";

      			if ((encoding == DecodedBitStreamParser.SHIFT_JIS) ||
      			   (encoding == DecodedBitStreamParser.EUC_JP) ||
      			   (encoding == DecodedBitStreamParser.UTF8) ||
      			   (encoding == DecodedBitStreamParser.ISO88591))
      			{
      				var ba:ByteArray = new ByteArray();
      				for (var k:int=0;k<readBytes.length;k++) { ba.writeByte(readBytes[k]); }
      				ba.position = 0;
      				if      (encoding == DecodedBitStreamParser.SHIFT_JIS) { result.Append(ba.readMultiByte(ba.length,"shift-jis")); }
      				else if (encoding == DecodedBitStreamParser.EUC_JP)    { result.Append(ba.readMultiByte(ba.length," 	euc-jp")); }
      				else if (encoding == DecodedBitStreamParser.UTF8)      { result.Append(ba.readMultiByte(ba.length,"utf-8")); }
      				else if (encoding == DecodedBitStreamParser.ISO88591) { result.Append(ba.readMultiByte(ba.length,"iso-8859-1")); }
      				
      			}
      			else
      			{
	      			
	      			var text:String ="";
	      			for (var i2:int=0;i2<readBytes.length;i2++)
	      			{
	      				text = text  + String.fromCharCode(readBytes[i2]);
	      			}
	      			result.Append(text);
	      		}
    		} 
    		catch (e:Error) 
    		{
      			throw new ReaderException();
    		}
    		byteSegments.addElement(readBytes);
        }

        private static function decodeAlphanumericSegment(bits:BitSource , result:StringBuilder, count:int, fc1InEffect:Boolean):void
        {
        	var start:int = result.length;
            // Read two characters at a time
            while (count > 1)
            {
                var nextTwoCharsBits:int = bits.readBits(11);
                result.Append(ALPHANUMERIC_CHARS[int(nextTwoCharsBits / 45)]);
                result.Append(ALPHANUMERIC_CHARS[int(nextTwoCharsBits % 45)]);
                count -= 2;
            }
            if (count == 1)
            {
                // special case: one character left
                result.Append(ALPHANUMERIC_CHARS[bits.readBits(6)]);
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

        private static function decodeNumericSegment(bits:BitSource , result:StringBuilder , count:int ):void
        {
            // Read three digits at a time
            while (count >= 3)
            {
                // Each 10 bits encodes three digits
                var threeDigitsBits:int  = bits.readBits(10);
                if (threeDigitsBits >= 1000)
                {
                    throw new ReaderException("Illegal value for 3-digit unit: " + threeDigitsBits);
                }
                result.Append(ALPHANUMERIC_CHARS[int(threeDigitsBits / 100)]);
                result.Append(ALPHANUMERIC_CHARS[int((threeDigitsBits / 10) % 10)]);
                result.Append(ALPHANUMERIC_CHARS[int(threeDigitsBits % 10)]);
                count -= 3;
            }
            if (count == 2)
            {
                // Two digits left over to read, encoded in 7 bits
                var twoDigitsBits:int  = bits.readBits(7);
                if (twoDigitsBits >= 100)
                {
                    throw new ReaderException("Illegal value for 2-digit unit: " + twoDigitsBits);
                }
                result.Append(ALPHANUMERIC_CHARS[int(twoDigitsBits / 10)]);
                result.Append(ALPHANUMERIC_CHARS[int(twoDigitsBits % 10)]);
            }
            else if (count == 1)
            {
                // One digit left over to read
                var digitBits:int = bits.readBits(4);
                if (digitBits >= 10)
                {
                    throw new ReaderException("Illegal value for digit unit: " + digitBits);
                }
                result.Append(ALPHANUMERIC_CHARS[digitBits]);
            }
        }

        private static function guessEncoding(bytes:Array):String
        {
                if (ASSUME_SHIFT_JIS) {
      return SHIFT_JIS;
    }
    
	// Does it start with the UTF-8 byte order mark? then guess it's UTF-8
    if (bytes.length > 3 && bytes[0] == 0xEF && bytes[1] ==  0xBB && bytes[2] == 0xBF) {
      return UTF8;
    }
    // For now, merely tries to distinguish ISO-8859-1, UTF-8 and Shift_JIS,
    // which should be by far the most common encodings. ISO-8859-1
    // should not have bytes in the 0x80 - 0x9F range, while Shift_JIS
    // uses this as a first byte of a two-byte character. If we see this
    // followed by a valid second byte in Shift_JIS, assume it is Shift_JIS.
    // If we see something else in that second byte, we'll make the risky guess
    // that it's UTF-8.
    var length:int = bytes.length;
    var canBeISO88591:Boolean = true;
    var canBeShiftJIS:Boolean = true;
    var maybeDoubleByteCount:int = 0;
    var maybeSingleByteKatakanaCount:int = 0;
    var sawLatin1Supplement:Boolean = false;
    var lastWasPossibleDoubleByteStart:Boolean = false;
    for (var i:int = 0; i < length && (canBeISO88591 || canBeShiftJIS); i++) {
      var value:int = bytes[i] & 0xFF;
      if ((value == 0xC2 || value == 0xC3) && i < length - 1) {
        // This is really a poor hack. The slightly more exotic characters people might want to put in
        // a QR Code, by which I mean the Latin-1 supplement characters (e.g. u-umlaut) have encodings
        // that start with 0xC2 followed by [0xA0,0xBF], or start with 0xC3 followed by [0x80,0xBF].
        var nextValue:int = bytes[i + 1] & 0xFF;
        if (nextValue <= 0xBF && ((value == 0xC2 && nextValue >= 0xA0) || (value == 0xC3 && nextValue >= 0x80))) {
          sawLatin1Supplement = true;
        }
      }
      if (value >= 0x7F && value <= 0x9F) {
        canBeISO88591 = false;
      }
      if (value >= 0xA1 && value <= 0xDF) {
        // count the number of characters that might be a Shift_JIS single-byte Katakana character
        if (!lastWasPossibleDoubleByteStart) {
          maybeSingleByteKatakanaCount++;
        }
      }
      if (!lastWasPossibleDoubleByteStart && ((value >= 0xF0 && value <= 0xFF) || value == 0x80 || value == 0xA0)) {
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
          if (i >= bytes.length - 1) {
            canBeShiftJIS = false;
          } else {
            var nextValue2:int = bytes[i + 1] & 0xFF;
            if (nextValue2 < 0x40 || nextValue2 > 0xFC) {
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
    // Distinguishing Shift_JIS and ISO-8859-1 can be a little tough. The crude heuristic is:
    // - If we saw
    //   - at least three byte that starts a double-byte value (bytes that are rare in ISO-8859-1), or
    //   - over 5% of bytes that could be single-byte Katakana (also rare in ISO-8859-1),
    // - and, saw no sequences that are invalid in Shift_JIS, then we conclude Shift_JIS
    if (canBeShiftJIS && (maybeDoubleByteCount >= 3 || 20 * maybeSingleByteKatakanaCount > length)) {
      return SHIFT_JIS;
    }
    // Otherwise, we default to ISO-8859-1 unless we know it can't be
    if (!sawLatin1Supplement && canBeISO88591) {
      return ISO88591;
    }
    // Otherwise, we take a wild guess with UTF-8
    return UTF8;
  }
  
  private static function parseECIValue(bits:BitSource):int {
    var firstByte:int = bits.readBits(8);
    if ((firstByte & 0x80) == 0) {
      // just one byte
      return firstByte & 0x7F;
    } else if ((firstByte & 0xC0) == 0x80) {
      // two bytes
      var secondByte:int = bits.readBits(8);
      return ((firstByte & 0x3F) << 8) | secondByte;
    } else if ((firstByte & 0xE0) == 0xC0) {
      // three bytes
      var secondThirdBytes:int = bits.readBits(16);
      return ((firstByte & 0x1F) << 16) | secondThirdBytes;
    }
    throw new Error("Bad ECI bits starting with byte " + firstByte);
  }

}
}