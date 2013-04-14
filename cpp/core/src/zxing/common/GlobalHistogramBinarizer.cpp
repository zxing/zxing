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

#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/NotFoundException.h>
#include <zxing/common/Array.h>

using zxing::GlobalHistogramBinarizer;
using zxing::Binarizer;
using zxing::ArrayRef;
using zxing::Ref;
using zxing::BitArray;
using zxing::BitMatrix;

// VC++
using zxing::LuminanceSource;

namespace {
  const int LUMINANCE_BITS = 5;
  const int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  const int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;
  const ArrayRef<char> EMPTY (0);
}

GlobalHistogramBinarizer::GlobalHistogramBinarizer(Ref<LuminanceSource> source) 
  : Binarizer(source), luminances(EMPTY), buckets(LUMINANCE_BUCKETS) {}

GlobalHistogramBinarizer::~GlobalHistogramBinarizer() {}

void GlobalHistogramBinarizer::initArrays(int luminanceSize) {
  if (luminances->size() < luminanceSize) {
    luminances = ArrayRef<char>(luminanceSize);
  }
  for (int x = 0; x < LUMINANCE_BUCKETS; x++) {
    buckets[x] = 0;
  }
}

Ref<BitArray> GlobalHistogramBinarizer::getBlackRow(int y, Ref<BitArray> row) {
  // std::cerr << "gbr " << y << std::endl;
  LuminanceSource& source = *getLuminanceSource();
  int width = source.getWidth();
  if (row == NULL || static_cast<int>(row->getSize()) < width) {
    row = new BitArray(width);
  } else {
    row->clear();
  }

  initArrays(width);
  ArrayRef<char> localLuminances = source.getRow(y, luminances);
  if (false) {
    std::cerr << "gbr " << y << " r ";
    for(int i=0, e=localLuminances->size(); i < e; ++i) {
      std::cerr << 0+localLuminances[i] << " ";
    }
    std::cerr << std::endl;
  }
  ArrayRef<int> localBuckets = buckets;
  for (int x = 0; x < width; x++) {
    int pixel = localLuminances[x] & 0xff;
    localBuckets[pixel >> LUMINANCE_SHIFT]++;
  }
  int blackPoint = estimateBlackPoint(localBuckets);
  // std::cerr << "gbr bp " << y << " " << blackPoint << std::endl;

  int left = localLuminances[0] & 0xff;
  int center = localLuminances[1] & 0xff;
  for (int x = 1; x < width - 1; x++) {
    int right = localLuminances[x + 1] & 0xff;
    // A simple -1 4 -1 box filter with a weight of 2.
    int luminance = ((center << 2) - left - right) >> 1;
    if (luminance < blackPoint) {
      row->set(x);
    }
    left = center;
    center = right;
  }
  return row;
}
 
Ref<BitMatrix> GlobalHistogramBinarizer::getBlackMatrix() {
  LuminanceSource& source = *getLuminanceSource();
  int width = source.getWidth();
  int height = source.getHeight();
  Ref<BitMatrix> matrix(new BitMatrix(width, height));

  // Quickly calculates the histogram by sampling four rows from the image.
  // This proved to be more robust on the blackbox tests than sampling a
  // diagonal as we used to do.
  initArrays(width);
  ArrayRef<int> localBuckets = buckets;
  for (int y = 1; y < 5; y++) {
    int row = height * y / 5;
    ArrayRef<char> localLuminances = source.getRow(row, luminances);
    int right = (width << 2) / 5;
    for (int x = width / 5; x < right; x++) {
      int pixel = localLuminances[x] & 0xff;
      localBuckets[pixel >> LUMINANCE_SHIFT]++;
    }
  }

  int blackPoint = estimateBlackPoint(localBuckets);

  ArrayRef<char> localLuminances = source.getMatrix();
  for (int y = 0; y < height; y++) {
    int offset = y * width;
    for (int x = 0; x < width; x++) {
      int pixel = localLuminances[offset + x] & 0xff;
      if (pixel < blackPoint) {
        matrix->set(x, y);
      }
    }
  }
  
  return matrix;
}

using namespace std;

int GlobalHistogramBinarizer::estimateBlackPoint(ArrayRef<int> const& buckets) {
  // Find tallest peak in histogram
  int numBuckets = buckets->size();
  int maxBucketCount = 0;
  int firstPeak = 0;
  int firstPeakSize = 0;
  if (false) {
    for (int x = 0; x < numBuckets; x++) {
      cerr << buckets[x] << " ";
    }
    cerr << endl;
  }
  for (int x = 0; x < numBuckets; x++) {
    if (buckets[x] > firstPeakSize) {
      firstPeak = x;
      firstPeakSize = buckets[x];
    }
    if (buckets[x] > maxBucketCount) {
      maxBucketCount = buckets[x];
    }
  }

  // Find second-tallest peak -- well, another peak that is tall and not
  // so close to the first one
  int secondPeak = 0;
  int secondPeakScore = 0;
  for (int x = 0; x < numBuckets; x++) {
    int distanceToBiggest = x - firstPeak;
    // Encourage more distant second peaks by multiplying by square of distance
    int score = buckets[x] * distanceToBiggest * distanceToBiggest;
    if (score > secondPeakScore) {
      secondPeak = x;
      secondPeakScore = score;
    }
  }

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
  // std::cerr << "! " << secondPeak << " " << firstPeak << " " << numBuckets << std::endl;
  if (secondPeak - firstPeak <= numBuckets >> 4) {
    throw NotFoundException();
  }

  // Find a valley between them that is low and closer to the white peak
  int bestValley = secondPeak - 1;
  int bestValleyScore = -1;
  for (int x = secondPeak - 1; x > firstPeak; x--) {
    int fromFirst = x - firstPeak;
    // Favor a "valley" that is not too close to either peak -- especially not
    // the black peak -- and that has a low value of course
    int score = fromFirst * fromFirst * (secondPeak - x) *
      (maxBucketCount - buckets[x]);
    if (score > bestValleyScore) {
      bestValley = x;
      bestValleyScore = score;
    }
  }

  // std::cerr << "bps " << (bestValley << LUMINANCE_SHIFT) << std::endl;
  return bestValley << LUMINANCE_SHIFT;
}

Ref<Binarizer> GlobalHistogramBinarizer::createBinarizer(Ref<LuminanceSource> source) {
  return Ref<Binarizer> (new GlobalHistogramBinarizer(source));
}
