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

namespace zxing {
using namespace std;

static const int MINIMUM_DIMENSION = 40;

static const int LUMINANCE_BITS = 5;
static const int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
static const int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

HybridBinarizer::HybridBinarizer(Ref<LuminanceSource> source) :
  GlobalHistogramBinarizer(source), cached_matrix_(NULL), cached_row_(NULL), cached_row_num_(-1) {

}

HybridBinarizer::~HybridBinarizer() {
}


Ref<BitMatrix> HybridBinarizer::getBlackMatrix() {
  binarizeEntireImage();
  return cached_matrix_;
}

Ref<Binarizer> HybridBinarizer::createBinarizer(Ref<LuminanceSource> source) {
  return Ref<Binarizer> (new HybridBinarizer(source));
}

void HybridBinarizer::binarizeEntireImage() {
  if (cached_matrix_ == NULL) {
    Ref<LuminanceSource> source = getLuminanceSource();
    if (source->getWidth() >= MINIMUM_DIMENSION && source->getHeight() >= MINIMUM_DIMENSION) {
      unsigned char* luminances = source->getMatrix();
      int width = source->getWidth();
      int height = source->getHeight();
      int subWidth = width >> 3;
      if (width & 0x07) {
        subWidth++;
      }
      int subHeight = height >> 3;
      if (height & 0x07) {
        subHeight++;
      }
      int *blackPoints = calculateBlackPoints(luminances, subWidth, subHeight, width, height);
      cached_matrix_.reset(new BitMatrix(width,height));
      calculateThresholdForBlock(luminances, subWidth, subHeight, width, height, blackPoints, cached_matrix_);
      delete [] blackPoints;
      delete [] luminances;
    } else {
      // If the image is too small, fall back to the global histogram approach.
      cached_matrix_.reset(GlobalHistogramBinarizer::getBlackMatrix());
    }
  }
}

void HybridBinarizer::calculateThresholdForBlock(unsigned char* luminances, int subWidth, int subHeight,
    int width, int height, int blackPoints[], Ref<BitMatrix> matrix) {
  for (int y = 0; y < subHeight; y++) {
    int yoffset = y << 3;
    if (yoffset + 8 >= height) {
      yoffset = height - 8;
    }
    for (int x = 0; x < subWidth; x++) {
      int xoffset = x << 3;
      if (xoffset + 8 >= width) {
        xoffset = width - 8;
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

void HybridBinarizer::threshold8x8Block(unsigned char* luminances, int xoffset, int yoffset, int threshold,
    int stride, Ref<BitMatrix> matrix) {
  for (int y = 0; y < 8; y++) {
    int offset = (yoffset + y) * stride + xoffset;
    for (int x = 0; x < 8; x++) {
      int pixel = luminances[offset + x] & 0xff;
      if (pixel < threshold) {
        matrix->set(xoffset + x, yoffset + y);
      }
    }
  }
}

int* HybridBinarizer::calculateBlackPoints(unsigned char* luminances, int subWidth, int subHeight,
    int width, int height) {
  int *blackPoints = new int[subHeight * subWidth];
  for (int y = 0; y < subHeight; y++) {
    int yoffset = y << 3;
    if (yoffset + 8 >= height) {
      yoffset = height - 8;
    }
    for (int x = 0; x < subWidth; x++) {
      int xoffset = x << 3;
      if (xoffset + 8 >= width) {
        xoffset = width - 8;
      }
      int sum = 0;
      int min = 255;
      int max = 0;
      for (int yy = 0; yy < 8; yy++) {
        int offset = (yoffset + yy) * width + xoffset;
        for (int xx = 0; xx < 8; xx++) {
          int pixel = luminances[offset + xx] & 0xff;
          sum += pixel;
          if (pixel < min) {
            min = pixel;
          }
          if (pixel > max) {
            max = pixel;
          }
        }
      }

      // If the contrast is inadequate, use half the minimum, so that this block will be
      // treated as part of the white background, but won't drag down neighboring blocks
      // too much.
      int average;
      if (max - min > 24) {
          average = (sum >> 6);
      } else {
        average = max == 0 ? 1 : (min >> 1);
      }
      blackPoints[y * subWidth + x] = average;
    }
  }
  return blackPoints;
}

} // namespace zxing

