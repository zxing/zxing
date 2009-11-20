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
	
 	public class Code128Reader extends AbstractOneDReader
    { 
		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.common.flexdatatypes.StringBuilder;
		import com.google.zxing.BarcodeFormat;
		import com.google.zxing.ReaderException;
		import com.google.zxing.Result;
		import com.google.zxing.ResultPoint;
		import com.google.zxing.common.BitArray;


            private static  var CODE_PATTERNS:Array = [
              [2, 1, 2, 2, 2, 2], // 0
              [2, 2, 2, 1, 2, 2],
              [2, 2, 2, 2, 2, 1],
              [1, 2, 1, 2, 2, 3],
              [1, 2, 1, 3, 2, 2],
              [1, 3, 1, 2, 2, 2], // 5
              [1, 2, 2, 2, 1, 3],
              [1, 2, 2, 3, 1, 2],
              [1, 3, 2, 2, 1, 2],
              [2, 2, 1, 2, 1, 3],
              [2, 2, 1, 3, 1, 2], // 10
              [2, 3, 1, 2, 1, 2],
              [1, 1, 2, 2, 3, 2],
              [1, 2, 2, 1, 3, 2],
              [1, 2, 2, 2, 3, 1],
              [1, 1, 3, 2, 2, 2], // 15
              [1, 2, 3, 1, 2, 2],
              [1, 2, 3, 2, 2, 1],
              [2, 2, 3, 2, 1, 1],
              [2, 2, 1, 1, 3, 2],
              [2, 2, 1, 2, 3, 1], // 20
              [2, 1, 3, 2, 1, 2],
              [2, 2, 3, 1, 1, 2],
              [3, 1, 2, 1, 3, 1],
              [3, 1, 1, 2, 2, 2],
              [3, 2, 1, 1, 2, 2], // 25
              [3, 2, 1, 2, 2, 1],
              [3, 1, 2, 2, 1, 2],
              [3, 2, 2, 1, 1, 2],
              [3, 2, 2, 2, 1, 1],
              [2, 1, 2, 1, 2, 3], // 30
              [2, 1, 2, 3, 2, 1],
              [2, 3, 2, 1, 2, 1],
              [1, 1, 1, 3, 2, 3],
              [1, 3, 1, 1, 2, 3],
              [1, 3, 1, 3, 2, 1], // 35
              [1, 1, 2, 3, 1, 3],
              [1, 3, 2, 1, 1, 3],
              [1, 3, 2, 3, 1, 1],
              [2, 1, 1, 3, 1, 3],
              [2, 3, 1, 1, 1, 3], // 40
              [2, 3, 1, 3, 1, 1],
              [1, 1, 2, 1, 3, 3],
              [1, 1, 2, 3, 3, 1],
              [1, 3, 2, 1, 3, 1],
              [1, 1, 3, 1, 2, 3], // 45
              [1, 1, 3, 3, 2, 1],
              [1, 3, 3, 1, 2, 1],
              [3, 1, 3, 1, 2, 1],
              [2, 1, 1, 3, 3, 1],
              [2, 3, 1, 1, 3, 1], // 50
              [2, 1, 3, 1, 1, 3],
              [2, 1, 3, 3, 1, 1],
              [2, 1, 3, 1, 3, 1],
              [3, 1, 1, 1, 2, 3],
              [3, 1, 1, 3, 2, 1], // 55
              [3, 3, 1, 1, 2, 1],
              [3, 1, 2, 1, 1, 3],
              [3, 1, 2, 3, 1, 1],
              [3, 3, 2, 1, 1, 1],
              [3, 1, 4, 1, 1, 1], // 60
              [2, 2, 1, 4, 1, 1],
              [4, 3, 1, 1, 1, 1],
              [1, 1, 1, 2, 2, 4],
              [1, 1, 1, 4, 2, 2],
              [1, 2, 1, 1, 2, 4], // 65
              [1, 2, 1, 4, 2, 1],
              [1, 4, 1, 1, 2, 2],
              [1, 4, 1, 2, 2, 1],
              [1, 1, 2, 2, 1, 4],
              [1, 1, 2, 4, 1, 2], // 70
              [1, 2, 2, 1, 1, 4],
              [1, 2, 2, 4, 1, 1],
              [1, 4, 2, 1, 1, 2],
              [1, 4, 2, 2, 1, 1],
              [2, 4, 1, 2, 1, 1], // 75
              [2, 2, 1, 1, 1, 4],
              [4, 1, 3, 1, 1, 1],
              [2, 4, 1, 1, 1, 2],
              [1, 3, 4, 1, 1, 1],
              [1, 1, 1, 2, 4, 2], // 80
              [1, 2, 1, 1, 4, 2],
              [1, 2, 1, 2, 4, 1],
              [1, 1, 4, 2, 1, 2],
              [1, 2, 4, 1, 1, 2],
              [1, 2, 4, 2, 1, 1], // 85
              [4, 1, 1, 2, 1, 2],
              [4, 2, 1, 1, 1, 2],
              [4, 2, 1, 2, 1, 1],
              [2, 1, 2, 1, 4, 1],
              [2, 1, 4, 1, 2, 1], // 90
              [4, 1, 2, 1, 2, 1],
              [1, 1, 1, 1, 4, 3],
              [1, 1, 1, 3, 4, 1],
              [1, 3, 1, 1, 4, 1],
              [1, 1, 4, 1, 1, 3], // 95
              [1, 1, 4, 3, 1, 1],
              [4, 1, 1, 1, 1, 3],
              [4, 1, 1, 3, 1, 1],
              [1, 1, 3, 1, 4, 1],
              [1, 1, 4, 1, 3, 1], // 100
              [3, 1, 1, 1, 4, 1],
              [4, 1, 1, 1, 3, 1],
              [2, 1, 1, 4, 1, 2],
              [2, 1, 1, 2, 1, 4],
              [2, 1, 1, 2, 3, 2], // 105
              [2, 3, 3, 1, 1, 1, 2]];


      private static  var MAX_AVG_VARIANCE:int = int(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.25);
      private static  var MAX_INDIVIDUAL_VARIANCE:int = int(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7);

      private static var CODE_SHIFT:int = 98;

      private static var CODE_CODE_C:int = 99;
      private static var CODE_CODE_B:int = 100;
      private static var CODE_CODE_A:int = 101;

      private static var CODE_FNC_1:int = 102;
      private static var CODE_FNC_2:int = 97;
      private static var CODE_FNC_3:int = 96;
      private static var CODE_FNC_4_A:int = 101;
      private static var CODE_FNC_4_B:int = 100;

      private static var CODE_START_A:int = 103;
      private static var CODE_START_B:int = 104;
      private static var CODE_START_C:int = 105;
      private static var CODE_STOP:int = 106;

      private static function findStartPattern(row:BitArray):Array 
      {
        var width:int = row.getSize();
        var rowOffset:int = 0;
        while (rowOffset < width) 
        {
          if (row._get(rowOffset)) {
            break;
          }
          rowOffset++;
        }

        var counterPosition:int = 0;
        var counters:Array = new Array(6);
        var i:int;
        for (i=0;i<counters.length;i++) { counters[i] = 0; }
        var patternStart:int = rowOffset;
        var isWhite:Boolean = false;
        var patternLength:int = counters.length;

        for (i = rowOffset; i < width; i++) {
          var pixel:Boolean = row._get(i);
          if (pixel != isWhite) 
          {
             counters[counterPosition] = counters[counterPosition] + 1;
            
          } else {
            if (counterPosition == patternLength - 1) {
              var bestVariance:int = MAX_AVG_VARIANCE;
              var bestMatch:int = -1;
              for (var startCode:int = CODE_START_A; startCode <= Code128Reader.CODE_START_C; startCode++) {
                var variance:int = patternMatchVariance(counters, CODE_PATTERNS[startCode], MAX_INDIVIDUAL_VARIANCE);
                if (variance < bestVariance) {
                  bestVariance = variance;
                  bestMatch = startCode;
                }
              }
              if (bestMatch >= 0) {
                // Look for whitespace before start pattern, >= 50% of width of start pattern            
                if (row.isRange(Math.max(0, patternStart - (i - patternStart) / 2), patternStart, false)) 
                {
                  return [patternStart, i, bestMatch]; 
                }
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
        throw new ReaderException("Code128Reader : findStartPattern : could not find pattern");
      }

      private static function decodeCode(row:BitArray, counters:Array, rowOffset:int):int 
      {
        recordPattern(row, rowOffset, counters);
        var bestVariance:int = MAX_AVG_VARIANCE; // worst variance we'll accept
        var bestMatch:int = -1;
        for (var d:int = 0; d < CODE_PATTERNS.length; d++) {
          var pattern:Array = CODE_PATTERNS[d];
          var variance:int = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
          if (variance < bestVariance) {
            bestVariance = variance;
            bestMatch = d;
          }
        }
        // TODO We're overlooking the fact that the STOP pattern has 7 values, not 6
        if (bestMatch >= 0) {
          return bestMatch;
        } else {
          throw new ReaderException("Code128Reader : decodeCode : no bestMatch found");
        }
      }

      public override function decodeRow(rowNumber:int,  row:BitArray, hints:Object):Result {

        var startPatternInfo:Array = findStartPattern(row);
        var startCode:int = startPatternInfo[2];
        var codeSet:int;
        switch (startCode) {
          case CODE_START_A:
            codeSet = CODE_CODE_A;
            break;
          case CODE_START_B:
            codeSet = CODE_CODE_B;
            break;
          case CODE_START_C:
            codeSet = CODE_CODE_C;
            break;
          default:
            throw new ReaderException("Code128Reader : decodeRow : startCode invalid : "+startCode);
        }

        var done:Boolean = false;
        var isNextShifted:Boolean = false;

        var result:StringBuilder = new StringBuilder();
        var lastStart:int = startPatternInfo[0];
        var nextStart:int = startPatternInfo[1];
        var counters:Array = new Array(6);

        var lastCode:int = 0;
        var code:int = 0;
        var checksumTotal:int = startCode;
        var multiplier:int = 0;
        var lastCharacterWasPrintable:Boolean = true;

        while (!done) {

          var unshift:Boolean = isNextShifted;
          isNextShifted = false;

          // Save off last code
          lastCode = code;

          // Decode another code from image
          code = decodeCode(row, counters, nextStart);

          // Remember whether the last code was printable or not (excluding CODE_STOP)
          if (code != CODE_STOP) {
            lastCharacterWasPrintable = true;
          }

          // Add to checksum computation (if not CODE_STOP of course)
          if (code != CODE_STOP) {
            multiplier++;
            checksumTotal += multiplier * code;
          }

          // Advance to where the next code will to start
          lastStart = nextStart;
          for (var i:int = 0; i < counters.length; i++) {
            nextStart += counters[i];
          }

          // Take care of illegal start codes
          switch (code) {
            case CODE_START_A:
            case CODE_START_B:
            case CODE_START_C:
              throw new ReaderException("Code128Reader : decodeRow : illegal startcode : "+code);
          }

          switch (codeSet) {
            case CODE_CODE_A:
              if (code < 64) {
                result.Append(String.fromCharCode(String(' ').charAt(0) + code));
              } else if (code < 96) {
                result.Append(String.fromCharCode(code - 64));
              } else {
                // Don't let CODE_STOP, which always appears, affect whether whether we think the last code
                // was printable or not
                if (code != CODE_STOP) {
                  lastCharacterWasPrintable = false;
                }
                switch (code) {
                  case CODE_FNC_1:
                  case CODE_FNC_2:
                  case CODE_FNC_3:
                  case CODE_FNC_4_A:
                    // do nothing?
                    break;
                  case CODE_SHIFT:
                    isNextShifted = true;
                    codeSet = CODE_CODE_B;
                    break;
                  case CODE_CODE_B:
                    codeSet = CODE_CODE_B;
                    break;
                  case CODE_CODE_C:
                    codeSet = CODE_CODE_C;
                    break;
                  case CODE_STOP:
                    done = true;
                    break;
                }
              }
              break;
            case CODE_CODE_B:
              if (code < 96) {
                result.Append(String.fromCharCode((' ').charCodeAt(0) + code));
              } else {
                if (code != CODE_STOP) {
                  lastCharacterWasPrintable = false;
                }
                switch (code) {
                  case CODE_FNC_1:
                  case CODE_FNC_2:
                  case CODE_FNC_3:
                  case CODE_FNC_4_B:
                    // do nothing?
                    break;
                  case CODE_SHIFT:
                    isNextShifted = true;
                    codeSet = CODE_CODE_C;
                    break;
                  case CODE_CODE_A:
                    codeSet = CODE_CODE_A;
                    break;
                  case CODE_CODE_C:
                    codeSet = CODE_CODE_C;
                    break;
                  case CODE_STOP:
                    done = true;
                    break;
                }
              }
              break;
            case CODE_CODE_C:
              if (code < 100) {
                if (code < 10) {
                  result.Append('0');
                }
                result.Append(code);
              } else {
                if (code != CODE_STOP) {
                  lastCharacterWasPrintable = false;
                }
                switch (code) {
                  case CODE_FNC_1:
                    // do nothing?
                    break;
                  case CODE_CODE_A:
                    codeSet = CODE_CODE_A;
                    break;
                  case CODE_CODE_B:
                    codeSet = CODE_CODE_B;
                    break;
                  case CODE_STOP:
                    done = true;
                    break;
                }
              }
              break;
          }

          // Unshift back to another code set if we were shifted
          if (unshift) {
            switch (codeSet) {
              case CODE_CODE_A:
                codeSet = CODE_CODE_C;
                break;
              case CODE_CODE_B:
                codeSet = CODE_CODE_A;
                break;
              case CODE_CODE_C:
                codeSet = CODE_CODE_B;
                break;
            }
          }

        }

        // Check for ample whitespice following pattern, but, to do this we first need to remember that we
        // fudged decoding CODE_STOP since it actually has 7 bars, not 6. There is a black bar left to read off.
        // Would be slightly better to properly read. Here we just skip it:
        while (row._get(nextStart)) {
          nextStart++;
        }
        if (!row.isRange(nextStart, Math.min(row.getSize(), nextStart + (nextStart - lastStart) / 2), false)) {
          throw new ReaderException("Code128Reader : decodeRow : no Range in row ");
        }

        // Pull out from sum the value of the penultimate check code
        checksumTotal -= multiplier * lastCode;
        // lastCode is the checksum then:
        if (checksumTotal % 103 != lastCode) {
          throw new ReaderException("Code128Reader : decodeRow : lastCode is checksum");
        }

        // Need to pull out the check digits from string
        var resultLength:int = result.length;
        // Only bother if, well, the result had at least one character, and if the checksum digit happened
        // to be a printable character. If it was just interpreted as a control code, nothing to remove
        if (resultLength > 0 && lastCharacterWasPrintable) {
          if (codeSet == CODE_CODE_C) {
              result.Remove(resultLength - 2, resultLength);
          } else {
              result.Remove(resultLength - 1, resultLength);
          }
        }

        var resultString:String = result.ToString();

        if (resultString.length == 0) {
          // Almost surely a false positive
          throw new ReaderException("Code128Reader : decodeRow : no resultstring could be found");
        }

        var left:Number = Number( (startPatternInfo[1] + startPatternInfo[0]) / 2);
        var right:Number = Number( (nextStart + lastStart) / 2);
        return new Result(
            resultString,
            null,
            [new ResultPoint(left, Number(rowNumber)),new ResultPoint(right, Number( rowNumber))],
            BarcodeFormat.CODE_128);

      }
    
    }



}