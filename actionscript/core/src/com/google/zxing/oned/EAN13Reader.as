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
	
	 public class EAN13Reader extends UPCEANReader 
    {
		import com.google.zxing.common.flexdatatypes.StringBuilder;
		import com.google.zxing.ReaderException;
		import com.google.zxing.BarcodeFormat;
		import com.google.zxing.common.BitArray;

          // For an EAN-13 barcode, the first digit is represented by the parities used
          // to encode the next six digits, according to the table below. For example,
          // if the barcode is 5 123456 789012 then the value of the first digit is
          // signified by using odd for '1', even for '2', even for '3', odd for '4',
          // odd for '5', and even for '6'. See http://en.wikipedia.org/wiki/EAN-13
          //
          //                Parity of next 6 digits
          //    Digit   0     1     2     3     4     5
          //       0    Odd   Odd   Odd   Odd   Odd   Odd
          //       1    Odd   Odd   Even  Odd   Even  Even
          //       2    Odd   Odd   Even  Even  Odd   Even
          //       3    Odd   Odd   Even  Even  Even  Odd
          //       4    Odd   Even  Odd   Odd   Even  Even
          //       5    Odd   Even  Even  Odd   Odd   Even
          //       6    Odd   Even  Even  Even  Odd   Odd
          //       7    Odd   Even  Odd   Even  Odd   Even
          //       8    Odd   Even  Odd   Even  Even  Odd
          //       9    Odd   Even  Even  Odd   Even  Odd
          //
          // Note that the encoding for '0' uses the same parity as a UPC barcode. Hence
          // a UPC barcode can be converted to an EAN-13 barcode by prepending a 0.
          //
          // The encodong is represented by the following array, which is a bit pattern
          // using Odd = 0 and Even = 1. For example, 5 is represented by:
          //
          //              Odd Even Even Odd Odd Even
          // in binary:
          //                0    1    1   0   0    1   == 0x19
          //
          public static var FIRST_DIGIT_ENCODINGS:Array = [0x00, 0x0B, 0x0D, 0xE, 0x13, 0x19, 0x1C, 0x15, 0x16, 0x1A];

          private  var decodeMiddleCounters:Array;

          public function EAN13Reader() 
          {
            decodeMiddleCounters = new Array(4);
          }

          protected override function decodeMiddle(row:BitArray, startRange:Array,  resultString:StringBuilder):int {
            var counters:Array = decodeMiddleCounters;
            counters[0] = 0;
            counters[1] = 0;
            counters[2] = 0;
            counters[3] = 0;
            var end:int = row.getSize();
            var rowOffset:int = startRange[1];

            var lgPatternFound:int = 0;
            for (var x2:int = 0; (x2 < 6) && (rowOffset < end); x2++) {
              var bestMatch2:int = decodeDigit(row, counters, rowOffset, L_AND_G_PATTERNS);
              resultString.Append(String.fromCharCode(('0').charCodeAt(0) + (bestMatch2 % 10)));
              for (var i:int = 0; i < counters.length; i++) {
                rowOffset += counters[i];
              }
              if (bestMatch2 >= 10) {
                lgPatternFound |= 1 << (5 - x2);
              }
            }

            determineFirstDigit(resultString, lgPatternFound);

            var middleRange:Array = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
            rowOffset = middleRange[1];

            for (var x:int = 0; (x < 6) && (rowOffset) < end; x++) 
            {
              var bestMatch:int = decodeDigit(row, counters, rowOffset, L_PATTERNS);
              resultString.Append(String.fromCharCode(('0').charCodeAt(0) + bestMatch));
              for (var i2:int = 0; i2 < counters.length; i2++) 
              {
                rowOffset += counters[i2];
              }
            }

            return rowOffset;
          }

          public  override function  getBarcodeFormat():BarcodeFormat {
            return BarcodeFormat.EAN_13;
          }

          /**
           * Based on pattern of odd-even ('L' and 'G') patterns used to encoded the explicitly-encoded digits
           * in a barcode, determines the implicitly encoded first digit and adds it to the result string.
           *
           * @param resultString string to insert decoded first digit into
           * @param lgPatternFound int whose bits indicates the pattern of odd/even L/G patterns used to
           * encode digits
           * @throws ReaderException if first digit cannot be determined
           */
          private static function determineFirstDigit( resultString:StringBuilder , lgPatternFound:int):void {
            for (var d:int = 0; d < 10; d++) {
              if (lgPatternFound == FIRST_DIGIT_ENCODINGS[d]) {
                resultString.Insert(0, String.fromCharCode( ('0').charCodeAt(0) + d));
                return;
              }
            }
            throw new ReaderException("EAN13Reader : determineFirstDigit : first digit not found");
          }

    
    
    }

}