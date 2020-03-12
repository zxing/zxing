// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
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

#include <zxing/common/Array.h>                     // for ArrayRef
#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/common/IllegalArgumentException.h>  // for IllegalArgumentException

#include "zxing/Binarizer.h"                        // for Binarizer
#include "zxing/LuminanceSource.h"                  // for LuminanceSource
#include "zxing/common/BitArray.h"                  // for BitArray
#include "zxing/common/BitMatrix.h"                 // for BitMatrix

#include <boost/assert.hpp>

#include <memory>

namespace pping {

using namespace std;

const int LUMINANCE_BITS = 5;
const int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
const int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

GlobalHistogramBinarizer::GlobalHistogramBinarizer(Ref<LuminanceSource> source) noexcept :
  Binarizer(source), cached_matrix_(nullptr), cached_row_(nullptr), cached_row_num_(-1) {

}

GlobalHistogramBinarizer::~GlobalHistogramBinarizer() = default;

FallibleRef<BitArray> GlobalHistogramBinarizer::getBlackRow(int y, Ref<BitArray> row) const MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (y == cached_row_num_) {
      BOOST_ASSERT_MSG(cached_row_ != nullptr, "Internal inconsistency: cached_row_num_ says there should be a cached row but there is none.");
      return cached_row_;
  }

  vector<int> histogram(LUMINANCE_BUCKETS, 0);
  LuminanceSource& source = *getLuminanceSource();
  int width = source.getWidth();
  if (row == nullptr || static_cast<int>(row->getSize()) < width) {
    row = new BitArray(width);
  } else {
    row->clear();
  }

  //TODO(flyashi): cache this instead of allocating and deleting per row
  auto const row_pixels( std::make_unique<unsigned char[]>( width ) );
  BOOST_VERIFY( source.getRow(y, row_pixels.get()) == row_pixels.get() );
  for (int x = 0; x < width; x++) {
      histogram[row_pixels[x] >> LUMINANCE_SHIFT]++;
  }

  auto blackPoint(estimate(histogram));
  if ( !blackPoint )
      return blackPoint.error();

  BitArray& array = *row;
  int left = row_pixels[0];
  int center = row_pixels[1];
  for (int x = 1; x < width - 1; x++) {
      int right = row_pixels[x + 1];
      // A simple -1 4 -1 box filter with a weight of 2.
      int luminance = ((center << 2) - left - right) >> 1;
      if (luminance < *blackPoint) {
      array.set(x);
      }
      left = center;
      center = right;
  }

  cached_row_ = row;
  cached_row_num_ = y;
  return row;
}

FallibleRef<BitMatrix> GlobalHistogramBinarizer::getBlackMatrix() const MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (cached_matrix_)
    return cached_matrix_;

  // Faster than working with the reference
  LuminanceSource& source = *getLuminanceSource();
  int width = source.getWidth();
  int height = source.getHeight();
  vector<int> histogram(LUMINANCE_BUCKETS, 0);

  // Quickly calculates the histogram by sampling four rows from the image.
  // This proved to be more robust on the blackbox tests than sampling a
  // diagonal as we used to do.
  ArrayRef<unsigned char> ref (width);
  unsigned char* row = &ref[0];
  for (int y = 1; y < 5; y++) {
    int rownum = height * y / 5;
    int right = (width << 2) / 5;
    row = source.getRow(rownum, row);
    for (int x = width / 5; x < right; x++) {
      histogram[row[x] >> LUMINANCE_SHIFT]++;
    }
  }

  auto const blackPoint(estimate(histogram));
  if (!blackPoint) 
    return blackPoint.error();

  Ref<BitMatrix> matrix_ref(new BitMatrix(width, height));
  BitMatrix& matrix = *matrix_ref;
  for (int y = 0; y < height; y++) {
    row = source.getRow(y, row);
    for (int x = 0; x < width; x++) {
      if (row[x] < *blackPoint)
        matrix.set(x, y);
    }
  }

  cached_matrix_ = matrix_ref;
  return matrix_ref;
}

Fallible<int> GlobalHistogramBinarizer::estimate(vector<int> &histogram) noexcept {
  int numBuckets = (int)histogram.size();
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
  if ( secondPeak - firstPeak <= numBuckets >> 4) {
    return failure<IllegalArgumentException>( "Too little dynamic range in luminance" ); //...FIXME...this is not an 'illegal argument exception'...
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

  return bestValley << LUMINANCE_SHIFT;
}

Ref<Binarizer> GlobalHistogramBinarizer::createBinarizer(Ref<LuminanceSource> source) const MB_NOEXCEPT_EXCEPT_BADALLOC {
  return Ref<Binarizer> (new GlobalHistogramBinarizer(source));
}

} // namespace zxing
