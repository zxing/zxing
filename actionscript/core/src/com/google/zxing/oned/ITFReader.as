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

    public class ITFReader extends AbstractOneDReader
    { 
    	import com.google.zxing.common.BitArray;
    	import com.google.zxing.ResultPoint;
    	import com.google.zxing.common.flexdatatypes.StringBuilder;
    	
    	import com.google.zxing.BarcodeFormat;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;
		import com.google.zxing.DecodeHintType;
    	
          private static  var MAX_AVG_VARIANCE:int = int(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42);
          private static  var MAX_INDIVIDUAL_VARIANCE:int = int(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.8);

          private static  var W:int = 3; // Pixel width of a wide line
          private static  var N:int = 1; // Pixed width of a narrow line

          // Stores the actual narrow line width of the image being decoded.
          private var narrowLineWidth:int = -1;

          /**
           * Start/end guard pattern.
           *
           * Note: The end pattern is reversed because the row is reversed before
           * searching for the END_PATTERN
           */
          private static  var START_PATTERN:Array = [N, N, N, N];
          private static  var END_PATTERN_REVERSED:Array = [N, N, W];
			
		  private static var DEFAULT_ALLOWED_LENGTHS:Array = [ 6, 10, 14 ];
          /**
           * Patterns of Wide / Narrow lines to indicate each digit
           */
          private static  var PATTERNS:Array = [
              [N, N, W, W, N], // 0
              [W, N, N, N, W], // 1
              [N, W, N, N, W], // 2
              [W, W, N, N, N], // 3
              [N, N, W, N, W], // 4
              [W, N, W, N, N], // 5
              [N, W, W, N, N], // 6
              [N, N, N, W, W], // 7
              [W, N, N, W, N], // 8
              [N, W, N, W, N]  // 9
          ];

          public override function  decodeRow(rowNumber:int, row:BitArray,  hints:Object):Result 
          {
            // Find out where the Middle section (payload) starts & ends
            var startRange:Array = decodeStart(row);
            var endRange:Array = decodeEnd(row);

		    if (startRange == null) { throw new ReaderException("oned : ITFReader : decodeRow : startRange null"); }
			if (endRange == null) { throw new ReaderException("oned : ITFReader : decodeRow : endRange null"); }


			var result:StringBuilder = new StringBuilder(20);
            decodeMiddle(row, startRange[1], endRange[0], result);
            var resultString:String = result.ToString();

			var allowedLengths:Array = null;
    		if (hints != null) 
    		{
      			allowedLengths =hints.getValuesByKey(DecodeHintType.ALLOWED_LENGTHS) as Array;
   	 		}

   	 		if (allowedLengths == null) 
   	 		{
      			allowedLengths = DEFAULT_ALLOWED_LENGTHS;
    		}

		    // To avoid false positives with 2D barcodes (and other patterns), make
		    // an assumption that the decoded string must be 6, 10 or 14 digits.
		    var length:int = resultString.length;
		    var lengthOK:Boolean = false;
		    for (var i:int = 0; i < allowedLengths.length; i++) {
		      if (length == allowedLengths[i]) {
		        lengthOK = true;
		        break;
		      }
		
		    }
		    if (!lengthOK) {
		      throw new ReaderException("oned : ITFReader : decodeRow : Length not OK");
		    }

            return new Result(
                resultString,
                null, // no natural byte representation for these barcodes
                [ new ResultPoint(startRange[1], Number(rowNumber)),
                                    new ResultPoint(startRange[0], Number(rowNumber))],
                BarcodeFormat.ITF);
          }

          /**
           * @param row          row of black/white values to search
           * @param payloadStart offset of start pattern
           * @param resultString {@link StringBuilder} to Append decoded chars to
           * @throws ReaderException if decoding could not complete successfully
           */
          public static function decodeMiddle( row:BitArray,  payloadStart:int, payloadEnd:int,  resultString:StringBuilder):void
          {

            // Digits are interleaved in pairs - 5 black lines for one digit, and the
            // 5
            // interleaved white lines for the second digit.
            // Therefore, need to scan 10 lines and then
            // split these into two arrays
            var counterDigitPair:Array = new Array(10);
            var counterBlack:Array = new Array(5);
            var counterWhite:Array = new Array(5);

            while (payloadStart < payloadEnd) {

              // Get 10 runs of black/white.
              recordPattern(row, payloadStart, counterDigitPair);
              // Split them into each array
              for (var k:int = 0; k < 5; k++) {
                var twoK:int = k << 1;
                counterBlack[k] = counterDigitPair[twoK];
                counterWhite[k] = counterDigitPair[twoK + 1];
              }

              var bestMatch:int = decodeDigit(counterBlack);
              resultString.Append(String.fromCharCode(('0').charCodeAt(0) + bestMatch));
              bestMatch = decodeDigit(counterWhite);
              resultString.Append(String.fromCharCode( ('0').charCodeAt(0) + bestMatch));

              for (var i:int = 0; i < counterDigitPair.length; i++) {
                payloadStart += counterDigitPair[i];
              }
            }
          }

          /**
           * Identify where the start of the middle / payload section starts.
           *
           * @param row row of black/white values to search
           * @return Array, containing index of start of 'start block' and end of
           *         'start block'
           * @throws ReaderException
           */
          public function decodeStart( row:BitArray):Array {
            var endStart:int = skipWhiteSpace(row);
            var startPattern:Array = findGuardPattern(row, endStart, START_PATTERN);

            // Determine the width of a narrow line in pixels. We can do this by
            // getting the width of the start pattern and dividing by 4 because its
            // made up of 4 narrow lines.
            this.narrowLineWidth = (startPattern[1] - startPattern[0]) >> 2;

            validateQuietZone(row, startPattern[0]);

            return startPattern;
          }

          /**
           * The start & end patterns must be pre/post fixed by a quiet zone. This
           * zone must be at least 10 times the width of a narrow line.  Scan back until
           * we either get to the start of the barcode or match the necessary number of
           * quiet zone pixels.
           *
           * Note: Its assumed the row is reversed when using this method to find
           * quiet zone after the end pattern.
           *
           * ref: http://www.barcode-1.net/i25code.html
           *
           * @param row bit array representing the scanned barcode.
           * @param startPattern index into row of the start or end pattern.
           * @throws ReaderException if the quiet zone cannot be found, a ReaderException is thrown.
           */
          private function validateQuietZone( row:BitArray, startPattern:int):void
          {

            var quietCount:int = this.narrowLineWidth * 10;  // expect to find this many pixels of quiet zone

            for (var i:int = startPattern - 1; quietCount > 0 && i >= 0; i--) {
              if (row._get(i)) {
                break;
              }
              quietCount--;
            }
            if (quietCount != 0) {
              // Unable to find the necessary number of quiet zone pixels.
              throw new ReaderException("ITFReader : validateQuietZone : Unable to find the necessary number of quiet zone pixels");
            }
          }

          /**
           * Skip all whitespace until we get to the first black line.
           *
           * @param row row of black/white values to search
           * @return index of the first black line.
           * @throws ReaderException Throws exception if no black lines are found in the row
           */
          private function skipWhiteSpace( row:BitArray):int {
            var width:int = row.getSize();
            var endStart:int = 0;
            while (endStart < width) {
              if (row._get(endStart)) {
                break;
              }
              endStart++;
            }
            if (endStart == width) {
              throw new ReaderException("ITFReader : skipWhiteSpace : endStart == width");
            }

            return endStart;
          }

          /**
           * Identify where the end of the middle / payload section ends.
           *
           * @param row row of black/white values to search
           * @return Array, containing index of start of 'end block' and end of 'end
           *         block'
           * @throws ReaderException
           */

          public function decodeEnd(row:BitArray):Array 
          {
          	var endPattern:Array = null; 
		    // For convenience, reverse the row and then
		    // search from 'the start' for the end block
		    row.reverse();
		    try {
		     var endStart:int = skipWhiteSpace(row);
		      endPattern = findGuardPattern(row, endStart, END_PATTERN_REVERSED);
		
		      // The start & end patterns must be pre/post fixed by a quiet zone. This
		      // zone must be at least 10 times the width of a narrow line.
		      // ref: http://www.barcode-1.net/i25code.html
		      validateQuietZone(row, endPattern[0]);
		
		      // Now recalc the indicies of where the 'endblock' starts & stops to
		      // accomodate
		      // the reversed nature of the search
		      var temp:int = endPattern[0];
		      endPattern[0] = row.getSize() - endPattern[1];
		      endPattern[1] = row.getSize() - temp;
		
		    }
		    finally 
		    {
		      // Put the row back the righ way.
		      row.reverse();
		    }
		      return endPattern;
          }

          /**
           * @param row       row of black/white values to search
           * @param rowOffset position to start search
           * @param pattern   pattern of counts of number of black and white pixels that are
           *                  being searched for as a pattern
           * @return start/end horizontal offset of guard pattern, as an array of two
           *         ints
           * @throws ReaderException if pattern is not found
           */
          public function findGuardPattern( row:BitArray, rowOffset:int, pattern:Array):Array {

            // TODO: This is very similar to implementation in AbstractUPCEANReader. Consider if they can be merged to
            // a single method.

            var patternLength:int = pattern.length;
            var counters:Array = new Array(patternLength);
            for (var i:int=0;i<patternLength;i++) { counters[i] = 0;}
            var width:int = row.getSize();
            var isWhite:Boolean = false;

            var counterPosition:int = 0;
            var patternStart:int = rowOffset;
            for (var x:int = rowOffset; x < width; x++) {
              var pixel:Boolean = row._get(x);
              if (pixel != isWhite) 
              {
              	counters[counterPosition] = counters[counterPosition] + 1;
              } 
              else {
                if (counterPosition == patternLength - 1) {
                  if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
                    return [patternStart, x];
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
                isWhite = !isWhite;
              }
            }
            throw new ReaderException("ITFReader : findGuardPattern : could not find pattern");
          }

          /**
           * Attempts to decode a sequence of ITF black/white lines into single
           * digit.
           *
           * @param counters the counts of runs of observed black/white/black/... values
           * @return The decoded digit
           * @throws ReaderException if digit cannot be decoded
           */
          private static function decodeDigit(counters:Array):int {

            var bestVariance:int = MAX_AVG_VARIANCE; // worst variance we'll accept
            var bestMatch:int = -1;
            var max:int = PATTERNS.length;
            for (var i:int = 0; i < max; i++) {
              var pattern:Array = PATTERNS[i];
              var variance:int = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
              if (variance < bestVariance) {
                bestVariance = variance;
                bestMatch = i;
              }
            }
            if (bestMatch >= 0) {
              return bestMatch;
		        } else {
			        throw new ReaderException("ITFReader : decodeDigit : could not find best match");
		        }
	        }
    
    }
}