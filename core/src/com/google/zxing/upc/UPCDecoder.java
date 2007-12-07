/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.upc;

import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.common.BitArray;

/**
 * This class takes a bitmap, and attempts to return a String which is the contents of the UPC
 * barcode in the image. It should be scale-invariant, but does not make any corrections for
 * rotation or skew.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class UPCDecoder {

  private static final byte[] BITMAP_SEARCH_PATTERN = { 50, 49, 51, 48, 52, 46, 54, 43, 57, 40, 60 };
  private static final byte[] START_END_PATTERN = { 1, 1, 1 };
  private static final byte[] MIDDLE_PATTERN = { 1, 1, 1, 1, 1 };
  private static final byte[][] DIGIT_PATTERNS = {
    { 30, 20, 10, 10 }, // 0
    { 20, 20, 20, 10 }, // 1
    { 20, 10, 20, 20 }, // 2
    { 10, 40, 10, 10 }, // 3
    { 10, 10, 30, 20 }, // 4
    { 10, 20, 30, 10 }, // 5
    { 10, 10, 10, 40 }, // 6
    { 10, 30, 10, 20 }, // 7
    { 10, 20, 10, 30 }, // 8
    { 30, 10, 10, 20 }  // 9
  };

  /** Alternative even-parity patterns for EAN-13 barcodes. */
  private static final byte[][] EVEN_PARITY_PATTERNS = {
    { 10, 10, 20, 30 }, // 0
    { 10, 20, 20, 20 }, // 1
    { 20, 20, 10, 20 }, // 2
    { 10, 10, 40, 10 }, // 3
    { 20, 30, 10, 10 }, // 4
    { 10, 30, 20, 10 }, // 5
    { 40, 10, 10, 10 }, // 6
    { 20, 10, 30, 10 }, // 7
    { 30, 10, 20, 10 }, // 8
    { 20, 10, 10, 30 }  // 9
  };

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
  private static final byte[] FIRST_DIGIT_ENCODINGS = {
    0x00, 0x0B, 0x0D, 0xE, 0x13, 0x19, 0x1C, 0x15, 0x16, 0x1A
  };


  // Parity types indicating how a given digit is encoded
  private static final int UNKNOWN_PARITY  = 0;
  private static final int ODD_PARITY      = 1;
  private static final int EVEN_PARITY     = 2;

  /**
   * Utility class for returning a matched character. Defines the character
   * plus the parity used for encoding it.
   */
  private static class CharResult {
    public char character;   // the encoded character
    public int parity;       // one of the parity types above
  }

  private static final int TOLERANCE = 5;

  private MonochromeBitmapSource bitmap;
  private int width;
  private int height;
  private StringBuffer result;

  UPCDecoder(MonochromeBitmapSource bitmap) {
	  this.bitmap = bitmap;
    width = bitmap.getWidth();
    height = bitmap.getHeight();
  }

  /**
   * To decode the image, we follow a search pattern defined in kBitmapSearchPattern. It is a
   * list of percentages which translate to row numbers to scan across. For each row, we scan
   * left to right, and if that fails, we reverse the row in place and try again to see if the
   * bar code was upside down.
   */
  String decode() {
    BitArray rowData = new BitArray(width);
    String longestResult = "";
    int found = -1;
    for (int x = 0; x < BITMAP_SEARCH_PATTERN.length; x++) {
      int row = height * BITMAP_SEARCH_PATTERN[x] / 100;
      bitmap.estimateBlackPoint(BlackPointEstimationMethod.ROW_SAMPLING, row);
      bitmap.getBlackRow(row, rowData, 0, width);

      if (decodeRow(rowData)) {
        found = x;
        break;
      }
      //Log("decode: row " + row + " normal result: " + result);
      if (result.length() > longestResult.length()) {
        longestResult = result.toString();
      }
      
      rowData.reverse();
      if (decodeRow(rowData)) {
        found = x;
        break;
      }
      //Log("decode: row " + row + " inverted result: " + result);
      if (result.length() > longestResult.length()) {
        longestResult = result.toString();
      }
    }
    
    if (found >= 0) {
      return result.toString();
    } else {
      return "";
    }
  }
  
  /**
   * UPC-A bar codes are made up of a left marker, six digits, a middle marker, six more digits,
   * and an end marker, reading from left to right. For more information, see:
   * <a href="http://en.wikipedia.org/wiki/Universal_Product_Code">
   * http://en.wikipedia.org/wiki/Universal_Product_Code</a>
   */
  private boolean decodeRow(BitArray rowData) {
    // TODO: Add support for UPC-E Zero Compressed bar codes.
    // FIXME: Don't trust the first result from findPattern() for the start sequence - resume from
    // that spot and try to start again if finding digits fails.
    result = new StringBuffer();
    int rowOffset = findPattern(rowData, 0, START_END_PATTERN, false);
    if (rowOffset < 0) {
      return false;
    }
    //Log("Start pattern ends at column " + rowOffset);

    rowOffset = decodeOneSide(rowData, rowOffset, true);
    if (rowOffset < 0) {
      return false;
    }

    rowOffset = findPattern(rowData, rowOffset, MIDDLE_PATTERN, true);
    if (rowOffset < 0) {
      return false;
    }
    //Log("Middle pattern ends at column " + rowOffset);

    // Pass in false for checkBothParities(). For an EAN-13 barcode, only the
    // left had side will use mixed parities.
    rowOffset = decodeOneSide(rowData, rowOffset, false);
    if (rowOffset < 0) {
      return false;
    }
    return verifyResult();
  }


  /**
   * Verifies the checksum. This is computed by adding up digits in the even
   * indices (0, 2, 4...) then adding the digits in the odd indices (1, 3, 5..)
   * and multiplying by 3. The total, plus the final checksum digit, should be
   * divisible by 10.
   *
   * Note that for a UPC barcode, we add the additional '0' to the front
   * (converting it to a EAN-13 code) for purposes of calculating the checksum
   */
  private boolean verifyResult() {
    // TODO - handle compressed barcodes.

    // length is 12 for UPC and 13 for EAN-13
    if (result.length() != 12 && result.length() != 13) {
      return false;
    }
    
    int checksum = 0;
    int end = result.length() - 2;
    int factor = 3;
    // Calculate from penultimate digit down to first. This avoids having to
    // account for the optional '0' on the front, which won't actually affect
    // the calculation.
    for (int i = end; i >= 0; i--) {
      int value = (result.charAt(i) - (int)'0') * factor;
      checksum += value;
      factor = factor == 3 ? 1 : 3;
    }
    int endValue = (result.charAt(end + 1) - (int)'0');
    //Log("checksum + endValue = " + (checksum + endValue));
    return (checksum + endValue) % 10 == 0;
  }

  private int decodeOneSide(BitArray rowData, int rowOffset, boolean checkBothParities) {
    int[] counters = new int[4];
    byte firstDigitPattern = 0;
    CharResult foundChar = new CharResult();
    for (int x = 0; x < 6 && rowOffset < width; x++) {
      recordPattern(rowData, rowOffset, counters, 4);
      for (int y = 0; y < 4; y++) {
        rowOffset += counters[y];
      }
      findDigit(counters, foundChar, checkBothParities);
      if (foundChar.parity == UNKNOWN_PARITY) {
        return -1;
      }
      if (foundChar.parity == EVEN_PARITY) {
        firstDigitPattern |= 1 << (5 - x);
      }
      result.append(foundChar.character);
    }
    // If checkBothParities is true then we're potentially looking at the left
    // hand side of an EAN-13 barcode, where the first digit is encoded by the
    // parity pattern. In that case, calculate the first digit by checking
    // the parity patterns.
    if (checkBothParities) {
      char firstDigit = '-';
      for (int i = 0; i < FIRST_DIGIT_ENCODINGS.length; i++) {
        if (firstDigitPattern == FIRST_DIGIT_ENCODINGS[i]) {
          firstDigit = (char)((int)'0' + i);
          break;
        }
      }
      if (firstDigit == '-') {
        return -1;
      }
      if (firstDigit != '0') {
        result.insert(0, firstDigit);
      }
    }
    return rowOffset;
  }

  /**
   * Returns the horizontal position just after the pattern was found if successful, otherwise
   * returns -1 if the pattern was not found. Searches are always left to right, and patterns
   * begin on white or black based on the flag.
   */
  private int findPattern(BitArray rowData, int rowOffset, byte[] pattern, boolean whiteFirst) {
    int[] counters = new int[pattern.length];
    int width = this.width;
    boolean isWhite = false;
    while (rowOffset < width) {
      isWhite = !rowData.get(rowOffset);
      if (whiteFirst == isWhite) {
        break;
      }
      rowOffset++;
    }

    int counterPosition = 0;
    for (int x = rowOffset; x < width; x++) {
      boolean pixel = rowData.get(x);
      if ((!pixel && isWhite) || (pixel && !isWhite)) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == pattern.length - 1) {
          if (doesPatternMatch(counters, pattern)) {
            return x;
          }
          for (int y = 2; y < pattern.length; y++) {
            counters[y - 2] = counters[y];
          }
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    return -1;
  }

  /**
   * Records a pattern of alternating white and black pixels, returning an array of how many
   * pixels of each color were seen. The pattern begins immediately based on the color of the
   * first pixel encountered, so a patternSize of 3 could result in WBW or BWB.
   */
  private void recordPattern(BitArray rowData, int rowOffset, int[] counters, int patternSize) {
    for (int i = 0; i < counters.length; i++) {
      counters[i] = 0;
    }
    boolean isWhite = !rowData.get(rowOffset);

    int counterPosition = 0;
    int width = this.width;
    for (int x = rowOffset; x < width; x++) {
      boolean pixel = rowData.get(x);
      if ((!pixel && isWhite) || (pixel && !isWhite)) {
        counters[counterPosition]++;
      } else {
        counterPosition++;
        if (counterPosition == patternSize) {
          return;
        } else {
          counters[counterPosition] = 1;
          isWhite = !isWhite;
        }
      }
    }
  }

  /**
   * This is an optimized version of doesPatternMatch() which is specific to recognizing digits.
   * The average is divided by 7 because there are 7 bits per digit, even though the color only
   * alternates four times. kDigitPatterns has been premultiplied by 10 for efficiency. Notice
   * that the contents of the counters array are modified to save an extra allocation, so don't
   * use these values after returning from this call.
   */
  private static void findDigit(int[] counters, CharResult result, boolean checkBothParities) {
    result.parity = UNKNOWN_PARITY;
    int total = counters[0] + counters[1] + counters[2] + counters[3];
    int average = total * 10 / 7;
    for (int x = 0; x < 4; x++) {
      counters[x] = counters[x] * 100 / average;
    }

    for (int x = 0; x < 10; x++) {
      boolean match = true;
      for (int y = 0; y < 4; y++) {
        int diff = counters[y] - DIGIT_PATTERNS[x][y];
        if (diff > TOLERANCE || diff < -TOLERANCE) {
          match = false;
          break;
        }
      }
      if (match) {
        result.parity = ODD_PARITY;
        result.character =  (char)((int)'0' + x);
        return;
      }
    }
    // If first pattern didn't match, look for even parity patterns, as used in
    // EAN-13 barcodes.
    if (checkBothParities) {
      for (int x = 0; x < 10; x++) {
        boolean match = true;
        for (int y = 0; y < 4; y++) {
          int diff = counters[y] - EVEN_PARITY_PATTERNS[x][y];
          if (diff > TOLERANCE || diff < -TOLERANCE) {
            match = false;
            break;
          }
        }
        if (match) {
          result.parity = EVEN_PARITY;
          result.character =  (char)((int)'0' + x);
          return;
        }
      }
    }
  }

  /**
   * Finds whether the given set of pixel counters matches the requested pattern. Taking an
   * average based on the number of counters offers some robustness when antialiased edges get
   * interpreted as the wrong color.
   */
  private static boolean doesPatternMatch(int[] counters, byte[] pattern) {
    // TODO: Remove the divide for performance.
    int total = 0;
    int numCounters = counters.length;
    for (int x = 0; x < numCounters; x++) {
      total += counters[x];
    }
    int average = total * 10 / counters.length;

    for (int x = 0; x < numCounters; x++) {
      int scaledCounter = counters[x] * 100 / average;
      int scaledPattern = pattern[x] * 10;
      if (scaledCounter < scaledPattern - TOLERANCE || scaledCounter > scaledPattern + TOLERANCE) {
        return false;
      }
    }
    return true;
  }

}
