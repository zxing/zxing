/*
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

using System;
namespace com.google.zxing.common
{

    /// <summary> <p>Encapsulates logic that estimates the optimal "black point", the luminance value
    /// which is the best line between "white" and "black" in a grayscale image.</p>
    /// 
    /// <p>For an interesting discussion of this issue, see
    /// <a href="http://webdiis.unizar.es/~neira/12082/thresholding.pdf">http://webdiis.unizar.es/~neira/12082/thresholding.pdf</a>.
    /// </p>
    /// 
    /// </summary>
    /// <author>  srowen@google.com (Sean Owen)
    /// </author>
    /// <author>  dswitkin@google.com (Daniel Switkin)
    /// </author>
    public sealed class BlackPointEstimator
    { 
          private BlackPointEstimator() 
          {
          }

          /**
           * <p>Given an array of <em>counts</em> of luminance values (i.e. a histogram), this method
           * decides which bucket of values corresponds to the black point -- which bucket contains the
           * count of the brightest luminance values that should be considered "black".</p>
           *
           * @param histogram an array of <em>counts</em> of luminance values
           * @return index within argument of bucket corresponding to brightest values which should be
           *         considered "black"
           * @throws ReaderException if "black" and "white" appear to be very close in luminance in the image
           */
          public static int estimate(int[] histogram)
          {
            try{
          
                int numBuckets = histogram.Length;
                int maxBucketCount = 0;
                // Find tallest peak in histogram
                int firstPeak = 0;
                int firstPeakSize = 0;
                for (int i = 0; i < numBuckets; i++) {
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

                // Kind of aribtrary; if the two peaks are very close, then we figure there is so little
                // dynamic range in the image, that discriminating black and white is too error-prone.
                // Decoding the image/line is either pointless, or may in some cases lead to a false positive
                // for 1D formats, which are relatively lenient.
                // We arbitrarily say "close" is "<= 1/16 of the total histogram buckets apart"
                if (secondPeak - firstPeak <= numBuckets >> 4) {
                  throw new ReaderException("");
                }

                // Find a valley between them that is low and closer to the white peak
                int bestValley = secondPeak - 1;
                int bestValleyScore = -1;
                for (int i = secondPeak - 1; i > firstPeak; i--) {
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
            catch (Exception e)
            {
                throw (ReaderException) e; 
            }
          }
    }
}