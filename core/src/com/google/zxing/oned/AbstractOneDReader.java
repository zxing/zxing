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
      if (tryHarder && image.isRotatedSupported()) {
        MonochromeBitmapSource rotatedImage = image.rotateCounterClockwise();
        return doDecode(rotatedImage, hints, tryHarder);        
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
    // that moving up and down by about 1/16 of the image is pretty good.
    int middle = height >> 1;
    int rowStep = Math.max(1, height >> 4);
    int maxLines = tryHarder ? 15 : 7;
    for (int x = 0; x < maxLines; x++) {

      int rowStepsAboveOrBelow = (x + 1) >> 1;
      boolean isAbove = (x & 0x01) == 0; // i.e. is x even?
      int rowNumber = middle + rowStep * (isAbove ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow);
      if (rowNumber < 0 || rowNumber >= height) {
        break;
      }

      image.estimateBlackPoint(BlackPointEstimationMethod.ROW_SAMPLING, rowNumber);
      image.getBlackRow(rowNumber, row, 0, width);

      try {
        return decodeRow(rowNumber, row, hints);
      } catch (ReaderException re) {
        if (tryHarder) {
          row.reverse(); // try scanning the row backwards
          try {
            return decodeRow(rowNumber, row, hints);
          } catch (ReaderException re2) {
            // continue
          }
        }
      }

    }

    throw new ReaderException("No barcode found");
  }

  protected static void recordPattern(BitArray row, int start, int[] counters) throws ReaderException {
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
  protected static float patternMatchVariance(int[] counters, int[] pattern) {
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

  /**
   * Fast round method.
   *
   * @return argument rounded to nearest int
   */
  protected static int round(float f) {
    return (int) (f + 0.5f);
  }

}
