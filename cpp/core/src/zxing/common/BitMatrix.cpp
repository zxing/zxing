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

namespace {
  int wordsForSize(int width,
                      int height,
                      int bitsPerWord,
                      int logBits) {
    int bits = width * height;
    int arraySize = (bits + bitsPerWord - 1) >> logBits;
    return arraySize;
  }
}

BitMatrix::BitMatrix(int dimension) :
  width_(dimension), height_(dimension), words_(0) {
  words_ = wordsForSize(width_, height_, bitsPerWord, logBits);
  bits_ = ArrayRef<int>(words_);
  clear();
}

BitMatrix::BitMatrix(int width, int height) :
  width_(width), height_(height), words_(0) {
  words_ = wordsForSize(width_, height_, bitsPerWord, logBits);
  bits_ = ArrayRef<int>(words_);
  clear();
}

BitMatrix::~BitMatrix() {}


void BitMatrix::flip(int x, int y) {
  int offset = x + width_ * y;
  bits_[offset >> logBits] ^= 1 << (offset & bitsMask);
}

void BitMatrix::clear() {
  std::fill(&bits_[0], &bits_[words_], 0);
}

void BitMatrix::setRegion(int left, int top, int width, int height) {
  if ((long)top < 0 || (long)left < 0) {
    throw IllegalArgumentException("topI and leftJ must be nonnegative");
  }
  if (height < 1 || width < 1) {
    throw IllegalArgumentException("height and width must be at least 1");
  }
  int right = left + width;
  int bottom = top + height;
  if (right > width_ || bottom > height_) {
    throw IllegalArgumentException("top + height and left + width must be <= matrix dimension");
  }
  for (int y = top; y < bottom; y++) {
    int yOffset = width_ * y;
    for (int x = left; x < right; x++) {
      int offset = x + yOffset;
      bits_[offset >> logBits] |= 1 << (offset & bitsMask);
    }
  }
}

Ref<BitArray> BitMatrix::getRow(int y, Ref<BitArray> row) {
  if (row.empty() || row->getSize() < width_) {
    row = new BitArray(width_);
  } else {
    row->clear();
  }
  int start = y * width_;
  int end = start + width_ - 1; // end is inclusive
  int firstWord = start >> logBits;
  int lastWord = end >> logBits;
  int bitOffset = start & bitsMask;
  for (int i = firstWord; i <= lastWord; i++) {
    int firstBit = i > firstWord ? 0 : start & bitsMask;
    int lastBit = i < lastWord ? bitsPerWord - 1 : end & bitsMask;
    int mask;
    if (firstBit == 0 && lastBit == logBits) {
      mask = std::numeric_limits<int>::max();
    } else {
      mask = 0;
      for (int j = firstBit; j <= lastBit; j++) {
        mask |= 1 << j;
      }
    }
    row->setBulk((i - firstWord) << logBits, (bits_[i] & mask) >> bitOffset);
    if (firstBit == 0 && bitOffset != 0) {
      int prevBulk = row->getBitArray()[i - firstWord - 1];
      prevBulk |= (bits_[i] & mask) << (bitsPerWord - bitOffset);
      row->setBulk((i - firstWord - 1) << logBits, prevBulk);
    }
  }
  return row;
}

int BitMatrix::getWidth() const {
  return width_;
}

int BitMatrix::getHeight() const {
  return height_;
}

int BitMatrix::getDimension() const {
  return width_;
}

ArrayRef<int> BitMatrix::getBits() const {
  return bits_;
}

namespace zxing {
  ostream& operator<<(ostream &out, const BitMatrix &bm) {
    for (int y = 0; y < bm.height_; y++) {
      for (int x = 0; x < bm.width_; x++) {
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
