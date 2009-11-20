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
		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.BinaryBitmap;
		import com.google.zxing.DecodeHintType;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;
		import com.google.zxing.ResultMetadataType;
		import com.google.zxing.ResultPoint;
		import com.google.zxing.Reader;

	public class AbstractOneDReader implements Reader
	{
		
          private static var INTEGER_MATH_SHIFT:int = 8;
          public static  var PATTERN_MATCH_RESULT_SCALE_FACTOR:int = (1 << INTEGER_MATH_SHIFT);

          //public  function decode(image:MonochromeBitmapSource):Result 
          //{
          //  return decode(image, null);
          //}

          public  function decode(image:BinaryBitmap,hints:HashTable=null):Result 
          {
            try 
            {
              return doDecode(image, hints);
            } 
            catch (re:ReaderException) 
            {
              var tryHarder:Boolean = (hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER));
              if (tryHarder && image.isRotateSupported()) 
              {
                var rotatedImage:BinaryBitmap = image.rotateCounterClockwise();
                var result:Result = doDecode(rotatedImage, hints);
                // Record that we found it rotated 90 degrees CCW / 270 degrees CW
                var metadata:HashTable = result.getResultMetadata();
                var orientation:int = 270;
                if (metadata != null && metadata.ContainsKey(ResultMetadataType.ORIENTATION)) 
                {
                  // But if we found it reversed in doDecode(), add in that result here:
                  orientation = (orientation + (int(metadata[ResultMetadataType.ORIENTATION]) )) % 360;
                }
                result.putMetadata(ResultMetadataType.ORIENTATION, orientation);
            	return result;    
              } 
              else 
              {
                throw re;
              }
            }
           return null; 
          }

          /**
           * We're going to examine rows from the middle outward, searching alternately above and below the
           * middle, and farther out each time. rowStep is the number of rows between each successive
           * attempt above and below the middle. So we'd scan row middle, then middle - rowStep, then
           * middle + rowStep, then middle - (2 * rowStep), etc.
           * rowStep is bigger as the image is taller, but is always at least 1. We've somewhat arbitrarily
           * decided that moving up and down by about 1/16 of the image is pretty good; we try more of the
           * image if "trying harder".
           *
           * @param image The image to decode
           * @param hints Any hints that were requested
           * @return The contents of the decoded barcode
           * @throws ReaderException Any spontaneous errors which occur
           */
          private function doDecode(image:BinaryBitmap, hints:HashTable):Result 
          {
            var width:int = image.getWidth();
            var height:int = image.getHeight();
            var row:BitArray = new BitArray(width);

            var middle:int = height >> 1;
            var tryHarder:Boolean = (hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER));
            var rowStep:int = Math.max(1, height >> (tryHarder ? 7 : 4));
            var MaxLines:int;
            if (tryHarder) 
            {
              MaxLines = height; // Look at the whole image, not just the center
            } else {
              MaxLines = 9; // Nine rows spaced 1/16 apart is roughly the middle half of the image
            }

            for (var x:int = 0; x < MaxLines; x++) {

              // Scanning from the middle out. Determine which row we're looking at next:
              var rowStepsAboveOrBelow:int = (x + 1) >> 1;
              var isAbove:Boolean = (x & 0x01) == 0; // i.e. is x even?
              var rowNumber:int = middle + rowStep * (isAbove ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow);
              if (rowNumber < 0 || rowNumber >= height) {
                // Oops, if we run off the top or bottom, stop
                break;
              }

              // Estimate black point for this row and load it:
              try {
                row = image.getBlackRow(rowNumber,row);
              } 
              catch (re:ReaderException) 
              {
                continue;
              }

              // While we have the image data in a BitArray, it's fairly cheap to reverse it in place to
              // handle decoding upside down barcodes.
              for (var attempt:int = 0; attempt < 2; attempt++) {
                if (attempt == 1) { // trying again?
                  row.reverse(); // reverse the row and continue
                }
                try {
                  // Look for a barcode
                  var result:Result = decodeRow(rowNumber, row, hints);
                  // We found our barcode (else exception thrown)
                  if (attempt == 1) 
                  {
                    // But it was upside down, so note that
                    result.putMetadata(ResultMetadataType.ORIENTATION, 180);
                    // And remember to flip the result points horizontally.
                    var points:Array = result.getResultPoints();
                    points[0] = ResultPoint(new ResultPoint(width - points[0].getX() - 1, points[0].getY()));
                    points[1] = ResultPoint(new ResultPoint(width - points[1].getX() - 1, points[1].getY()));
                  }
                  return result;
                } catch (re:ReaderException) {
                  // continue -- just couldn't decode this row
                  var a:int=0;
                }
              }
            }

            throw new ReaderException("AbstractOneDReader : doDecode : could not decode image");
          }

          /**
           * Records the size of successive runs of white and black pixels in a row, starting at a given point.
           * The values are recorded in the given array, and the number of runs recorded is equal to the size
           * of the array. If the row starts on a white pixel at the given start point, then the first count
           * recorded is the run of white pixels starting from that point; likewise it is the count of a run
           * of black pixels if the row begin on a black pixels at that point.
           *
           * @param row row to count from
           * @param start offset into row to start at
           * @param counters array into which to record counts
           * @throws ReaderException if counters cannot be filled entirely from row before running out of pixels
           */
          public static function recordPattern(row:BitArray, start:int, counters:Array):void 
          {
            var numCounters:int = counters.length;
           for (var i:int=0;i<counters.length;i++)  { counters[i] = 0;}
           
            var end:int = row.getSize();
            if (start >= end) {
              throw new ReaderException("AbstractOneDReader : recordPattern : start after end ("+start+" > "+end);
            }
            var isWhite:Boolean = !row._get(start);
            var counterPosition:int = 0;

            var k:int = start;
            while (k < end) 
            {
              var pixel:Boolean = row._get(k);
              if (pixel != isWhite)  
              {
             		counters[counterPosition] = counters[counterPosition] + 1;
              } 
              else 
              {
                counterPosition++;
                if (counterPosition == numCounters) 
                {
                  break;
                } 
                else 
                {
                  counters[counterPosition] = 1;
                  isWhite = !isWhite;//isWhite = !isWhite;  Is this too clever? shorter byte code, no conditional
                }
              }
              k++;
            }
            // If we read fully the last section of pixels and filled up our counters -- or filled
            // the last counter but ran off the side of the image, OK. Otherwise, a problem.
            if (!(counterPosition == numCounters || (counterPosition == numCounters - 1 && k == end))) 
            {
              throw new ReaderException("AbstractOneDReader : recordPattern : barcode seems to extend outside of the image");
            }
          }

          /**
           * Determines how closely a set of observed counts of runs of black/white values matches a given
           * target pattern. This is reported as the ratio of the total variance from the expected pattern
           * proportions across all pattern elements, to the length of the pattern.
           *
           * @param counters observed counters
           * @param pattern expected pattern
           * @param MaxIndividualVariance The most any counter can differ before we give up
           * @return ratio of total variance between counters and pattern compared to total pattern size,
           *  where the ratio has been multiplied by 256. So, 0 means no variance (perfect match); 256 means
           *  the total variance between counters and patterns equals the pattern length, higher values mean
           *  even more variance
           */
          public static function patternMatchVariance(counters:Array,pattern:Array, MaxIndividualVariance:int):int 
          {
            var numCounters:int = counters.length;
            var total:int = 0;
            var patternLength:int = 0;
            for (var i:int = 0; i < numCounters; i++) 
            {
              total += counters[i];
              patternLength += pattern[i];
            }
            if (total < patternLength) 
            {
              // If we don't even have one pixel per unit of bar width, assume this is too small
              // to reliably match, so fail:
              return int.MAX_VALUE;
            }
            // We're going to fake floating-point math in integers. We just need to use more bits.
            // Scale up patternLength so that intermediate values below like scaledCounter will have
            // more "significant digits"
            var unitBarWidth:int = (total << INTEGER_MATH_SHIFT) / patternLength;
            MaxIndividualVariance = (MaxIndividualVariance * unitBarWidth) >> INTEGER_MATH_SHIFT;

            var totalVariance:int = 0;
            for (var x:int = 0; x < numCounters; x++) {
              var counter:int = (counters[x]) << INTEGER_MATH_SHIFT;
              var scaledPattern:int = pattern[x] * unitBarWidth;
              var variance:int = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
              if (variance > MaxIndividualVariance) 
              {
                return int.MAX_VALUE
              }
              totalVariance += variance; 
            }
            return totalVariance / total;
          }

          // This declaration should not be necessary, since this class is
          // abstract and so does not have to provide an implementation for every
          // method of an interface it implements, but it is causing NoSuchMethodError
          // issues on some Nokia JVMs. So we add this superfluous declaration:

          public function decodeRow(rowNumber:int, row:BitArray, hints:Object):Result{return null;};
	}
}