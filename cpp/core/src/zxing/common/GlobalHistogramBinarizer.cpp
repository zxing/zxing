/*
 *  GlobalHistogramBinarizer.cpp
 *  zxing
 *
 *  Copyright 2010 ZXing authors. All rights reserved.
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
  Binarizer(source), cached_matrix_(NULL), cached_row_(NULL), cached_row_num_(-1) {

}

GlobalHistogramBinarizer::~GlobalHistogramBinarizer() {
}


Ref<BitArray> GlobalHistogramBinarizer::getBlackRow(int y, Ref<BitArray> row) {
  if (y == cached_row_num_) {
    if (cached_row_ != NULL) {
      return cached_row_;
    } else {
      throw IllegalArgumentException("Too little dynamic range in luminance");
    }
  }

  vector<int> histogram(LUMINANCE_BUCKETS, 0);
  LuminanceSource& source = *getLuminanceSource();
  int width = source.getWidth();
  if (row == NULL || static_cast<int>(row->getSize()) < width) {
    row = new BitArray(width);
  } else {
    row->clear();
  }

  //TODO(flyashi): cache this instead of allocating and deleting per row
  unsigned char* row_pixels = NULL;
  try {
    row_pixels = new unsigned char[width];
    row_pixels = source.getRow(y, row_pixels);
    for (int x = 0; x < width; x++) {
      histogram[row_pixels[x] >> LUMINANCE_SHIFT]++;
    }
    int blackPoint = estimate(histogram) << LUMINANCE_SHIFT;

    BitArray& array = *row;
    int left = row_pixels[0];
    int center = row_pixels[1];
    for (int x = 1; x < width - 1; x++) {
      int right = row_pixels[x + 1];
      // A simple -1 4 -1 box filter with a weight of 2.
      int luminance = ((center << 2) - left - right) >> 1;
      if (luminance < blackPoint) {
        array.set(x);
      }
      left = center;
      center = right;
    }

    cached_row_ = row;
    cached_row_num_ = y;
    delete [] row_pixels;
    return row;
  } catch (IllegalArgumentException const& iae) {
    // Cache the fact that this row failed.
    cached_row_ = NULL;
    cached_row_num_ = y;
    delete [] row_pixels;
    throw iae;
  }
}

Ref<BitMatrix> GlobalHistogramBinarizer::getBlackMatrix() {
  if (cached_matrix_ != NULL) {
    return cached_matrix_;
  }

  // Faster than working with the reference
  LuminanceSource& source = *getLuminanceSource();
  int width = source.getWidth();
  int height = source.getHeight();
  vector<int> histogram(LUMINANCE_BUCKETS, 0);

  // Quickly calculates the histogram by sampling four rows from the image.
  // This proved to be more robust on the blackbox tests than sampling a
  // diagonal as we used to do.
  unsigned char* row = new unsigned char[width];
  for (int y = 1; y < 5; y++) {
    int rownum = height * y / 5;
    int right = (width << 2) / 5;
    int sdf;
    row = source.getRow(rownum, row);
    for (int x = width / 5; x < right; x++) {
      histogram[row[x] >> LUMINANCE_SHIFT]++;
      sdf = histogram[row[x] >> LUMINANCE_SHIFT];
    }
  }

  int blackPoint = estimate(histogram) << LUMINANCE_SHIFT;

  Ref<BitMatrix> matrix_ref(new BitMatrix(width, height));
  BitMatrix& matrix = *matrix_ref;
  for (int y = 0; y < height; y++) {
    row = source.getRow(y, row);
    for (int x = 0; x < width; x++) {
      if (row[x] <= blackPoint)
        matrix.set(x, y);
    }
  }

  cached_matrix_ = matrix_ref;
  delete [] row;
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

Ref<Binarizer> GlobalHistogramBinarizer::createBinarizer(Ref<LuminanceSource> source) {
  return Ref<Binarizer> (new GlobalHistogramBinarizer(source));
}

} // namespace zxing
