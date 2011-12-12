// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  HybridBinarizer.cpp
 *  zxing
 *
 *  Copyright 2010 ZXing authors All rights reserved.
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

#include <zxing/common/HybridBinarizer.h>

#include <zxing/common/IllegalArgumentException.h>

using namespace std;
using namespace zxing;

namespace {
  const int BLOCK_SIZE_POWER = 3;
  const int BLOCK_SIZE = 1 << BLOCK_SIZE_POWER;
  const int BLOCK_SIZE_MASK = BLOCK_SIZE - 1;
  const int MINIMUM_DIMENSION = BLOCK_SIZE * 5;
}

HybridBinarizer::HybridBinarizer(Ref<LuminanceSource> source) :
  GlobalHistogramBinarizer(source), matrix_(NULL), cached_row_(NULL), cached_row_num_(-1) {
}

HybridBinarizer::~HybridBinarizer() {
}


Ref<Binarizer>
HybridBinarizer::createBinarizer(Ref<LuminanceSource> source) {
  return Ref<Binarizer> (new HybridBinarizer(source));
}

Ref<BitMatrix> HybridBinarizer::getBlackMatrix() {
  // Calculates the final BitMatrix once for all requests. This could
  // be called once from the constructor instead, but there are some
  // advantages to doing it lazily, such as making profiling easier,
  // and not doing heavy lifting when callers don't expect it.
  if (matrix_) {
    return matrix_;
  }
  LuminanceSource& source = *getLuminanceSource();
  if (source.getWidth() >= MINIMUM_DIMENSION &&
      source.getHeight() >= MINIMUM_DIMENSION) {
    unsigned char* luminances = source.getMatrix();
    int width = source.getWidth();
    int height = source.getHeight();
    int subWidth = width >> BLOCK_SIZE_POWER;
    if ((width & BLOCK_SIZE_MASK) != 0) {
      subWidth++;
    }
    int subHeight = height >> BLOCK_SIZE_POWER;
    if ((height & BLOCK_SIZE_MASK) != 0) {
      subHeight++;
    }
    int* blackPoints =
      calculateBlackPoints(luminances, subWidth, subHeight, width, height);

    Ref<BitMatrix> newMatrix (new BitMatrix(width, height));
    calculateThresholdForBlock(luminances,
                               subWidth,
                               subHeight,
                               width,
                               height,
                               blackPoints,
                               newMatrix);
    matrix_ = newMatrix;

    // N.B.: these deletes are inadequate if anything between the new
    // and this point can throw.  As of this writing, it doesn't look
    // like they do.

    delete [] blackPoints;
    delete [] luminances;
  } else {
    // If the image is too small, fall back to the global histogram approach.
    matrix_ = GlobalHistogramBinarizer::getBlackMatrix();
  }
  return matrix_;
}

void
HybridBinarizer::calculateThresholdForBlock(unsigned char* luminances,
                                            int subWidth,
                                            int subHeight,
                                            int width,
                                            int height,
                                            int blackPoints[],
                                            Ref<BitMatrix> const& matrix) {
  for (int y = 0; y < subHeight; y++) {
    int yoffset = y << BLOCK_SIZE_POWER;
    if (yoffset + BLOCK_SIZE >= height) {
      yoffset = height - BLOCK_SIZE;
    }
    for (int x = 0; x < subWidth; x++) {
      int xoffset = x << BLOCK_SIZE_POWER;
      if (xoffset + BLOCK_SIZE >= width) {
        xoffset = width - BLOCK_SIZE;
      }
      int left = (x > 1) ? x : 2;
      left = (left < subWidth - 2) ? left : subWidth - 3;
      int top = (y > 1) ? y : 2;
      top = (top < subHeight - 2) ? top : subHeight - 3;
      int sum = 0;
      for (int z = -2; z <= 2; z++) {
        int *blackRow = &blackPoints[(top + z) * subWidth];
        sum += blackRow[left - 2];
        sum += blackRow[left - 1];
        sum += blackRow[left];
        sum += blackRow[left + 1];
        sum += blackRow[left + 2];
      }
      int average = sum / 25;
      threshold8x8Block(luminances, xoffset, yoffset, average, width, matrix);
    }
  }
}

void HybridBinarizer::threshold8x8Block(unsigned char* luminances,
                                        int xoffset,
                                        int yoffset,
                                        int threshold,
                                        int stride,
                                        Ref<BitMatrix> const& matrix) {
  for (int y = 0, offset = yoffset * stride + xoffset;
       y < BLOCK_SIZE;
       y++,  offset += stride) {
    for (int x = 0; x < BLOCK_SIZE; x++) {
      int pixel = luminances[offset + x] & 0xff;
      if (pixel <= threshold) {
        matrix->set(xoffset + x, yoffset + y);
      }
    }
  }
}

namespace {
  inline int getBlackPointFromNeighbors(int* blackPoints, int subWidth, int x, int y) {
    return (blackPoints[(y-1)*subWidth+x] +
            2*blackPoints[y*subWidth+x-1] +
            blackPoints[(y-1)*subWidth+x-1]) >> 2;
  }
}

int* HybridBinarizer::calculateBlackPoints(unsigned char* luminances, int subWidth, int subHeight,
    int width, int height) {
  int *blackPoints = new int[subHeight * subWidth];
  for (int y = 0; y < subHeight; y++) {
    int yoffset = y << BLOCK_SIZE_POWER;
    if (yoffset + BLOCK_SIZE >= height) {
      yoffset = height - BLOCK_SIZE;
    }
    for (int x = 0; x < subWidth; x++) {
      int xoffset = x << BLOCK_SIZE_POWER;
      if (xoffset + BLOCK_SIZE >= width) {
        xoffset = width - BLOCK_SIZE;
      }
      int sum = 0;
      int min = 0xFF;
      int max = 0;
      for (int yy = 0, offset = yoffset * width + xoffset;
           yy < BLOCK_SIZE;
           yy++, offset += width) {
        for (int xx = 0; xx < BLOCK_SIZE; xx++) {
          int pixel = luminances[offset + xx] & 0xFF;
          sum += pixel;
          if (pixel < min) {
            min = pixel;
          }
          if (pixel > max) {
            max = pixel;
          }
        }
      }
      
      // See
      // http://groups.google.com/group/zxing/browse_thread/thread/d06efa2c35a7ddc0
      int average = sum >> 6;
      if (max - min <= 24) {
        average = min >> 1;
        if (y > 0 && x > 0) {
          int bp = getBlackPointFromNeighbors(blackPoints, subWidth, x, y);
          if (min < bp) {
            average = bp;
          }
        }
      }
      blackPoints[y * subWidth + x] = average;
    }
  }
  return blackPoints;
}

