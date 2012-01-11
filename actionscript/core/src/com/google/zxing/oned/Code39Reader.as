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
package com.google.zxing.oned
{
   public class Code39Reader extends OneDReader 
    { 
    	
    	import com.google.zxing.common.BitArray;
    	import com.google.zxing.BarcodeFormat;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;
		import com.google.zxing.ResultPoint;
    	import com.google.zxing.common.flexdatatypes.HashTable;
    	import com.google.zxing.common.flexdatatypes.StringBuilder;
    	import com.google.zxing.BinaryBitmap;
    	
          public static  var ALPHABET_STRING:String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";
          public static  var ALPHABET:Array = ALPHABET_STRING.split("");

          /**
           * These represent the encodings of characters, as patterns of wide and narrow bars.
           * The 9 least-significant bits of each int correspond to the pattern of wide and narrow,
           * with 1s representing "wide" and 0s representing narrow.
           */
          public static var CHARACTER_ENCODINGS:Array = [
              0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064, // 0-9
              0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C, // A-J
              0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016, // K-T
              0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094, // U-*
              0x0A8, 0x0A2, 0x08A, 0x02A // $-%
          ];

          public static  var ASTERISK_ENCODING:int = CHARACTER_ENCODINGS[39];

          private  var usingCheckDigit:Boolean;
          private  var extendedMode:Boolean;

          /**
           * Creates a reader that assumes all encoded data is data, and does not treat the 
           * character as a check digit. It will not decoded "extended Code 39" sequences.
           */

          /**
           * Creates a reader that can be configured to check the last character as a check digit.
           * It will not decoded "extended Code 39" sequences.
           *
           * @param usingCheckDigit if true, treat the last data character as a check digit, not
           * data, and verify that the checksum passes.
           */


	//	function decode(image:BinaryBitmap, hints:HashTable=null):Result { return null; }
	
          /**
           * Creates a reader that can be configured to check the last character as a check digit,
           * or optionally attempt to decode "extended Code 39" sequences that are used to encode
           * the full ASCII character set.
           *
           * @param usingCheckDigit if true, treat the last data character as a check digit, not
           * data, and verify that the checksum passes.
           * @param extendedMode if true, will attempt to decode extended Code 39 sequences in the
           * text.
           */
          public function Code39Reader(usingCheckDigit:Boolean=false, extendedMode:Boolean=true):void {
          	if (usingCheckDigit == false)
          	{
	            //usingCheckDigit = false;
	            extendedMode = false;
          		
          	}
          	else
          	{
            	this.usingCheckDigit = usingCheckDigit;
            	if (extendedMode != true)
            	{
            		this.extendedMode = true;//textendedMode;
            	}
            	else
            	{
            		this.extendedMode = false;
            	}
           }
          }

          public override function decodeRow(rowNumber:Object,  row:BitArray, hints:Object):Result {

            var start:Array = findAsteriskPattern(row);
            var nextStart:int = start[1];
            var end:int = row.getSize();

            // Read off white space
            while (nextStart < end && !row._get(nextStart)) {
              nextStart++;
            }

            var result:StringBuilder = new StringBuilder();
            var counters:Array = new Array(9);
            var decodedChar:String;
            var lastStart:int;
            do {
              recordPattern(row, nextStart, counters);
              var pattern:int = toNarrowWidePattern(counters);
              decodedChar = patternToChar(pattern);
              result.Append(decodedChar);
              lastStart = nextStart;
              for (var i2:int = 0; i2 < counters.length; i2++) {
                nextStart += counters[i2];
              }
              // Read off white space
              while (nextStart < end && !row._get(nextStart)) {
                nextStart++;
              }
            } while (decodedChar != '*');

            result.Remove(result.length - 1, 1);  // remove asterisk

            // Look for whitespace after pattern:
            var lastPatternSize:int = 0;
            for (var i:int = 0; i < counters.length; i++) {
              lastPatternSize += counters[i];
            }
            var whiteSpaceAfterEnd:int = nextStart - lastStart - lastPatternSize;
            // If 50% of last pattern size, following last pattern, is not whitespace, fail
            // (but if it's whitespace to the very end of the image, that's OK)
            if (nextStart != end && whiteSpaceAfterEnd / 2 < lastPatternSize) {
              throw new ReaderException("Code39Reader : decodeRow : 50% of last pattern size, following last pattern, is not whitespace");
            }

            if (usingCheckDigit) {
              var max:int = result.length - 1;
              var total:int = 0;
              for (var i3:int = 0; i3 < max; i3++) {
                total += ALPHABET_STRING.indexOf(result.charAt(i3));
              }
             if (result.charAt(max) != ALPHABET[total % 43]) 
             {
                throw new ReaderException("Code39Reader : decodeRow : checkDigit incorrect)");
              }
              result.Remove(max,1);
            }

            var resultString:String = result.toString();
            if (extendedMode) {
              resultString = decodeExtended(resultString);
            }

            if (resultString.length == 0) {
              // Almost surely a false positive
              throw new ReaderException("Code39Reader : decodeRow : no result string");
            }

            var left:Number = Number((start[1] + start[0]) / 2);
            var right:Number = Number( (nextStart + lastStart) / 2);
            return new Result(
                resultString,
                null,
                [new ResultPoint(left, Number( rowNumber)),new ResultPoint(right, Number( rowNumber))],
                BarcodeFormat.CODE_39);

          }

          private static function findAsteriskPattern(row:BitArray):Array {
            var width:int = row.getSize();
            var rowOffset:int = 0;
            while (rowOffset < width) {
              if (row._get(rowOffset)) {
                break;
              }
              rowOffset++;
            }

            var counterPosition:int = 0;
            var counters:Array = new Array(9);
            var patternStart:int = rowOffset;
            var isWhite:Boolean = false;
            var patternLength:int = counters.length;

            for (var i:int = rowOffset; i < width; i++) {
              var pixel:Boolean = row._get(i);
              if (pixel != isWhite) 
              {
   	            counters[counterPosition] = counters[counterPosition]  + 1;
              } 
              else {
                if (counterPosition == patternLength - 1) {
                  try {
                    if (toNarrowWidePattern(counters) == ASTERISK_ENCODING) {
                      // Look for whitespace before start pattern, >= 50% of width of start pattern
                      if (row.isRange(Math.max(0, patternStart - (i - patternStart) / 2), patternStart, false)) 
                      {
                        return [patternStart, i];
                      }
                    }
                  } catch (re:ReaderException ) {
                    // no match, continue
                  }
                  patternStart += counters[0] + counters[1];
                  for (var y:int = 2; y < patternLength; y++) {
                    counters[y - 2] = counters[y];
                  }
                  counters[patternLength - 2] = 0;
                  counters[patternLength - 1] = 0;
                  counterPosition--;
                } else {
                  counterPosition++;
                }
                counters[counterPosition] = 1;
                isWhite= !isWhite;
              }
            }
            throw new ReaderException("Code39Reader : findAsteriskPattern : could not find asterisk pattern");
          }

          private static function toNarrowWidePattern(counters:Array):int {
            var numCounters:int = counters.length;
            var maxNarrowCounter:int = 0;
            var wideCounters:int;
            do {
              var minCounter:int = int.MAX_VALUE;
              for (var i:int = 0; i < numCounters; i++) {
                var counter:int = counters[i];
                if (counter < minCounter && counter > maxNarrowCounter) {
                  minCounter = counter;
                }
              }
              maxNarrowCounter = minCounter;
              wideCounters = 0;
              var totalWideCountersWidth:int = 0;
              var pattern:int = 0;
              for (var i2:int = 0; i2 < numCounters; i2++) 
              {
                var counter3:int = counters[i2];
                if (counters[i2] > maxNarrowCounter) {
                  pattern |= 1 << (numCounters - 1 - i2);
                  wideCounters++;
                  totalWideCountersWidth += counter3;
                }
              }
              if (wideCounters == 3) {
                // Found 3 wide counters, but are they close enough in width?
                // We can perform a cheap, conservative check to see if any individual
                // counter is more than 1.5 times the average:
                for (var i4:int = 0; i4 < numCounters && wideCounters > 0; i4++) {
                  var counter5:int = counters[i4];
                  if (counters[i4] > maxNarrowCounter) {
                    wideCounters--;
                    // totalWideCountersWidth = 3 * average, so this checks if counter >= 3/2 * average
                    if ((counter5 << 1) >= totalWideCountersWidth) {
                      throw new ReaderException("Code39Reader : toNarrowWidePattern : counter NOT >= 3/2 * average");
                    }
                  }
                }
                return pattern;
              }
            } while (wideCounters > 3);
            throw new ReaderException("Code39Reader : toNarrowWidePattern : could not convert pattern");
          }

          private static function patternToChar(pattern:int):String {
            for (var i:int = 0; i < CHARACTER_ENCODINGS.length; i++) {
              if (CHARACTER_ENCODINGS[i] == pattern) {
                return ALPHABET[i];
              }
            }
            throw new ReaderException("Code39Reader : patternToChar : could not convert pattern to char");
          }

          private static function decodeExtended(encoded:String):String {
            var Length:int = encoded.length;
            var decoded:StringBuilder = new StringBuilder(Length);
            for (var i:int = 0; i < Length; i++) {
              var c:String = encoded.substr(i,1);
              if (c == '+' || c == '$' || c == '%' || c == '/') {
                var next:String= encoded.substr(i+1,1);
                var decodedChar:String = '\0';
                switch (c) {
                  case '+':
                    // +A to +Z map to a to z
                    if (next >= 'A' && next <= 'Z') {
                      decodedChar = String.fromCharCode(next.charCodeAt(0) + 32);
                    } else {
                      throw new ReaderException("Code39Reader : decodeExtended : character is not part of the alphabet");
                    }
                    break;
                  case '$':
                    // $A to $Z map to control codes SH to SB
                    if (next >= 'A' && next <= 'Z') {
                      decodedChar = String.fromCharCode(next.charCodeAt(0) - 64);
                    } else {
                      throw new ReaderException("Code39Reader : decodeExtended : character is no control code");
                    }
                    break;
                  case '%':
                    // %A to %E map to control codes ESC to US
                    if (next >= 'A' && next <= 'E') {
                      decodedChar = String.fromCharCode(next.charCodeAt(0) - 38);
                    } else if (next >= 'F' && next <= 'W') {
                      decodedChar = String.fromCharCode(next.charCodeAt(0) - 11);
                    } else {
                      throw new ReaderException("Code39Reader : decodeExtended : character is no control code");
                    }
                    break;
                  case '/':
                    // /A to /O map to ! to , and /Z maps to :
                    if (next >= 'A' && next <= 'O') {
                      decodedChar = String.fromCharCode(next.charCodeAt(0) - 32);
                    } else if (next == 'Z') {
                      decodedChar = ':';
                    } else {
                      throw new ReaderException("Code39Reader : decodeExtended : failed to map escape strings");
                    }
                    break;
                }
                decoded.Append(decodedChar);
                // bump up i again since we read two characters
                i++;
              } else {
                decoded.Append(c);
              }
            }
            return decoded.ToString();
          }
    }
}