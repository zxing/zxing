/*
 *  GlobalHistogramBinarizer.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 16/10/2009.
 *  Copyright 2008 ZXing authors All rights reserved.
 *  Modified by Lukasz Warchol on 02/02/2010.
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

#include <zxing/common/GlobalHistogramBinarizer.h>

#include <zxing/common/IllegalArgumentException.h>

namespace zxing {
using namespace std;

const int LUMINANCE_BITS = 5;
const int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
const int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

GlobalHistogramBinarizer::GlobalHistogramBinarizer(Ref<LuminanceSource> source) :
  Binarizer(source) {

}

GlobalHistogramBinarizer::~GlobalHistogramBinarizer() {
}


Ref<BitArray> GlobalHistogramBinarizer::estimateBlackRow(int y,
  Ref<BitArray> row){
  vector<int> histogram(LUMINANCE_BUCKETS, 0);
  LuminanceSource& source = *getSource();
  int width = source.getWidth();
  if (row == NULL || static_cast<int>(row->getSize()) < width) {
    row = new BitArray(width);
  } else {
    row->clear();
  }

  for (int x = 0; x < width; x++) {
    unsigned char pixel = source.getPixel(x, y);
    histogram[pixel >> LUMINANCE_SHIFT]++;
  }
  int blackPoint = estimate(histogram) << LUMINANCE_SHIFT;


  Ref<BitArray> array_ref(new BitArray(width));
  BitArray& array = *array_ref;

  int left = source.getPixel(0, y);
  int center = source.getPixel(1, y);
  for (int x = 1; x < width - 1; x++) {
    int right = source.getPixel(x+1, y);
    // A simple -1 4 -1 box filter with a weight of 2.
    int luminance = ((center << 2) - left - right) >> 1;
    if (luminance < blackPoint) {
      array.set(x);
    }
    left = center;
    center = right;
  }

  return array_ref;
}

Ref<BitMatrix> GlobalHistogramBinarizer::estimateBlackMatrix() {
  // Faster than working with the reference
  LuminanceSource& source = *getSource();
  int width = source.getWidth();
  int height = source.getHeight();
  vector<int> histogram(LUMINANCE_BUCKETS, 0);


  // Quickly calculates the histogram by sampling four rows from the image.
  // This proved to be more robust on the blackbox tests than sampling a
  // diagonal as we used to do.
  for (int y = 1; y < 5; y++) {
    int row = height * y / 5;
    int right = (width << 2) / 5;
    int sdf;
    for (int x = width / 5; x < right; x++) {
      unsigned char pixel = source.getPixel(x, row);
      histogram[pixel >> LUMINANCE_SHIFT]++;
      sdf = histogram[pixel >> LUMINANCE_SHIFT];
    }
  }

  int blackPoint = estimate(histogram) << LUMINANCE_SHIFT;

  Ref<BitMatrix> matrix_ref(new BitMatrix(width, height));
  BitMatrix& matrix = *matrix_ref;
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      if (source.getPixel(x, y) <= blackPoint)
        matrix.set(x, y);
    }
  }
  return matrix_ref;
}

int GlobalHistogramBinarizer::estimate(vector<int> &histogram) {
  int numBuckets = histogram.size();
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

  // Kind of arbitrary; if the two peaks are very close, then we figure there is
  // so little dynamic range in the image, that discriminating black and white
  // is too error-prone.
  // Decoding the image/line is either pointless, or may in some cases lead to
  // a false positive for 1D formats, which are relatively lenient.
  // We arbitrarily say "close" is
  // "<= 1/16 of the total histogram buckets apart"
  if (secondPeak - firstPeak <= numBuckets >> 4) {
    throw IllegalArgumentException("Too little dynamic range in luminance");
  }

  // Find a valley between them that is low and closer to the white peak
  int bestValley = secondPeak - 1;
  int bestValleyScore = -1;
  for (int i = secondPeak - 1; i > firstPeak; i--) {
    int fromFirst = i - firstPeak;
    // Favor a "valley" that is not too close to either peak -- especially not
    // the black peak -- and that has a low value of course
    int score = fromFirst * fromFirst * (secondPeak - i) *
      (maxBucketCount - histogram[i]);
    if (score > bestValleyScore) {
      bestValley = i;
      bestValleyScore = score;
    }
  }

  return bestValley;
}

} // namespace zxing

