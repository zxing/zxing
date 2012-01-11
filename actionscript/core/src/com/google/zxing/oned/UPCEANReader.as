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
	
	public class UPCEANReader extends OneDReader
    { 

		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.common.flexdatatypes.StringBuilder;
		import com.google.zxing.common.BitArray;
		import com.google.zxing.BarcodeFormat;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;
		import com.google.zxing.ResultPoint;
		import com.google.zxing.ResultMetadataType;


          public static var MAX_AVG_VARIANCE:int = int(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42);
          public static var MAX_INDIVIDUAL_VARIANCE:int = int(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7);

          /**
           * Start/end guard pattern.
           */
          public static var START_END_PATTERN:Array = [1, 1, 1];

          /**
           * Pattern marking the middle of a UPC/EAN pattern, separating the two halves.
           */
          public static var MIDDLE_PATTERN:Array = [1, 1, 1, 1, 1];

          /**
           * "Odd", or "L" patterns used to encode UPC/EAN digits.
           */
           
          public static var L_PATTERNS:Array = [
              [3, 2, 1, 1], // 0
              [2, 2, 2, 1], // 1
              [2, 1, 2, 2], // 2
              [1, 4, 1, 1], // 3
              [1, 1, 3, 2], // 4
              [1, 2, 3, 1], // 5
              [1, 1, 1, 4], // 6
              [1, 3, 1, 2], // 7
              [1, 2, 1, 3], // 8
              [3, 1, 1, 2]];  // 9
          

          /**
           * As above but also including the "even", or "G" patterns used to encode UPC/EAN digits.
           */
          //public static var L_AND_G_PATTERNS:Array = new Array(20);
			public static var L_AND_G_PATTERNS:Array = [
														  [3, 2, 1, 1], // 0
											              [2, 2, 2, 1], // 1
											              [2, 1, 2, 2], // 2
											              [1, 4, 1, 1], // 3
											              [1, 1, 3, 2], // 4
											              [1, 2, 3, 1], // 5
											              [1, 1, 1, 4], // 6
											              [1, 3, 1, 2], // 7
											              [1, 2, 1, 3], // 8
											              [3, 1, 1, 2],  // 9
          												  [1, 1, 2, 3], //R0
          												  [1, 2, 2, 2], //R1
          												  [2, 2, 1, 2], //R2
          												  [1, 1, 4, 1], //R3
          												  [2, 3, 1, 1], //R4
          												  [1, 3, 2, 1], //R5
          												  [4, 1, 1, 1], //R6
          												  [2, 1, 3, 1], //R7
          												  [3, 1, 2, 1], //R8
          												  [2, 1, 1, 3], //R9
          												  ];
         // create the inverse of the L patterns
/*          static {
            L_AND_G_PATTERNS = new int[20][];
            for (var i:int = 0; i < 10; i++) 
            {
              	L_AND_G_PATTERNS[i] = L_PATTERNS[i];
            }
            for (int i = 10; i < 20; i++) 
            {
              	var widths:Array = L_PATTERNS[i - 10];
          		var reversedWidths:Array = new Array(widths.length);
              	for (int j = 0; j < widths.length; j++) 
          		{
                	reversedWidths[j] = widths[widths.length - j - 1];
              }
             	 L_AND_G_PATTERNS[i] = reversedWidths;
            }
          }
*/
          private var decodeRowStringBuffer:StringBuilder;
          private var extensionReader:UPCEANExtensionSupport;
  		  private var eanManSupport:EANManufacturerOrgSupport;

          public function UPCEANReader():void
          {
            	decodeRowStringBuffer = new StringBuilder(20);
                extensionReader = new UPCEANExtensionSupport();
    			eanManSupport = new EANManufacturerOrgSupport();
          }

          public static function findStartGuardPattern(row:BitArray):Array 
          {
            var foundStart:Boolean = false;
            var startRange:Array = null;
            var nextStart:int = 0;
            while (!foundStart) 
            {
              startRange = findGuardPattern(row, nextStart, false, START_END_PATTERN);
              var start:int = startRange[0];
              nextStart = startRange[1];
              // Make sure there is a quiet zone at least as big as the start pattern before the barcode. If
              // this check would run off the left edge of the image, do not accept this barcode, as it is
              // very likely to be a false positive.
              var quietStart:int = start - (nextStart - start);
              if (quietStart >= 0) 
              {
                foundStart = row.isRange(quietStart, start, false);
              }
            }
            return startRange;
          }

		public override function decodeRow(rowNumber:Object, row:BitArray, o:Object):Result
		{
			if (o is HashTable){ return decodeRow_HashTable(rowNumber as int,row,o as HashTable); }
			else if (o is Array) { return decodeRow_Array(rowNumber as int,row,o as Array); }
			else {throw new Error('AbstractUPCEANReader : decodeRow : unknow type of object');}
			 	
		}  
		
          public function decodeRow_HashTable(rowNumber:int, row:BitArray, hints:HashTable):Result 
          {
            return decodeRow(rowNumber, row, findStartGuardPattern(row));
          }

          public  function decodeRow_Array(rowNumber:int, row:BitArray, startGuardRange:Array):Result 
          {
            var result:StringBuilder = decodeRowStringBuffer;// empty stringbuilder
            result.length = 0;
            var endStart:int = decodeMiddle(row, startGuardRange, result);
            var endRange:Array = decodeEnd(row, endStart);

            // Make sure there is a quiet zone at least as big as the end pattern after the barcode. The
            // spec might want more whitespace, but in practice this is the maximum we can count on.
            var end:int = endRange[1];
            var quietEnd:int = end + (end - endRange[0]);
            if (quietEnd >= row.getSize() || !row.isRange(end, quietEnd, false)) 
            {
              throw new ReaderException("AbstractUPCEANReader : decodeRow_Array : ending white space is missing");
            }

            var resultString:String = result.toString();
            if (!checkChecksum(resultString)) {
              throw new ReaderException("AbstractUPCEANReader : decodeRow_Array : checkChecksum failed");
            }

            var left:Number = (Number) (startGuardRange[1] + startGuardRange[0]) / 2;
            var right:Number = (Number) (endRange[1] + endRange[0]) / 2;
            var format:BarcodeFormat = getBarcodeFormat();
		    var decodeResult:Result = new Result(resultString,
		        null, // no natural byte representation for these barcodes
		        [
		            new ResultPoint(left, rowNumber),
		            new ResultPoint(right, rowNumber)],
		        format);
		
		    try {
		      var extensionResult:Result  = extensionReader.decodeRow(rowNumber, row, endRange[1]);
		      decodeResult.putAllMetadata(extensionResult.getResultMetadata());
		      decodeResult.addResultPoints(extensionResult.getResultPoints());
		    } catch (re:ReaderException) {
		      // continue
		    }
		
		    if ((format == BarcodeFormat.EAN_13) || (format == BarcodeFormat.UPC_A)) {
		      var countryID:String = eanManSupport.lookupCountryIdentifier(resultString);
		      if (countryID != null) {
		        decodeResult.putMetadata(ResultMetadataType.POSSIBLE_COUNTRY, countryID);
		      }
		    }
		
		    return decodeResult;

          }

          public function getBarcodeFormat():BarcodeFormat 
          {
          		return null;
          }

          /**
           * @return {@link #checkStandardUPCEANChecksum(String)} 
           */
          public function checkChecksum(s:String):Boolean {
            return checkStandardUPCEANChecksum(s);
          }

          /**
           * Computes the UPC/EAN checksum on a string of digits, and reports
           * whether the checksum is correct or not.
           *
           * @param s string of digits to check
           * @return true iff string of digits passes the UPC/EAN checksum algorithm
           * @throws ReaderException if the string does not contain only digits
           */
          public static function checkStandardUPCEANChecksum(s:String):Boolean {
            var length:int = s.length;
            if (length == 0) 
            {
              return false;
            }

            var sum:int = 0;
            for (var i:int = length - 2; i >= 0; i -= 2) {
              var digit:int = s.charCodeAt(i) - ('0').charCodeAt(0);
              if (digit < 0 || digit > 9) {
                throw new ReaderException("AbstractUPCEANReader : checkStandardUPCEANChecksum : digit out of range ("+digit+")");
              }
              sum += digit;
            }
            sum *= 3;
            for (var i3:int = length - 1; i3 >= 0; i3 -= 2) {
              var digit2:int = s.charCodeAt(i3) - ('0').charCodeAt(0);
              if (digit2 < 0 || digit2 > 9) {
                throw new ReaderException("AbstractUPCEANReader : checkStandardUPCEANChecksum : digit2 out of range ("+digit2+")");
              }
              sum += digit2;
            }
            return sum % 10 == 0;
          }

          /**
           * Subclasses override this to decode the portion of a barcode between the start and end guard patterns.
           *
           * @param row row of black/white values to search
           * @param startRange start/end offset of start guard pattern
           * @param resultString {@link StringBuffer} to append decoded chars to
           * @return horizontal offset of first pixel after the "middle" that was decoded
           * @throws ReaderException if decoding could not complete successfully
           */
          protected function decodeMiddle(row:BitArray, startRange:Array, resultString:StringBuilder):int{return -1;};

          public function decodeEnd(row:BitArray, endStart:int):Array 
          {
            return findGuardPattern(row, endStart, false, START_END_PATTERN);
          }

          /**
           * @param row row of black/white values to search
           * @param rowOffset position to start search
           * @param whiteFirst if true, indicates that the pattern specifies white/black/white/...
           * pixel counts, otherwise, it is interpreted as black/white/black/...
           * @param pattern pattern of counts of number of black and white pixels that are being
           * searched for as a pattern
           * @return start/end horizontal offset of guard pattern, as an array of two ints
           * @throws ReaderException if pattern is not found
           */
          public static function findGuardPattern( row:BitArray, rowOffset:int, whiteFirst:Boolean, pattern:Array):Array
              {
            var patternLength:int = pattern.length;
            var counters:Array = new Array(patternLength);
            for (var i:int=0;i<patternLength;i++) { counters[i] = 0; }
            var width:int = row.getSize();
            var isWhite:Boolean = false;
            while (rowOffset < width) {
              isWhite = !row._get(rowOffset);
              if (whiteFirst == isWhite) {
                break;
              }
              rowOffset++;
            }

            var counterPosition:int = 0;
            var patternStart:int = rowOffset;
            for (var x:int = rowOffset; x < width; x++) 
            {
              var pixel:Boolean = row._get(x);
              if (pixel != isWhite) 
              {
				counters[counterPosition] = counters[counterPosition] + 1;             	
              } 
              else 
              {
                if (counterPosition == patternLength - 1) 
                {
                  if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) 
                  {
                    return [patternStart, x];
                  }
                  patternStart = patternStart + counters[0] + counters[1];
                  for (var y:int = 2; y < patternLength; y++)
                  {
                    counters[y - 2] = counters[y];
                  }
                  counters[patternLength - 2] = 0;
                  counters[patternLength - 1] = 0;
                  counterPosition--;
                } 
                else 
                {
                  counterPosition++;
                }
                counters[counterPosition] = 1;
                isWhite = !isWhite;
              }
            }
            throw new ReaderException("AbstractUPCEANReader : findGuardPattern : pattern not found)");
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
           * @throws ReaderException if digit cannot be decoded
           */
          public static function decodeDigit(row:BitArray,counters:Array, rowOffset:int, patterns:Array):int
              {
              	
            recordPattern(row, rowOffset, counters);
            var bestVariance:int = MAX_AVG_VARIANCE; // worst variance we'll accept
            var bestMatch:int = -1;
            var max:int = patterns.length;
            for (var i:int = 0; i < max; i++) 
            {
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
              throw new ReaderException("AbstractUPCEANReader : decodeDigit : not bestMatch found");
            }
          }
	}
}