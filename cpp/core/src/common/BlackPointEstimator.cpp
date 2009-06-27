/*
 *  BlackPointEstimator.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 12/05/2008.
 *  Copyright 2008 Google UK. All rights reserved.
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

#include "BlackPointEstimator.h"
#include "IllegalArgumentException.h"

using namespace std;

namespace common {
  
  size_t BlackPointEstimator::estimate(valarray<int> &histogram) {
    
    size_t numBuckets = histogram.size();
    int maxBucketCount = 0;
    
    // Find tallest peak in histogram
    size_t firstPeak = 0;
    int firstPeakSize = 0;
    for (size_t i = 0; i < numBuckets; i++) {
      if (histogram[i] > firstPeakSize) {
        firstPeak = i;
        firstPeakSize = histogram[i];
      }
      if (histogram[i] > maxBucketCount) {
        maxBucketCount = histogram[i];
      }
    }
    
    // Find second-tallest peak -- well, another peak that is tall and not
    // so close to the first one
    size_t secondPeak = 0;
    int secondPeakScore = 0;
    for (size_t i = 0; i < numBuckets; i++) {
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
    
    // Kind of aribtrary; if the two peaks are very close, then we figure there is so little
    // dynamic range in the image, that discriminating black and white is too error-prone.
    // Decoding the image/line is either pointless, or may in some cases lead to a false positive
    // for 1D formats, which are relatively lenient.
    // We arbitrarily say "close" is "<= 1/16 of the total histogram buckets apart"
    if (secondPeak - firstPeak <= numBuckets >> 4) {
      throw IllegalArgumentException
        ("Too little dynamic range in luminance");
    }
    
    // Find a valley between them that is low and closer to the white peak
    size_t bestValley = secondPeak - 1;
    int bestValleyScore = -1;
    for (size_t i = secondPeak - 1; i > firstPeak; i--) {
      int fromFirst = i - firstPeak;
      // Favor a "valley" that is not too close to either peak -- especially not the black peak --
      // and that has a low value of course
      int score = fromFirst * fromFirst * (secondPeak - i) * (maxBucketCount - histogram[i]);
      if (score > bestValleyScore) {
        bestValley = i;
        bestValleyScore = score;
      }
    }
    
    return bestValley;
  }

}
