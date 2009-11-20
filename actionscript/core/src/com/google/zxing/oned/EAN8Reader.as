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
 	 import com.google.zxing.common.BitArray;
 	 import com.google.zxing.common.flexdatatypes.StringBuilder;
	 import com.google.zxing.BarcodeFormat;

	 public class EAN8Reader extends AbstractUPCEANReader 
	 {
	 	
          private var decodeMiddleCounters:Array;
          
          public function EAN8Reader() {
            decodeMiddleCounters = new Array(4);
          }

          protected override function decodeMiddle(row:BitArray, startRange:Array,  result:StringBuilder):int {
            var counters:Array = decodeMiddleCounters;
            counters[0] = 0;
            counters[1] = 0;
            counters[2] = 0;
            counters[3] = 0;
            var end:int = row.getSize();
            var rowOffset:int = startRange[1];

            for (var x2:int = 0; x2 < 4 && rowOffset < end; x2++) {
              var bestMatch2:int = decodeDigit(row, counters, rowOffset, L_PATTERNS);
              result.Append(String.fromCharCode( ('0').charCodeAt(0) + bestMatch2));
              for (var i:int = 0; i < counters.length; i++) {
                rowOffset += counters[i];
              }
            }

            var middleRange:Array = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
            rowOffset = middleRange[1];

            for (var x:int = 0; x < 4 && rowOffset < end; x++) {
              var bestMatch:int = decodeDigit(row, counters, rowOffset, L_PATTERNS);
              result.Append(String.fromCharCode( ('0').charCodeAt(0) + bestMatch));
              for (var i2:int = 0; i2 < counters.length; i2++) {
                rowOffset += counters[i2];
              }
            }

            return rowOffset;
          }

          public override function getBarcodeFormat():BarcodeFormat {
            return BarcodeFormat.EAN_8;  
          }
            
            }

}