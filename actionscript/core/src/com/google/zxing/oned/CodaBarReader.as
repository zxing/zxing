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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
import com.google.zxing.common.flexdatatypes.StringBuilder;

/**
 * <p>Decodes Codabar barcodes.</p>
 *
 * @author Bas Vijfwinkel
 */
public final class CodaBarReader extends OneDReader {

  public static var ALPHABET_STRING:String = "0123456789-$:/.+ABCDTN";
  public static var ALPHABET:Array = CodaBarReader.ALPHABET_STRING.split("");

  /**
   * These represent the encodings of characters, as patterns of wide and narrow bars. The 7 least-significant bits of
   * each int correspond to the pattern of wide and narrow, with 1s representing "wide" and 0s representing narrow. NOTE
   * : c is equal to the  * pattern NOTE : d is equal to the e pattern
   */
  private static var CHARACTER_ENCODINGS:Array = [
      0x003, 0x006, 0x009, 0x060, 0x012, 0x042, 0x021, 0x024, 0x030, 0x048, // 0-9
      0x00c, 0x018, 0x045, 0x051, 0x054, 0x015, 0x01A, 0x029, 0x00B, 0x00E, // -$:/.+ABCD
      0x01A, 0x029 //TN
  ];

  // minimal number of characters that should be present (inclusing start and stop characters)
  // this check has been added to reduce the number of false positive on other formats
  // until the cause for this behaviour has been determined
  // under normal circumstances this should be set to 3
  private static var minCharacterLength:int = 6; 
  
  // multiple start/end patterns
  // official start and end patterns
  private static var STARTEND_ENCODING:Array = ['E', '*', 'A', 'B', 'C', 'D', 'T', 'N'];
  // some codabar generator allow the codabar string to be closed by every character
  //private static final char[] STARTEND_ENCODING = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '$', ':', '/', '.', '+', 'A', 'B', 'C', 'D', 'T', 'N'};
  
  // some industries use a checksum standard but this is not part of the original codabar standard
  // for more information see : http://www.mecsw.com/specs/codabar.html

  public override function decodeRow(rowNumber:Object, row:BitArray, o:Object):Result 
  {
    var start:Array = findAsteriskPattern(row);
    start[1] = 0; // settings this to 0 improves the recognition rate somehow?
    var nextStart:int = start[1];
    var end:int = row.getSize();

    // Read off white space
    while (nextStart < end && !row._get(nextStart)) {
      nextStart++;
    }

    var result:StringBuilder  = new StringBuilder();
    //int[] counters = new int[7];
    var counters:Array;
    var lastStart:int;

    do {
      counters = [0, 0, 0, 0, 0, 0, 0]; // reset counters
      recordPattern(row, nextStart, counters);

      var decodedChar:String = toNarrowWidePattern(counters);
      if (decodedChar == '!') {
        throw NotFoundException.getNotFoundInstance();
      }
      result.Append(decodedChar);
      lastStart = nextStart;
      for (i = 0; i < counters.length; i++) {
        nextStart += counters[i];
      }

      // Read off white space
      while (nextStart < end && !row._get(nextStart)) {
        nextStart++;
      }
    } while (nextStart < end); // no fixed end pattern so keep on reading while data is available

    // Look for whitespace after pattern:
    var lastPatternSize:int = 0;
    for (var i:int = 0; i < counters.length; i++) {
      lastPatternSize += counters[i];
    }

    var whiteSpaceAfterEnd:int = nextStart - lastStart - lastPatternSize;
    // If 50% of last pattern size, following last pattern, is not whitespace, fail
    // (but if it's whitespace to the very end of the image, that's OK)
    if (nextStart != end && (whiteSpaceAfterEnd / 2 < lastPatternSize)) {
      throw NotFoundException.getNotFoundInstance();
    }

	// valid result?
	if (result.length < 2)
	{
		throw NotFoundException.getNotFoundInstance();
	}
	
	var startchar:String = result.charAt(0);
	if (!arrayContains(STARTEND_ENCODING,startchar))
	{
		//invalid start character
		throw NotFoundException.getNotFoundInstance();
	}
    
	// find stop character
    for (var k:int = 1;k < result.length ;k++) 
	{
      if (result.charAt(k) == startchar) 
	  {
        // found stop character -> discard rest of the string
		if ((k+1) != result.length)
		{
			result.Remove(k+1,result.length-1);
			k = result.length;// break out of loop
		} 
	  }
    }

    // remove stop/start characters character and check if a string longer than 5 characters is contained
    if (result.length > minCharacterLength) 
	{ 
		result.deleteCharAt(result.length-1); 
		result.deleteCharAt(0); 
	}
	else
	{
		// Almost surely a false positive ( start + stop + at least 1 character)
		throw NotFoundException.getNotFoundInstance();
	}

    var left:Number =  (start[1] + start[0]) / 2.0;
    var right:Number =  (nextStart + lastStart) / 2.0;
    return new Result(
        result.toString(),
        null,
        [new ResultPoint(left, Math.floor(rowNumber as Number)),new ResultPoint(right, Math.floor(rowNumber as Number))],
        BarcodeFormat.CODABAR);
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
    var counters:Array = new Array(7);
    for (var k:int = 0;k<counters.length;k++){counters[k]=0;}
    var patternStart:int = rowOffset;
    var isWhite:Boolean = false;
    var patternLength:int = counters.length;

    for (var i:int = rowOffset; i < width; i++) {
      var pixel:Boolean = row._get(i);
      if (pixel != isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          try {
            if (arrayContains(STARTEND_ENCODING, toNarrowWidePattern(counters))) {
              // Look for whitespace before start pattern, >= 50% of width of start pattern
              if (row.isRange(Math.max(0, patternStart - (i - patternStart) / 2), patternStart, false)) {
                return [patternStart, i];
              }
            }
          } catch (re:IllegalArgumentException ) {
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
        if (isWhite) {isWhite = false; } else { isWhite = true;} //  isWhite = !isWhite;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  private static function arrayContains(array:Array, key:String):Boolean {
    if (array != null) {
      for (var i:int = 0; i < array.length; i++) {
        if (array[i] == key) {
          return true;
        }
      }
    }
    return false;
  }

  private static function toNarrowWidePattern(counters:Array):String {
    // BAS : I have changed the following part because some codabar images would fail with the original routine
    //        I took from the Code39Reader.java file
    // ----------- change start
    var numCounters:int = counters.length;
    var maxNarrowCounter:int = 0;

    var minCounter:int = int.MAX_VALUE;
    for (var i:int = 0; i < numCounters; i++) {
      if (counters[i] < minCounter) {
        minCounter = counters[i];
      }
      if (counters[i] > maxNarrowCounter) {
        maxNarrowCounter = counters[i];
      }
    }
    // ---------- change end


    do {
      var wideCounters:int = 0;
      var pattern:int = 0;
      for (i = 0; i < numCounters; i++) {
        if (counters[i] > maxNarrowCounter) {
          pattern |= 1 << (numCounters - 1 - i);
          wideCounters++;
        }
      }

      if ((wideCounters == 2) || (wideCounters == 3)) {
        for (i = 0; i < CHARACTER_ENCODINGS.length; i++) {
          if (CHARACTER_ENCODINGS[i] == pattern) {
            return ALPHABET[i];
          }
        }
      }
      maxNarrowCounter--;
    } while (maxNarrowCounter > minCounter);
    return '!';
  }
}
}
