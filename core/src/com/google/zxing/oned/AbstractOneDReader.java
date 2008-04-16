/*
 * Copyright 2008 Google Inc.
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

import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.common.BitArray;

import java.util.Hashtable;

/**
 * <p>Encapsulates functionality and implementation that is common to all families
 * of one-dimensional barcodes.</p>
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author srowen@google.com (Sean Owen)
 */
public abstract class AbstractOneDReader implements OneDReader {

  public final Result decode(MonochromeBitmapSource image) throws ReaderException {
    return decode(image, null);
  }

  public final Result decode(MonochromeBitmapSource image, Hashtable hints) throws ReaderException {
    boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
    try {
      return doDecode(image, hints, tryHarder);
    } catch (ReaderException re) {
      if (tryHarder && image.isRotateSupported()) {
        MonochromeBitmapSource rotatedImage = image.rotateCounterClockwise();
        Result result = doDecode(rotatedImage, hints, tryHarder);
        // Record that we found it rotated 90 degrees CCW / 270 degrees CW
        Hashtable metadata = result.getResultMetadata();
        int orientation = 270;
        if (metadata != null && metadata.containsKey(ResultMetadataType.ORIENTATION)) {
          // But if we found it reversed in doDecode(), add in that result here:
          orientation = (orientation + ((Integer) metadata.get(ResultMetadataType.ORIENTATION)).intValue()) % 360;
        }
        result.putMetadata(ResultMetadataType.ORIENTATION, new Integer(orientation));
        return result;
      } else {
        throw re;
      }
    }
  }

  private Result doDecode(MonochromeBitmapSource image, Hashtable hints, boolean tryHarder) throws ReaderException {

    int width = image.getWidth();
    int height = image.getHeight();

    BitArray row = new BitArray(width);

    // We're going to examine rows from the middle outward, searching alternately above and below the middle,
    // and farther out each time. rowStep is the number of rows between each successive attempt above and below
    // the middle. So we'd scan row middle, then middle - rowStep, then middle + rowStep,
    // then middle - 2*rowStep, etc.
    // rowStep is bigger as the image is taller, but is always at least 1. We've somewhat arbitrarily decided
    // that moving up and down by about 1/16 of the image is pretty good; we try more of the image if
    // "trying harder"
    int middle = height >> 1;
    int rowStep = Math.max(1, height >> (tryHarder ? 7 : 4));
    int maxLines;
    if (tryHarder) {
      maxLines = height; // Look at the whole image; looking for more than one barcode
    } else {
      maxLines = 7;
    }

    for (int x = 0; x < maxLines; x++) {

      // Scanning from the middle out. Determine which row we're looking at next:
      int rowStepsAboveOrBelow = (x + 1) >> 1;
      boolean isAbove = (x & 0x01) == 0; // i.e. is x even?
      int rowNumber = middle + rowStep * (isAbove ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow);
      if (rowNumber < 0 || rowNumber >= height) {
        // Oops, if we run off the top or bottom, stop
        break;
      }

      // Estimate black point for this row and load it:
      try {
        image.estimateBlackPoint(BlackPointEstimationMethod.ROW_SAMPLING, rowNumber);
      } catch (ReaderException re) {
        continue;
      }
      image.getBlackRow(rowNumber, row, 0, width);

      // We may try twice for each row, if "trying harder":
      for (int attempt = 0; attempt < 2; attempt++) {
        if (attempt == 1) { // trying again?
          if (tryHarder) { // only if "trying harder"
            row.reverse(); // reverse the row and continue
          } else {
            break;
          }
        }
        try {
          // Look for a barcode
          Result result = decodeRow(rowNumber, row, hints);
          // We found our barcode
          if (attempt == 1) {
            // But it was upside down, so note that
            result.putMetadata(ResultMetadataType.ORIENTATION, new Integer(180));
          }
          return result;
        } catch (ReaderException re) {
          // continue -- just couldn't decode this row
        }
      }
    }

    throw new ReaderException("No barcode found");
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
  public static void recordPattern(BitArray row, int start, int[] counters) throws ReaderException {
    int numCounters = counters.length;
    for (int i = 0; i < numCounters; i++) {
      counters[i] = 0;
    }
    int end = row.getSize();
    if (start >= end) {
      throw new ReaderException("Couldn't fully read a pattern");
    }
    boolean isWhite = !row.get(start);
    int counterPosition = 0;
    int i = start;
    while (i < end) {
      boolean pixel = row.get(i);
      if ((!pixel && isWhite) || (pixel && !isWhite)) {
        counters[counterPosition]++;
      } else {
        counterPosition++;
        if (counterPosition == numCounters) {
          break;
        } else {
          counters[counterPosition] = 1;
          isWhite = !isWhite;
        }
      }
      i++;
    }
    // If we read fully the last section of pixels and filled up our counters -- or filled
    // the last counter but ran off the side of the image, OK. Otherwise, a problem.
    if (!(counterPosition == numCounters || (counterPosition == numCounters - 1 && i == end))) {
      throw new ReaderException("Couldn't fully read a pattern");
    }
  }

  /**
   * Determines how closely a set of observed counts of runs of black/white values matches a given
   * target pattern. This is reported as the ratio of the total variance from the expected pattern proportions
   * across all pattern elements, to the length of the pattern.
   *
   * @param counters observed counters
   * @param pattern expected pattern
   * @return average variance between counters and pattern
   */
  public static float patternMatchVariance(int[] counters, int[] pattern) {
    int total = 0;
    int numCounters = counters.length;
    int patternLength = 0;
    for (int i = 0; i < numCounters; i++) {
      total += counters[i];
      patternLength += pattern[i];
    }
    float unitBarWidth = (float) total / (float) patternLength;

    float totalVariance = 0.0f;
    for (int x = 0; x < numCounters; x++) {
      float scaledCounter = (float) counters[x] / unitBarWidth;
      float width = pattern[x];
      float abs = scaledCounter > width ? scaledCounter - width : width - scaledCounter;
      totalVariance += abs;
    }
    return totalVariance / (float) patternLength;
  }

  // This declaration should not be necessary, since this class is
  // abstract and so does not have to provide an implementation for every
  // method of an interface it implements, but it is causing NoSuchMethodError
  // issues on some Nokia JVMs. So we add this superfluous declaration:

  public abstract Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws ReaderException;

}
