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
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class BlackPointEstimator {

  private BlackPointEstimator() {
  }

  /**
   * <p>Given an array of <em>counts</em> of luminance values (i.e. a histogram), this method
   * decides which bucket of values corresponds to the black point -- which bucket contains the
   * count of the brightest luminance values that should be considered "black".</p>
   *
   * @param histogram an array of <em>counts</em> of luminance values
   * @param biasTowardsWhite values higher than 1.0 suggest that a higher black point is desirable (e.g.
   *  more values are considered black); less than 1.0 suggests that lower is desirable. Must be greater
   *  than 0.0; 1.0 is a good "default"
   * @return index within argument of bucket corresponding to brightest values which should be
   *  considered "black"
   */
  public static int estimate(int[] histogram, float biasTowardsWhite) {

	  if (Float.isNaN(biasTowardsWhite) || biasTowardsWhite <= 0.0f) {
		  throw new IllegalArgumentException("Illegal biasTowardsWhite: " + biasTowardsWhite);
	  }

    int numBuckets = histogram.length;

    // Find tallest peak in histogram
    int firstPeak = 0;
    int firstPeakSize = 0;
    for (int i = 0; i < numBuckets; i++) {
      if (histogram[i] > firstPeakSize) {
        firstPeak = i;
        firstPeakSize = histogram[i];
      }
    }

    // Find second-tallest peak -- well, another peak that is tall and not
    // so close to the first one
    int secondPeak = 0;
    int secondPeakScore = 0;
    for (int i = 0; i < numBuckets; i++) {
      int distanceToBiggest = i - firstPeak;
      // Encourage more distant second peaks by multiplying by square of distance
      int score = histogram[i] * distanceToBiggest * distanceToBiggest;
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

    // Find a valley between them that is low and closer to the white peak
    int bestValley = secondPeak - 1;
    int bestValleyScore = -1;
    for (int i = secondPeak - 1; i > firstPeak; i--) {
      int fromFirst = (int) (biasTowardsWhite * (i - firstPeak));
      // Favor a "valley" that is not too close to either peak -- especially not the black peak --
      // and that has a low value of course
      int score = fromFirst * fromFirst * (secondPeak - i) * (256 - histogram[i]);
      if (score > bestValleyScore) {
        bestValley = i;
        bestValleyScore = score;
      }
    }

    return bestValley;
  }

}