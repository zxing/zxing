/*
 * Copyright 2008 ZXing authors
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
package com.google.zxing.datamatrix.decoder
{
	 /**
 * <p>Data Matrix Codes can encode text as bits in one of several modes, and can use multiple modes
 * in one Data Matrix Code. This class decodes the bits back into text.</p>
 *
 * <p>See ISO 16022:2006, 5.2.1 - 5.2.9.2</p>
 *
 * @author bbrown@google.com (Brian Brown)
 * @author Sean Owen
 */

    public class DecodedBitStreamParser
    {
    	import com.google.zxing.common.flexdatatypes.ArrayList;
    	import com.google.zxing.common.BitMatrix;
    	import com.google.zxing.common.BitSource;
    	import com.google.zxing.common.DecoderResult;
    	import com.google.zxing.common.flexdatatypes.StringBuilder;
    	import com.google.zxing.common.zxingByteArray;
    	import com.google.zxing.ReaderException;
    	import com.google.zxing.FormatException;
           /**
           * See ISO 16022:2006, Annex C Table C.1
           * The C40 Basic Character Set (*'s used for placeholders for the shift values)
           */
          private static  var C40_BASIC_SET_CHARS:Array = [
              '*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
              'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
              'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
          ];
          
          private static  var C40_SHIFT2_SET_CHARS:Array = [
            '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.',
            '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_'
	        ];
          
          /**
           * See ISO 16022:2006, Annex C Table C.2
           * The Text Basic Character Set (*'s used for placeholders for the shift values)
           */
          private static  var TEXT_BASIC_SET_CHARS:Array = [
            '*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
          ];
          
          private static  var TEXT_SHIFT3_SET_CHARS:Array = [
            '\'', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
            'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '{', '|', '}', '~', String.fromCharCode(0x127)
          ];
          
          public static const PAD_ENCODE:int = 0;  // Not really an encoding
          public static const ASCII_ENCODE:int = 1;
          public static const C40_ENCODE:int = 2;
          public static const TEXT_ENCODE:int = 3;
          public static const ANSIX12_ENCODE:int = 4;
          public static const EDIFACT_ENCODE:int = 5;
          public static const BASE256_ENCODE:int = 6;

          public function DecodedBitStreamParser() {
          }

          public static function  decode(bytes:Array ):DecoderResult {
            var bits:BitSource  = new BitSource(bytes);
            var result:StringBuilder = new StringBuilder(100);
            var resultTrailer:StringBuilder  = new StringBuilder(0);
            var  byteSegments:ArrayList = new ArrayList(1);
            var mode:int = ASCII_ENCODE;
            do {
              if (mode == ASCII_ENCODE) {
                mode = decodeAsciiSegment(bits, result, resultTrailer);
              } else {
                switch (mode) {
                  case C40_ENCODE:
                    decodeC40Segment(bits, result);
                    break;
                  case TEXT_ENCODE:
                    decodeTextSegment(bits, result);
                    break;
                  case ANSIX12_ENCODE:
                    decodeAnsiX12Segment(bits, result);
                    break;
                  case EDIFACT_ENCODE:
                    decodeEdifactSegment(bits, result);
                    break;
                  case BASE256_ENCODE:
                    decodeBase256Segment(bits, result, byteSegments);
                    break;
                  default:
                    throw new ReaderException("DecodedBitStreamParser : decode : unknown mode : "+mode);
                }
                mode = ASCII_ENCODE;
              }
            } while (mode != PAD_ENCODE && bits.available() > 0);
            if (resultTrailer.length > 0) {
              result.Append(resultTrailer);
            }
            return new DecoderResult(bytes, result.ToString(), (byteSegments.Count === 0) ? null : byteSegments, null);
          }
          
          /**
           * See ISO 16022:2006, 5.2.3 and Annex C, Table C.2
           */
          private static function decodeAsciiSegment(bits:BitSource, result:StringBuilder, resultTrailer:StringBuilder):int
              {
            var upperShift:Boolean = false;
            do {
              var oneByte:int = bits.readBits(8);
              if (oneByte == 0) {
	    	        throw new ReaderException("DecodedBitStreamParser : decodeAsciiSegment : oneByte = 0");
	            } else if (oneByte <= 128) {  // ASCII data (ASCII value + 1)
	    	        oneByte = upperShift ? (oneByte + 128) : oneByte;
	    	        upperShift = false;
	    	        result.Append(String.fromCharCode(oneByte - 1));
	    	        return ASCII_ENCODE;
	            } else if (oneByte == 129) {  // Pad
	    	        return PAD_ENCODE;
	            } else if (oneByte <= 229) {  // 2-digit data 00-99 (Numeric Value + 130)
	              var value:int = oneByte - 130;
	              if (value < 10) { // padd with '0' for single digit values
	                result.Append('0');
	              }
	    	        result.Append(value);
	            } else if (oneByte == 230) {  // Latch to C40 encodation
	    	        return C40_ENCODE;
	            } else if (oneByte == 231) {  // Latch to Base 256 encodation
	    	        return BASE256_ENCODE;
	            } else if (oneByte == 232) {  
					// FNC1
	    	        String.fromCharCode(29);// translate as ASCII 29
	            } else if ((oneByte == 233) || (oneByte == 234)) 
				{   // Structured Append, Reader Programming
					// Ignore these symbols for now
					// throw new ReaderException("DecodedBitStreamParser : decodeAsciiSegment : oneByte = 234");
	            } else if (oneByte == 235) {  // Upper Shift (shift to Extended ASCII)
	    	        upperShift = true;
	            } else if (oneByte == 236) {  // 05 Macro
                result.Append("[)>\u001E05\u001D");
                resultTrailer.Insert(0, "\u001E\u0004");
              } else if (oneByte == 237) {  // 06 Macro
	    	        result.Append("[)>\u001E06\u001D");
                resultTrailer.Insert(0, "\u001E\u0004");
	            } else if (oneByte == 238) {  // Latch to ANSI X12 encodation
	    	        return ANSIX12_ENCODE;
	            } else if (oneByte == 239) {  // Latch to Text encodation
	    	        return TEXT_ENCODE;
	            } else if (oneByte == 240) {  // Latch to EDIFACT encodation
	    	        return EDIFACT_ENCODE;
	            } else if (oneByte == 241) {  // ECI Character
					// TODO(bbrown): I think we need to support ECI
					//throw ReaderException.getInstance();
					// Ignore this symbol for now
	            } else if (oneByte >= 242) {  // Not to be used in ASCII encodation
					// ... but work around encoders that end with 254, latch back to ASCII
					if (oneByte == 254 && bits.available() == 0) {
					// Ignore
					} else {
						throw FormatException.getFormatInstance();
				}
      }
            } while (bits.available() > 0);
            return ASCII_ENCODE;
          }

          /**
           * See ISO 16022:2006, 5.2.5 and Annex C, Table C.1
           */
          private static function decodeC40Segment(bits:BitSource , result:StringBuilder ):void {
            // Three C40 values are encoded in a 16-bit value as
            // (1600 * C1) + (40 * C2) + C3 + 1
            // TODO(bbrown): The Upper Shift with C40 doesn't work in the 4 value scenario all the time
            var upperShift:Boolean = false;

            var cValues:Array = new Array(3);
            do {
              // If there is only one byte left then it will be encoded as ASCII
              if (bits.available() == 8) {
                return;
              }
              var firstByte:int = bits.readBits(8);
              if (firstByte == 254) {  // Unlatch codeword
                return;
              }

              parseTwoBytes(firstByte, bits.readBits(8), cValues);

              var shift:int = 0;
              for (var i:int = 0; i < 3; i++) {
                var cValue:int = cValues[i];
                switch (shift) {
                  case 0:
                    if (cValue < 3) {
                      shift = cValue + 1;
                    } else {
                      if (upperShift) {
                        result.Append(C40_BASIC_SET_CHARS[cValue] + 128);
                        upperShift = false;
                      } else {
                        result.Append(C40_BASIC_SET_CHARS[cValue]);
                      }
                    }
                    break;
                  case 1:
                    if (upperShift) {
                      result.Append(cValue + 128);
                      upperShift = false;
                    } else {
                      result.Append(cValue);
                    }
                    shift = 0;
                    break;
                  case 2:
                    if (cValue < 27) {
                      if (upperShift) {
                        result.Append(C40_SHIFT2_SET_CHARS[cValue] + 128);
                        upperShift = false;
                      } else {
                        result.Append(C40_SHIFT2_SET_CHARS[cValue]);
                      }
                    } else if (cValue == 27) {  // FNC1
                      result.Append(String.fromCharCode(29)); // translate as ASCII 29
                    } else if (cValue == 30) {  // Upper Shift
                      upperShift = true;
                    } else {
                      throw new ReaderException("DecodedBitStreamParser : decodeC40Segment : cValue = no match:"+cValue);
                    }
                    shift = 0;
                    break;
                  case 3:
                    if (upperShift) {
                      result.Append(cValue + 224);
                      upperShift = false;
                    } else {
                      result.Append(cValue + 96);
                    }
                    shift = 0;
                    break;
                  default:
                    throw new ReaderException("DecodedBitStreamParser : decodeC40Segment : no match for shift:"+shift);
                }
              }
            } while (bits.available() > 0);
          }
          
          /**
           * See ISO 16022:2006, 5.2.6 and Annex C, Table C.2
           */
          private static function decodeTextSegment(bits:BitSource , result:StringBuilder ):void {
            // Three Text values are encoded in a 16-bit value as
            // (1600 * C1) + (40 * C2) + C3 + 1
            // TODO(bbrown): The Upper Shift with Text doesn't work in the 4 value scenario all the time
            var upperShift:Boolean = false;

            var cValues:Array = new Array(3);
            do {
              // If there is only one byte left then it will be encoded as ASCII
              if (bits.available() == 8) {
                return;
              }
              var firstByte:int = bits.readBits(8);
              if (firstByte == 254) {  // Unlatch codeword
                return;
              }

              parseTwoBytes(firstByte, bits.readBits(8), cValues);

              var shift:int = 0;
              for (var i:int = 0; i < 3; i++) {
                var cValue:int = cValues[i];
                switch (shift) {
                  case 0:
							 if (cValue < 3) {
								shift = cValue + 1;
								} else if (cValue < TEXT_BASIC_SET_CHARS.length) {
									var textChar:String = TEXT_BASIC_SET_CHARS[cValue];
								if (upperShift) {
								result.Append(String.fromCharCode(textChar.charCodeAt(0) + 128));
								upperShift = false;
							} else {
								result.Append(textChar);
							}
								} else {
								throw FormatException.getFormatInstance();
							}
						break;
                  case 1:
                    if (upperShift) {
                      result.Append(cValue + 128);
                      upperShift = false;
                    } else {
                      result.Append(cValue);
                    }
                    shift = 0;
                    break;
                  case 2:
                    // Shift 2 for Text is the same encoding as C40
                    if (cValue < C40_SHIFT2_SET_CHARS.length) {
						var c40char:String =C40_SHIFT2_SET_CHARS[cValue]; 
                      if (upperShift) {
                        result.Append(c40char + 128);
                        upperShift = false;
                      } else {
                        result.Append(c40char);
                      }
                    } else if (cValue == 27) {  // FNC1
                      result.Append(String.fromCharCode(29)); // translate as ASCII 29
                    } else if (cValue == 30) {  // Upper Shift
                      upperShift = true;
                    } else {
                      throw new ReaderException("DecodedBitStreamParser : decodeTextSegment : no match for cValue:"+cValue);
                    }
                    shift = 0;
                    break;
                  case 3:
				  if (cValue < TEXT_SHIFT3_SET_CHARS.length) 
				  {
					var textChar2:String = TEXT_SHIFT3_SET_CHARS[cValue];
                    if (upperShift) {
                      result.Append(textChar2 + 128);
                      upperShift = false;
                    } else {
                      result.Append(textChar2);
                    }
                    shift = 0;
                    
					}
					else
					{
						throw new ReaderException();
					}
					break;
                  default:
                    throw new ReaderException("DecodedBitStreamParser : decodeTextSegment : no match for shift"+shift);
                }
              }
            } while (bits.available() > 0);
          }
          
          /**
           * See ISO 16022:2006, 5.2.7
           */
          private static function decodeAnsiX12Segment(bits:BitSource, result:StringBuilder):void {
            // Three ANSI X12 values are encoded in a 16-bit value as
            // (1600 * C1) + (40 * C2) + C3 + 1

            var cValues:Array = new Array(3);
            do {
              // If there is only one byte left then it will be encoded as ASCII
              if (bits.available() == 8) {
                return;
              }
              var firstByte:int = bits.readBits(8);
              if (firstByte == 254) {  // Unlatch codeword
                return;
              }

              parseTwoBytes(firstByte, bits.readBits(8), cValues);

              for (var i:int = 0; i < 3; i++) {
                var cValue:int = cValues[i];
                if (cValue == 0) {  // X12 segment terminator <CR>
                  result.Append('\r');
                } else if (cValue == 1) {  // X12 segment separator *
                  result.Append('*');
                } else if (cValue == 2) {  // X12 sub-element separator >
                  result.Append('>');
                } else if (cValue == 3) {  // space
                  result.Append(' ');
                } else if (cValue < 14) {  // 0 - 9
                  result.Append(cValue + 44);
                } else if (cValue < 40) {  // A - Z
                  result.Append(cValue + 51);
                } else {
                  throw new ReaderException("DecodedBitStreamParser : decodeTextSegment : no match for cValue : "+ cValue);
                }
              }
            } while (bits.available() > 0);
          }

          private static function parseTwoBytes(firstByte:int, secondByte:int, result:Array):void {
            var fullBitValue:int = (firstByte << 8) + secondByte - 1;
            var temp:int = fullBitValue / 1600;
            result[0] = temp;
            fullBitValue -= temp * 1600;
            temp = fullBitValue / 40;
            result[1] = temp;
            result[2] = fullBitValue - temp * 40;
          }
          
          /**
           * See ISO 16022:2006, 5.2.8 and Annex C Table C.3
           */
          private static function decodeEdifactSegment(bits:BitSource, result:StringBuilder):void {
            var unlatch:Boolean = false;
            do {
              // If there is only two or less bytes left then it will be encoded as ASCII
              if (bits.available() <= 16) {
                return;
              }

              for (var i:int = 0; i < 4; i++) {
                var edifactValue:int = bits.readBits(6);

                // Check for the unlatch character
                if (edifactValue == 0x2B67) {  // 011111
                  unlatch = true;
                  // If we encounter the unlatch code then continue reading because the Codeword triple
                  // is padded with 0's
                }
                
                if (!unlatch) {
                  if ((edifactValue & 0x20) == 0) {  // no 1 in the leading (6th) bit
                    edifactValue |= 0x40;  // Add a leading 01 to the 6 bit binary value
                  }
                  result.Append(edifactValue);
                }
              }
            } while (!unlatch && bits.available() > 0);
          }
          
          /**
           * See ISO 16022:2006, 5.2.9 and Annex B, B.2
           */
          private static function decodeBase256Segment(bits:BitSource, result:StringBuilder , byteSegments:ArrayList ):void 
		  {
            // Figure out how long the Base 256 Segment is.
       // 	Figure out how long the Base 256 Segment is.
			var codewordPosition:int = 1 + bits.getByteOffset(); // position is 1-indexed
			var d1:int = unrandomize255State(bits.readBits(8), codewordPosition++);
			var count:int;
			if (d1 == 0) 
			{  // Read the remainder of the symbol
				count = bits.available() / 8;
			} 
			else if (d1 < 250) 
			{
				count = d1;
			} 
			else 
			{
				count = 250 * (d1 - 249) + unrandomize255State(bits.readBits(8), codewordPosition++);
			}

			// We're seeing NegativeArraySizeException errors from users.
			if (count < 0) 
			{
				throw FormatException.getFormatInstance();
			}

			var bytes:Array = new Array(count);
			for (var i:int = 0; i < count; i++) 
			{
				// Have seen this particular error in the wild, such as at
				// http://www.bcgen.com/demo/IDAutomationStreamingDataMatrix.aspx?MODE=3&D=Fred&PFMT=3&PT=F&X=0.3&O=0&LM=0.2
				if (bits.available() < 8) 
				{
					throw FormatException.getFormatInstance();
				}
				bytes[i] = unrandomize255State(bits.readBits(8), codewordPosition++);
			}
			byteSegments.addElement(bytes);
			/*try 
			{
					result.append(new String(bytes, "ISO8859_1"));
			} catch (UnsupportedEncodingException uee) 
			{
				throw new RuntimeException("Platform does not support required encoding: " + uee);
			}*/
			for (var k:int=0;k<bytes.length;k++)
			{
				result.Append(String.fromCharCode(bytes[k])); // BAS :Flex does not support encodings
			}
          }
          
          /**
           * See ISO 16022:2006, Annex B, B.2
           */
          private static function unrandomize255State(randomizedBase256Codeword:int,
                                                  base256CodewordPosition:int ):int {
            var pseudoRandomNumber:int = ((149 * base256CodewordPosition) % 255) + 1;
            var tempVariable:int = randomizedBase256Codeword - pseudoRandomNumber;
            var result:int = (tempVariable >= 0 ? tempVariable : (tempVariable + 256)); 
            return result; 
          }
    }

}