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
	
	    /**
     * <p>Implements decoding of the UPC-E format.</p>
     * <p/>
     * <p><a href="http://www.barcodeisland.com/upce.phtml">This</a> is a great reference for
     * UPC-E information.</p>
     *
     * @author Sean Owen
     */


    public  class UPCEReader extends UPCEANReader
    { 

			import com.google.zxing.common.flexdatatypes.StringBuilder;
			import com.google.zxing.common.flexdatatypes.HashTable;
			import com.google.zxing.common.BitArray;
			import com.google.zxing.ReaderException;
			import com.google.zxing.BarcodeFormat;
			import com.google.zxing.Result;
			import com.google.zxing.BinaryBitmap;
			import com.google.zxing.NotFoundException;


           /**
           * The pattern that marks the middle, and end, of a UPC-E pattern.
           * There is no "second half" to a UPC-E barcode.
           */
          private var MIDDLE_END_PATTERN:Array = [1, 1, 1, 1, 1, 1];

          /**
           * See {@link #L_AND_G_PATTERNS}; these values similarly represent patterns of
           * even-odd parity encodings of digits that imply both the number system (0 or 1)
           * used, and the check digit.
           */
          private static var NUMSYS_AND_CHECK_DIGIT_PATTERNS:Array = [
              [0x38, 0x34, 0x32, 0x31, 0x2C, 0x26, 0x23, 0x2A, 0x29, 0x25],
              [0x07, 0x0B, 0x0D, 0x0E, 0x13, 0x19, 0x1C, 0x15, 0x16, 0x1A]
          ];

          private var decodeMiddleCounters:Array;

		// function decode(image:BinaryBitmap, hints:HashTable=null):Result { return null; }

          public function UPCEReader() {
            decodeMiddleCounters = new Array(4);
          }

          protected override function decodeMiddle( row:BitArray, startRange:Array, result:StringBuilder):int {
            var counters:Array = decodeMiddleCounters;
            counters[0] = 0;
            counters[1] = 0;
            counters[2] = 0;
            counters[3] = 0;
            var end:int = row.getSize();
            var rowOffset:int = startRange[1];

            var lgPatternFound:int = 0;

            for (var x:int = 0; x < 6 && rowOffset < end; x++) {
              var bestMatch:int = decodeDigit(row, counters, rowOffset, L_AND_G_PATTERNS);
              result.Append(String.fromCharCode( ('0').charCodeAt(0) + (bestMatch % 10)));
              for (var i:int = 0; i < counters.length; i++) {
                rowOffset += counters[i];
              }
              if (bestMatch >= 10) {
                lgPatternFound |= 1 << (5 - x);
              }
            }

            determineNumSysAndCheckDigit(result, lgPatternFound);

            return rowOffset;
          }

          public override function decodeEnd(row:BitArray, endStart:int):Array {
            return findGuardPattern(row, endStart, true, MIDDLE_END_PATTERN);
          }

          public override function checkChecksum(s:String):Boolean {
              return super.checkChecksum(convertUPCEtoUPCA(s));
          }

          private static function determineNumSysAndCheckDigit(resultString:StringBuilder , lgPatternFound:int):void
              {

            for (var numSys:int = 0; numSys <= 1; numSys++) {
              for (var d:int = 0; d < 10; d++) {
                if (lgPatternFound == NUMSYS_AND_CHECK_DIGIT_PATTERNS[numSys][d]) {
                  resultString.Insert(0, String.fromCharCode(('0').charCodeAt(0) + numSys));
                  resultString.Append(String.fromCharCode( ('0').charCodeAt(0) + d));
                  return;
                }
              }
            }
            throw new ReaderException("UPCEReader : determineNumSysAndCheckDigit : could not determine numsys");
          }

          public  override function getBarcodeFormat():BarcodeFormat {
            return BarcodeFormat.UPC_E;  
          }

          /**
           * Expands a UPC-E value back into its full, equivalent UPC-A code value.
           *
           * @param upce UPC-E code as string of digits
           * @return equivalent UPC-A code as string of digits
           */
          public static function convertUPCEtoUPCA(upce:String):String {
            var result:StringBuilder = new StringBuilder(12);
            result.Append(upce.substr(0,1));
            var lastChar:String = upce.substr(6,1);//upceChars[5];
            switch (lastChar) {
              case '0':
              case '1':
              case '2':
                result.Append(upce.substr(1,2));//upceChars_String, 0, 2)
                result.Append(lastChar);
                result.Append("0000");
                result.Append(upce.substr(3,3));//upceChars_String, 2, 3);
                break;
              case '3':
                result.Append(upce.substr(1,3));//upceChars_String, 0, 3);
                result.Append("00000");
                result.Append(upce.substr(4,2));//upceChars_String, 3, 2);
                break;
              case '4':
                result.Append(upce.substr(1,4));//upceChars_String, 0, 4);
                result.Append("00000");
                result.Append(upce.substr(5,1));//upceChars[4]);
                break;
              default:
                result.Append(upce.substr(1,5));//upceChars_String, 0, 5);
                result.Append("0000");
                result.Append(lastChar);
                break;
            }
            result.Append(upce.substr(7,1));
            return result.ToString();
          }
    
    
    
    
  /**
   * Attempts to decode a single UPC/EAN-encoded digit.
   *
   * @param row row of black/white values to decode
   * @param counters the counts of runs of observed black/white/black/... values
   * @param rowOffset horizontal offset to start decoding from
   * @param patterns the set of patterns to use to decode -- sometimes different encodings
   * for the digits 0-9 are used, and this indicates the encodings for 0 to 9 that should
   * be used
   * @return horizontal offset of first pixel beyond the decoded digit
   * @throws NotFoundException if digit cannot be decoded
   */
  public static function decodeDigit(row:BitArray , counters:Array, rowOffset:int,patterns:Array):int {
    recordPattern(row, rowOffset, counters);
    var bestVariance:int = MAX_AVG_VARIANCE; // worst variance we'll accept
    var bestMatch:int = -1;
    var max:int = patterns.length;
    for (var i:int = 0; i < max; i++) {
      var pattern:Array = patterns[i];
      var variance:int = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
      if (variance < bestVariance) {
        bestVariance = variance;
        bestMatch = i;
      }
    }
    if (bestMatch >= 0) {
      return bestMatch;
    } else {
      throw NotFoundException.getNotFoundInstance();
    }
  }
  
    }


}