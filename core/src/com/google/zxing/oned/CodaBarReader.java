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

package com.google.zxing.oned;

import java.util.Hashtable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;

/**
 * <p>Decodes Codabar barcodes.</p>
 *
 * @author Bas Vijfwinkel
 */
public final class CodaBarReader extends OneDReader {

  private static final String ALPHABET_STRING = "0123456789-$:/.+ABCDTN";
  private static final char[] ALPHABET = ALPHABET_STRING.toCharArray();

  /**
   * These represent the encodings of characters, as patterns of wide and narrow bars. The 7 least-significant bits of
   * each int correspond to the pattern of wide and narrow, with 1s representing "wide" and 0s representing narrow. NOTE
   * : c is equal to the  * pattern NOTE : d is equal to the e pattern
   */
  private static final int[] CHARACTER_ENCODINGS = {
      0x003, 0x006, 0x009, 0x060, 0x012, 0x042, 0x021, 0x024, 0x030, 0x048, // 0-9
      0x00c, 0x018, 0x025, 0x051, 0x054, 0x015, 0x01A, 0x029, 0x00B, 0x00E, // -$:/.+ABCD
      0x01A, 0x029 //TN
  };

  // minimal number of characters that should be present (inclusing start and stop characters)
  // this check has been added to reduce the number of false positive on other formats
  // until the cause for this behaviour has been determined
  // under normal circumstances this should be set to 3
  private static final int minCharacterLength = 6; 
  
  // multiple start/end patterns
  // official start and end patterns
  private static final char[] STARTEND_ENCODING = {'E', '*', 'A', 'B', 'C', 'D', 'T', 'N'};
  // some codabar generator allow the codabar string to be closed by every character
  //private static final char[] STARTEND_ENCODING = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '$', ':', '/', '.', '+', 'A', 'B', 'C', 'D', 'T', 'N'};
  
  // some industries use a checksum standard but this is not part of the original codabar standard
  // for more information see : http://www.mecsw.com/specs/codabar.html

  public Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws NotFoundException {
    int[] start = findAsteriskPattern(row);
    start[1] = 0; // BAS: settings this to 0 improves the recognition rate somehow?
    int nextStart = start[1];
    int end = row.getSize();

    // Read off white space
    while (nextStart < end && !row.get(nextStart)) {
      nextStart++;
    }

    StringBuffer result = new StringBuffer();
    //int[] counters = new int[7];
    int[] counters;
    int lastStart;

    do {
      counters = new int[]{0, 0, 0, 0, 0, 0, 0}; // reset counters
      recordPattern(row, nextStart, counters);

      char decodedChar = toNarrowWidePattern(counters);
      if (decodedChar == '!') {
        throw NotFoundException.getNotFoundInstance();
      }
      result.append(decodedChar);
      lastStart = nextStart;
      for (int i = 0; i < counters.length; i++) {
        nextStart += counters[i];
      }

      // Read off white space
      while (nextStart < end && !row.get(nextStart)) {
        nextStart++;
      }
    } while (nextStart < end); // no fixed end pattern so keep on reading while data is available

    // Look for whitespace after pattern:
    int lastPatternSize = 0;
    for (int i = 0; i < counters.length; i++) {
      lastPatternSize += counters[i];
    }

    int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;
    // If 50% of last pattern size, following last pattern, is not whitespace, fail
    // (but if it's whitespace to the very end of the image, that's OK)
    if ((nextStart) != end && (whiteSpaceAfterEnd / 2 < lastPatternSize)) {
      throw NotFoundException.getNotFoundInstance();
    }

	// valid result?
	if (result.length() < 2)
	{
		throw NotFoundException.getNotFoundInstance();
	}
	
	char startchar = result.charAt(0);
	if (!arrayContains(STARTEND_ENCODING, startchar))
	{
		//invalid start character
		throw NotFoundException.getNotFoundInstance();
	}
    
	// find stop character
    for (int k = 1;k < result.length() ;k++) 
	{
      if (result.charAt(k) == startchar) 
	  {
        // found stop character -> discard rest of the string
		if ((k+1) != result.length())
		{
			result.delete(k+1,result.length()-1);
			k = result.length();// break out of loop
		} 
	  }
    }

    // remove stop/start characters character and check if a string longer than 5 characters is contained
    if (result.length() > minCharacterLength) 
	{ 
		result.deleteCharAt(result.length()-1); 
		result.deleteCharAt(0); 
	}
	else
	{
		// Almost surely a false positive ( start + stop + at least 1 character)
		throw NotFoundException.getNotFoundInstance();
	}

    float left = (float) (start[1] + start[0]) / 2.0f;
    float right = (float) (nextStart + lastStart) / 2.0f;
    return new Result(
        result.toString(),
        null,
        new ResultPoint[]{
            new ResultPoint(left, (float) rowNumber),
            new ResultPoint(right, (float) rowNumber)},
        BarcodeFormat.CODABAR);
  }

  private static int[] findAsteriskPattern(BitArray row) throws NotFoundException {
    int width = row.getSize();
    int rowOffset = 0;
    while (rowOffset < width) {
      if (row.get(rowOffset)) {
        break;
      }
      rowOffset++;
    }

    int counterPosition = 0;
    int[] counters = new int[7];
    int patternStart = rowOffset;
    boolean isWhite = false;
    int patternLength = counters.length;

    for (int i = rowOffset; i < width; i++) {
      boolean pixel = row.get(i);
      if (pixel ^ isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          try {
            if (arrayContains(STARTEND_ENCODING, toNarrowWidePattern(counters))) {
              // Look for whitespace before start pattern, >= 50% of width of start pattern
              if (row.isRange(Math.max(0, patternStart - (i - patternStart) / 2), patternStart, false)) {
                return new int[]{patternStart, i};
              }
            }
          } catch (IllegalArgumentException re) {
            // no match, continue
          }
          patternStart += counters[0] + counters[1];
          for (int y = 2; y < patternLength; y++) {
            counters[y - 2] = counters[y];
          }
          counters[patternLength - 2] = 0;
          counters[patternLength - 1] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite ^= true; // isWhite = !isWhite;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  private static boolean arrayContains(char[] array, char key) {
    if (array != null) {
      for (int i = 0; i < array.length; i++) {
        if (array[i] == key) {
          return true;
        }
      }
    }
    return false;
  }

  private static char toNarrowWidePattern(int[] counters) {
    // BAS : I have changed the following part because some codabar images would fail with the original routine
    //        I took from the Code39Reader.java file
    // ----------- change start
    int numCounters = counters.length;
    int maxNarrowCounter = 0;

    int minCounter = Integer.MAX_VALUE;
    for (int i = 0; i < numCounters; i++) {
      if (counters[i] < minCounter) {
        minCounter = counters[i];
      }
      if (counters[i] > maxNarrowCounter) {
        maxNarrowCounter = counters[i];
      }
    }
    // ---------- change end


    do {
      int wideCounters = 0;
      int pattern = 0;
      for (int i = 0; i < numCounters; i++) {
        if (counters[i] > maxNarrowCounter) {
          pattern |= 1 << (numCounters - 1 - i);
          wideCounters++;
        }
      }

      if ((wideCounters == 2) || (wideCounters == 3)) {
        for (int i = 0; i < CHARACTER_ENCODINGS.length; i++) {
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
