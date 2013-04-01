// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
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

#include <zxing/common/BitMatrix.h>
#include <zxing/common/IllegalArgumentException.h>

#include <iostream>
#include <sstream>
#include <string>

using std::ostream;
using std::ostringstream;

using zxing::BitMatrix;
using zxing::BitArray;
using zxing::ArrayRef;
using zxing::Ref;

void BitMatrix::init(int width, int height) {
  if (width < 1 || height < 1) {
    throw IllegalArgumentException("Both dimensions must be greater than 0");
  }
  this->width = width;
  this->height = height;
  this->rowSize = (width + bitsPerWord - 1) >> logBits;
  bits = ArrayRef<int>(rowSize * height);
}

BitMatrix::BitMatrix(int dimension) {
  init(dimension, dimension);
}

BitMatrix::BitMatrix(int width, int height) {
  init(width, height);
}

BitMatrix::~BitMatrix() {}

void BitMatrix::flip(int x, int y) {
  int offset = y * rowSize + (x >> logBits);
  bits[offset] ^= 1 << (x & bitsMask);
}

/*
void BitMatrix::clear() {
  std::fill(&bits[0], &bits[rowSize], 0);
}
*/

void BitMatrix::setRegion(int left, int top, int width, int height) {
  if (top < 0 || left < 0) {
    throw IllegalArgumentException("Left and top must be nonnegative");
  }
  if (height < 1 || width < 1) {
    throw IllegalArgumentException("Height and width must be at least 1");
  }
  int right = left + width;
  int bottom = top + height;
  if (bottom > this->height || right > this->width) {
    throw new IllegalArgumentException("The region must fit inside the matrix");
  }
  for (int y = top; y < bottom; y++) {
    int offset = y * rowSize;
    for (int x = left; x < right; x++) {
      bits[offset + (x >> logBits)] |= 1 << (x & bitsMask);
    }
  }
}

/*
void BitMatrix::setRegion(int left, int top, int width, int height) {
  if ((long)top < 0 || (long)left < 0) {
    throw IllegalArgumentException("topI and leftJ must be nonnegative");
  }
  if (height < 1 || width < 1) {
    throw IllegalArgumentException("height and width must be at least 1");
  }
  int right = left + width;
  int bottom = top + height;
  if (right > this->width || bottom > this->height) {
    throw IllegalArgumentException("top + height and left + width must be <= matrix dimension");
  }
  for (int y = top; y < bottom; y++) {
    int offset =  y * rowSize;
    for (int x = left; x < right; x++) {
      int offset = x + yOffset;
      bits[offset + (x >> 5)] |= 1 << (offset & bitsMask);
    }
  }
}
*/

Ref<BitArray> BitMatrix::getRow(int y, Ref<BitArray> row) {
  if (row.empty() || row->getSize() < width) {
    row = new BitArray(width);
  }
  int offset = y * rowSize;
  for (int x = 0; x < rowSize; x++) {
    row->setBulk(x << logBits, bits[offset + x]);
  }
  return row;
}

int BitMatrix::getWidth() const {
  return width;
}

int BitMatrix::getHeight() const {
  return height;
}

/*
namespace zxing {
  ostream& operator<<(ostream &out, const BitMatrix &bm) {
    for (int y = 0; y < bm.height; y++) {
      for (int x = 0; x < bm.width; x++) {
        out << (bm.get(x, y) ? "X " : "  ");
      }
      out << "\n";
    }
    return out;
  }
}

const char* BitMatrix::description() {
  ostringstream out;
  out << *this;
  return out.str().c_str();
}

*/
