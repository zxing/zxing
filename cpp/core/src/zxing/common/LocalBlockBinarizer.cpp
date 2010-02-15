/*
 *  LocalBlockBinarizer.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 17/10/2009.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include <zxing/common/LocalBlockBinarizer.h>

namespace zxing {

const int GLOBAL = 0;
const int THRESHOLD = 1;

LocalBlockBinarizer::LocalBlockBinarizer(Ref<LuminanceSource> source) :
    Binarizer(source) {

}

LocalBlockBinarizer::~LocalBlockBinarizer() {
}

Ref<BitArray> LocalBlockBinarizer::estimateBlackRow(int y, Ref<BitArray> row) {
  //TODO: implement
  return Ref<BitArray>();
}

// Calculates the final BitMatrix once for all requests. This could be called once from the
// constructor instead, but there are some advantages to doing it lazily, such as making
// profiling easier, and not doing heavy lifting when callers don't expect it.
Ref<BitMatrix> LocalBlockBinarizer::estimateBlackMatrix() {
  Ref<LuminanceSource> source = getSource();
  unsigned char* luminances = source->copyMatrix();
  int width = source->getWidth();
  int height = source->getHeight();
  // Sharpening does not really help for 2d barcodes
  //	sharpenRow(luminances, width, height);

  int subWidth = width >> 3;
  int subHeight = height >> 3;

  unsigned char* averages = new unsigned char[subWidth * subHeight];
  unsigned char* types = new unsigned char[subWidth * subHeight];

  calculateBlackPoints(luminances, averages, types, subWidth, subHeight, width);

  Ref<BitMatrix> matrix(new BitMatrix(width, height));
  calculateThresholdForBlock(luminances, subWidth, subHeight, width, averages, types, *matrix);

  delete[] averages;
  delete[] types;
  delete[] luminances;

  return matrix;
}

// For each 8x8 block in the image, calculate the average black point using a 5x5 grid
// of the blocks around it. Also handles the corner cases, but will ignore up to 7 pixels
// on the right edge and 7 pixels at the bottom of the image if the overall dimensions are not
// multiples of eight. In practice, leaving those pixels white does not seem to be a problem.
void LocalBlockBinarizer::calculateThresholdForBlock(const unsigned char* luminances, int subWidth, int subHeight,
    int stride, const unsigned char* averages, const unsigned char* types, BitMatrix& matrix) {
  // Calculate global average
  int global = 0;
  for (int y = 0; y < subHeight; y++) {
    for (int x = 0; x < subWidth; x++) {
      global += averages[y * subWidth + x];
    }
  }

  global /= subWidth * subHeight;


  for (int y = 0; y < subHeight; y++) {
    for (int x = 0; x < subWidth; x++) {
      int left = (x > 0) ? x : 1;
      left = (left < subWidth - 1) ? left : subWidth - 2;
      int top = (y > 0) ? y : 1;
      top = (top < subHeight - 1) ? top : subHeight - 2;
      int sum = 0;
      int contrast = 0;
      for (int z = -1; z <= 1; z++) {
//				sum += averages[(top + z) * subWidth + left - 2];
        sum += averages[(top + z) * subWidth + left - 1];
        sum += averages[(top + z) * subWidth + left];
        sum += averages[(top + z) * subWidth + left + 1];
//				sum += averages[(top + z) * subWidth + left + 2];

//				type += types[(top + z) * subWidth + left - 2];
        contrast += types[(top + z) * subWidth + left - 1];
        contrast += types[(top + z) * subWidth + left];
        contrast += types[(top + z) * subWidth + left + 1];
//				type += types[(top + z) * subWidth + left + 2];
      }
      int average = sum / 9;


      if (contrast > 2)
        threshold8x8Block(luminances, x << 3, y << 3, average, stride, matrix);
//			else if(average < global)	// Black
//				matrix.setRegion(x << 3, y << 3, 8, 8);
      // If white, we don't need to do anything - the block is already cleared.
    }
  }
}

// Applies a single threshold to an 8x8 block of pixels.
void LocalBlockBinarizer::threshold8x8Block(const unsigned char* luminances, int xoffset, int yoffset, int threshold,
    int stride, BitMatrix& matrix) {
  for (int y = 0; y < 8; y++) {
    int offset = (yoffset + y) * stride + xoffset;
    for (int x = 0; x < 8; x++) {
      int pixel = luminances[offset + x];
      if (pixel < threshold) {
        matrix.set(xoffset + x, yoffset + y);
      }
    }
  }
}

// Calculates a single black point for each 8x8 block of pixels and saves it away.
void LocalBlockBinarizer::calculateBlackPoints(const unsigned char* luminances, unsigned char* averages,
    unsigned char* types, int subWidth, int subHeight, int stride) {
  for (int y = 0; y < subHeight; y++) {
    for (int x = 0; x < subWidth; x++) {
      int sum = 0;
      int min = 255;
      int max = 0;
      for (int yy = 0; yy < 8; yy++) {
        int offset = ((y << 3) + yy) * stride + (x << 3);
        const unsigned char* lumo = luminances + offset;
        for (int xx = 0; xx < 8; xx++) {
          int pixel = lumo[xx];
          sum += pixel;
          if (pixel < min) {
            min = pixel;
          }
          if (pixel > max) {
            max = pixel;
          }
        }
      }

      // If the contrast is inadequate, we treat the block as white.
      // An arbitrary value is chosen here. Higher values mean less noise, but may also reduce
      // the ability to recognise some barcodes.
      int average = sum >> 6;
      int type;

      if (max - min > 30)
        type = THRESHOLD;
      else
        type = GLOBAL;
      //			int average = (max - min > 24) ? (sum >> 6) : (min-1);
      averages[y * subWidth + x] = average;
      types[y * subWidth + x] = type;
    }
  }
}

// Applies a simple -1 4 -1 box filter with a weight of 2 to each row.
void LocalBlockBinarizer::sharpenRow(unsigned char* luminances, int width, int height) {
  for (int y = 0; y < height; y++) {
    int offset = y * width;
    int left = luminances[offset];
    int center = luminances[offset + 1];
    for (int x = 1; x < width - 1; x++) {
      unsigned char right = luminances[offset + x + 1];
      int pixel = ((center << 2) - left - right) >> 1;
      // Must clamp values to 0..255 so they will fit in a byte.
      if (pixel > 255) {
        pixel = 255;
      } else if (pixel < 0) {
        pixel = 0;
      }
      luminances[offset + x] = (unsigned char)pixel;
      left = center;
      center = right;
    }
  }
}

}
