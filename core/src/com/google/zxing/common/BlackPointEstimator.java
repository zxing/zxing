/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.common;

import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;

/**
 * <p>Encapsulates logic that estimates the optimal "black point", the luminance value
 * which is the best line between "white" and "black" in a grayscale image.</p>
 *
 * <p>For an interesting discussion of this issue, see
 * <a href="http://webdiis.unizar.es/~neira/12082/thresholding.pdf">this paper</a>.</p>
 *
 * NOTE: This class is not thread-safe.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class BlackPointEstimator {

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

  private static int[] luminances = null;
  private static int[] histogram = null;

  private BlackPointEstimator() {
  }

  private static void initArrays(int luminanceSize) {
    if (luminances == null || luminances.length < luminanceSize) {
      luminances = new int[luminanceSize];
    }
    if (histogram == null) {
      histogram = new int[LUMINANCE_BUCKETS];
    } else {
      for (int x = 0; x < LUMINANCE_BUCKETS; x++) {
        histogram[x] = 0;
      }
    }
  }

  /**
   * Calculates the black point for the supplied bitmap.
   *
   * @param source The bitmap to analyze.
   * @param method The pixel sampling technique to use.
   * @param argument The row index in the case of ROW_SAMPLING, otherwise ignored.
   * @return The black point as an integer 0-255.
   * @throws ReaderException An exception thrown if the blackpoint cannot be determined.
   */
  public static int estimate(MonochromeBitmapSource source, BlackPointEstimationMethod method,
      int argument) throws ReaderException {
    int width = source.getWidth();
    int height = source.getHeight();
    initArrays(width);

    if (method.equals(BlackPointEstimationMethod.TWO_D_SAMPLING)) {
      // We used to sample a diagonal in the 2D case, but it missed a lot of pixels, and it required
      // n calls to getLuminance(). We had a net improvement of 63 blackbox tests decoded by
      // sampling several rows from the middle of the image, using getLuminanceRow(). We read more
      // pixels total, but with fewer function calls, and more continguous memory.
      for (int y = 1; y < 5; y++) {
        int row = height * y / 5;
        int[] localLuminances = source.getLuminanceRow(row, luminances);
        int right = width * 4 / 5;
        for (int x = width / 5; x < right; x++) {
          histogram[localLuminances[x] >> LUMINANCE_SHIFT]++;
        }
      }
    } else if (method.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
      if (argument < 0 || argument >= height) {
        throw new IllegalArgumentException("Row is not within the image: " + argument);
      }

      int[] localLuminances = source.getLuminanceRow(argument, luminances);
      for (int x = 0; x < width; x++) {
        histogram[localLuminances[x] >> LUMINANCE_SHIFT]++;
      }
    } else {
      throw new IllegalArgumentException("Unknown method");
    }
    return findBestValley(histogram) << LUMINANCE_SHIFT;
  }

  /**
   * <p>Given an array of <em>counts</em> of luminance values (i.e. a histogram), this method
   * decides which bucket of values corresponds to the black point -- which bucket contains the
   * count of the brightest luminance values that should be considered "black".</p>
   *
   * @param buckets an array of <em>counts</em> of luminance values
   * @return index within argument of bucket corresponding to brightest values which should be
   *         considered "black"
   * @throws ReaderException if "black" and "white" appear to be very close in luminance
   */
  public static int findBestValley(int[] buckets) throws ReaderException {
    int numBuckets = buckets.length;
    int maxBucketCount = 0;
    // Find tallest peak in histogram
    int firstPeak = 0;
    int firstPeakSize = 0;
    for (int i = 0; i < numBuckets; i++) {
      if (buckets[i] > firstPeakSize) {
        firstPeak = i;
        firstPeakSize = buckets[i];
      }
      if (buckets[i] > maxBucketCount) {
        maxBucketCount = buckets[i];
      }
    }

    // Find second-tallest peak -- well, another peak that is tall and not
    // so close to the first one
    int secondPeak = 0;
    int secondPeakScore = 0;
    for (int i = 0; i < numBuckets; i++) {
      int distanceToBiggest = i - firstPeak;
      // Encourage more distant second peaks by multiplying by square of distance
      int score = buckets[i] * distanceToBiggest * distanceToBiggest;
      if (score > secondPeakScore) {
        secondPeak = i;
        secondPeakScore = score;
      }
    }

    // Put firstPeak first
    if (firstPeak > secondPeak) {
      int temp = firstPeak;
      firstPeak = secondPeak;
      secondPeak = temp;
    }

    // Kind of arbitrary; if the two peaks are very close, then we figure there is so little
    // dynamic range in the image, that discriminating black and white is too error-prone.
    // Decoding the image/line is either pointless, or may in some cases lead to a false positive
    // for 1D formats, which are relatively lenient.
    // We arbitrarily say "close" is "<= 1/16 of the total histogram buckets apart"
    if (secondPeak - firstPeak <= numBuckets >> 4) {
      throw ReaderException.getInstance();
    }

    // Find a valley between them that is low and closer to the white peak
    int bestValley = secondPeak - 1;
    int bestValleyScore = -1;
    for (int i = secondPeak - 1; i > firstPeak; i--) {
      int fromFirst = i - firstPeak;
      // Favor a "valley" that is not too close to either peak -- especially not the black peak --
      // and that has a low value of course
      int score = fromFirst * fromFirst * (secondPeak - i) * (maxBucketCount - buckets[i]);
      if (score > bestValleyScore) {
        bestValley = i;
        bestValleyScore = score;
      }
    }

    return bestValley;
  }

}