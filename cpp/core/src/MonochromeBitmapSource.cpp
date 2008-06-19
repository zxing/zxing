/*
 *  MonochromeBitmapSource.cpp
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

#include "MonochromeBitmapSource.h"
#include "BlackPointEstimator.h"
#include "ReaderException.h"
#include <limits>

#define LUMINANCE_BITS (5)
#define LUMINANCE_SHIFT (8 - LUMINANCE_BITS)
#define LUMINANCE_BUCKETS (1 << LUMINANCE_BITS)

using namespace std;

MonochromeBitmapSource::MonochromeBitmapSource() :
blackPoint_(numeric_limits<unsigned char>::max()),
lastEstimationMethod_(BlackPointEstimationMethod_None), 
lastEstimationArgument_(-1) {
}


bool MonochromeBitmapSource::isBlack(size_t x, size_t y) {
  return getPixel(x, y) < blackPoint_;
}

Ref<BitArray> MonochromeBitmapSource::getBlackRow(size_t y,
                                                  Ref<BitArray> row, 
                                                  size_t startX,
                                                  size_t getWidth) {
  Ref<BitArray> result(row ? row : new BitArray(getWidth));
  if (row) {
    result->clear();
  }
  valarray<unsigned char> pixelRow(getWidth);
  for (size_t i = 0; i < getWidth; i++) {
    pixelRow[i] = getPixel(startX + i, y);
  }
  
  // If the current decoder calculated the blackPoint based on one row, 
  // assume we're trying to decode a 1D barcode, and apply some sharpening.
  if (lastEstimationMethod_ == BlackPointEstimationMethod_RowSampling) {
    unsigned int left = pixelRow[0];
    unsigned int center = pixelRow[1];
    for (size_t i = 1; i < getWidth - 1; i++) {
      unsigned int right = pixelRow[i+1];
      // Simple -1 4 -1 box filter with a weight of 2
      unsigned int luminance = ((center << 2) - left - right) >> 1;
      if (luminance < blackPoint_) {
        result->set(i);
      }
      left = center;
      center = right;
    }
  } else {
    for (size_t i = 0; i < getWidth; i++) {
      if (pixelRow[i] < blackPoint_) {
        result->set(i);
      }
    }
  }
  
  return result;
}

void MonochromeBitmapSource::
estimateBlackPoint(BlackPointEstimationMethod method, int arg) {
  size_t width = getWidth();
  size_t height = getHeight();
  if (method != lastEstimationMethod_ || arg != lastEstimationArgument_) {
    valarray<int> histogram(LUMINANCE_BUCKETS);
    if (method == BlackPointEstimationMethod_2D) {
      size_t minDimension = width < height ? width : height;
      size_t yOffset = height == minDimension ? 0 : (height - width) >> 1;
      size_t xOffset = width == minDimension ? 0 : (width - height) >> 1;
      for (size_t n = 0; n < minDimension; n++) {
        unsigned char pixel = getPixel(xOffset + n, yOffset + n);
        histogram[pixel >> LUMINANCE_SHIFT]++;
      }
    } else if (method == BlackPointEstimationMethod_RowSampling) {
      if (arg < 0 || arg > (int)height) {
        throw new IllegalArgumentException
        ("black point estimation argument out of range");
      }
      size_t y = static_cast<size_t> (arg);
      for (size_t x = 0; x < width; x++) {
        histogram[getPixel(x, y) >> LUMINANCE_SHIFT]++;
      }
    } else {
      throw new IllegalArgumentException
      ("unknown black point estimation method");
    }
    
    blackPoint_ = BlackPointEstimator::estimate(histogram) << LUMINANCE_SHIFT;    
    lastEstimationMethod_ = method;
    lastEstimationArgument_ = arg;
  }
}

BlackPointEstimationMethod MonochromeBitmapSource::getLastEstimationMethod() {
  return BlackPointEstimationMethod_None;
}

