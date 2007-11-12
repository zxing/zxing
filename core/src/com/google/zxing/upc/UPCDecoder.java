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

import com.google.zxing.common.BitArray;
import com.google.zxing.MonochromeBitmapSource;

import java.util.Arrays;

/**
 * This class takes a bitmap, and attempts to return a String which is the contents of the UPC
 * barcode in the image. It should be scale-invariant, but does not make any corrections for
 * rotation or skew.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class UPCDecoder {
  UPCDecoder(MonochromeBitmapSource bitmap) {
	mBitmap = bitmap;
    if (bitmap != null) {
      mWidth = bitmap.getWidth();
      mHeight = bitmap.getHeight();
    }
  }

  // To decode the image, we follow a search pattern defined in kBitmapSearchPattern. It is a
  // list of percentages which translate to row numbers to scan across. For each row, we scan
  // left to right, and if that fails, we reverse the row in place and try again to see if the
  // bar code was upside down.
  public String decode() {
    if (mBitmap == null) return "";

    BitArray rowData = new BitArray(mWidth);
    String longestResult = "";
    int found = -1;
    for (int x = 0; x < kBitmapSearchPattern.length; x++) {
      int row = mHeight * kBitmapSearchPattern[x] / 100;
      mBitmap.getBlackRow(row, rowData, 0, mWidth);

      if (decodeRow(rowData)) {
        found = x;
        break;
      }
      //Log("decode: row " + row + " normal result: " + mResult);
      if (mResult.length() > longestResult.length()) {
        longestResult = mResult;
      }
      
      rowData.reverse();
      if (decodeRow(rowData)) {
        found = x;
        break;
      }
      //Log("decode: row " + row + " inverted result: " + mResult);
      if (mResult.length() > longestResult.length()) {
        longestResult = mResult;
      }
    }
    
    if (found >= 0) return mResult;
    else return "";
  }
  
  // UPC-A bar codes are made up of a left marker, six digits, a middle marker, six more digits,
  // and an end marker, reading from left to right. For more information, see:
  //
  // http://en.wikipedia.org/wiki/Universal_Product_Code
  //
  // TODO: Add support for UPC-E Zero Compressed bar codes.
  // TODO: Add support for EAN-13 (European Article Number) bar codes.
  // FIXME: Don't trust the first result from findPattern() for the start sequence - resume from
  // that spot and try to start again if finding digits fails.
  private boolean decodeRow(BitArray rowData) {
    mResult = "";
    int rowOffset = findPattern(rowData, 0, kStartEndPattern, false);
    if (rowOffset < 0) return false;
    //Log("Start pattern ends at column " + rowOffset);

    rowOffset = decodeOneSide(rowData, rowOffset);
    if (rowOffset < 0) return false;

    rowOffset = findPattern(rowData, rowOffset, kMiddlePattern, true);
    if (rowOffset < 0) return false;
    //Log("Middle pattern ends at column " + rowOffset);

    rowOffset = decodeOneSide(rowData, rowOffset);
    if (rowOffset < 0) return false;

    // We could attempt to read the end pattern for sanity, but there's not much point.
    // UPC-A codes have 12 digits, so any other result is garbage.
    return (mResult.length() == 12);
  }

  private int decodeOneSide(BitArray rowData, int rowOffset) {
    int[] counters = new int[4];
    for (int x = 0; x < 6 && rowOffset < mWidth; x++) {
      recordPattern(rowData, rowOffset, counters, 4);
      for (int y = 0; y < 4; y++) {
        rowOffset += counters[y];
      }
      char c = findDigit(counters);
      if (c == '-') {
        return -1;
      } else {
        mResult += c;
      }
    }
    return rowOffset;
  }

  // Returns the horizontal position just after the pattern was found if successful, otherwise
  // returns -1 if the pattern was not found. Searches are always left to right, and patterns
  // begin on white or black based on the flag.
  private int findPattern(BitArray rowData, int rowOffset, byte[] pattern, boolean whiteFirst) {
    int[] counters = new int[pattern.length];
    int width = mWidth;
    boolean isWhite = false;
    for (; rowOffset < width; rowOffset++) {
      isWhite = !rowData.get(rowOffset);
      if (whiteFirst == isWhite) {
        break;
      }
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

  // Records a pattern of alternating white and black pixels, returning an array of how many
  // pixels of each color were seen. The pattern begins immediately based on the color of the
  // first pixel encountered, so a patternSize of 3 could result in WBW or BWB.
  private void recordPattern(BitArray rowData, int rowOffset, int[] counters, int patternSize) {
    Arrays.fill(counters, 0);
    boolean isWhite = !rowData.get(rowOffset);

    int counterPosition = 0;
    int width = mWidth;
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

  // This is an optimized version of doesPatternMatch() which is specific to recognizing digits.
  // The average is divided by 7 because there are 7 bits per digit, even though the color only
  // alternates four times. kDigitPatterns has been premultiplied by 10 for efficiency. Notice
  // that the contents of the counters array are modified to save an extra allocation, so don't
  // use these values after returning from this call.
  // TODO: add EAN even parity support
  private char findDigit(int[] counters) {
    int total = counters[0] + counters[1] + counters[2] + counters[3];
    int average = total * 10 / 7;
    for (int x = 0; x < 4; x++) {
      counters[x] = counters[x] * 100 / average;
    }

    for (int x = 0; x < 10; x++) {
      boolean match = true;
      for (int y = 0; y < 4; y++) {
        int diff = counters[y] - kDigitPatterns[x][y];
        if (diff > kTolerance || diff < -kTolerance) {
          match = false;
          break;
        }
      }
      if (match) return kDigits[x];
    }
    return '-';
  }

  // Finds whether the given set of pixel counters matches the requested pattern. Taking an
  // average based on the number of counters offers some robustness when antialiased edges get
  // interpreted as the wrong color.
  // TODO: Remove the divide for performance.
  private boolean doesPatternMatch(int[] counters, byte[] pattern) {
    int total = 0;
    for (int x = 0; x < counters.length; x++) {
      total += counters[x];
    }
    int average = total * 10 / counters.length;

    for (int x = 0; x < counters.length; x++) {
      int scaledCounter = counters[x] * 100 / average;
      int scaledPattern = pattern[x] * 10;
      if (scaledCounter < scaledPattern - kTolerance || scaledCounter > scaledPattern + kTolerance) {
        return false;
      }
    }
    return true;
  }

  private static final byte[] kBitmapSearchPattern = { 50, 49, 51, 48, 52, 46, 54, 43, 57, 40, 60 };
  private static final byte[] kStartEndPattern = { 1, 1, 1 };
  private static final byte[] kMiddlePattern = { 1, 1, 1, 1, 1 };

  private static final byte[][] kDigitPatterns = {
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

  private static final char[] kDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
  private static final int kTolerance = 5;

  private MonochromeBitmapSource mBitmap;
  private int mWidth;
  private int mHeight;
  private String mResult;
}
