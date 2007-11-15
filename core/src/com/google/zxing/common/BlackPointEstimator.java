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

package com.google.zxing.common;

/**
 * <p>Encapsulates logic that estimates the optimal "black point", the luminance value
 * which is the best line between "white" and "black" in a grayscale image.</p>
 *
 * <p>For an interesting discussion of this issue, see
 * <a href="http://webdiis.unizar.es/~neira/12082/thresholding.pdf">http://webdiis.unizar.es/~neira/12082/thresholding.pdf</a>.
 * </p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class BlackPointEstimator {

  private BlackPointEstimator() {
  }

  /**
   * <p>Given an array of <em>counts</em> of luminance values (i.e. a histogram), this method
   * decides which bucket of values corresponds to the black point -- which bucket contains the
   * count of the brightest luminance values that should be considered "black".</p>
   *
   * @param luminanceBuckets an array of <em>counts</em> of luminance values
   * @return index within argument of bucket corresponding to brightest values which should be
   *  considered "black"
   */
  public static int estimate(int[] luminanceBuckets) {

    int numBuckets = luminanceBuckets.length;

    // Find tallest peak in histogram
    int firstPeak = 0;
    int firstPeakSize = 0;
    for (int i = 0; i < numBuckets; i++) {
      if (luminanceBuckets[i] > firstPeakSize) {
        firstPeak = i;
        firstPeakSize = luminanceBuckets[i];
      }
    }

    // Find second-tallest peak -- well, another peak that is tall and not
    // so close to the first one
    int secondPeak = 0;
    int secondPeakScore = 0;
    for (int i = 0; i < numBuckets; i++) {
      int distanceToBiggest = i - firstPeak;
      // Encourage more distant second peaks by multiplying by square of distance
      int score = luminanceBuckets[i] * distanceToBiggest * distanceToBiggest;
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

    // Find a valley between them that is low and close to the midpoint of the two peaks
    int bestValley = firstPeak;
    int bestValleyScore = 0;
    for (int i = firstPeak + 1; i < secondPeak; i++) {
      // Encourage low valleys near the mid point between peaks
      int score = (firstPeakSize - luminanceBuckets[i]) * (i - firstPeak) * (secondPeak - i);
      if (score > bestValleyScore) {
        bestValley = i;
        bestValleyScore = score;
      }
    }

    return bestValley;
  }

}