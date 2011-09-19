/*
 *  BitMatrix.cpp
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

#include <zxing/common/BitMatrix.h>
#include <zxing/common/IllegalArgumentException.h>

#include <iostream>
#include <sstream>
#include <string>

namespace zxing {
using namespace std;

unsigned int logDigits(unsigned digits) {
  unsigned log = 0;
  unsigned val = 1;
  while (val < digits) {
    log++;
    val <<= 1;
  }
  return log;
}

const unsigned int bitsPerWord = numeric_limits<unsigned int>::digits;
const unsigned int logBits = logDigits(bitsPerWord);
const unsigned int bitsMask = (1 << logBits) - 1;

static size_t wordsForSize(size_t width, size_t height) {
  size_t bits = width * height;
  int arraySize = bits >> logBits;
  if (bits - (arraySize << logBits) != 0) {
    arraySize++;
  }
  return arraySize;
}

BitMatrix::BitMatrix(size_t dimension) :
    width_(dimension), height_(dimension), words_(0), bits_(NULL) {

  words_ = wordsForSize(width_, height_);
  bits_ = new unsigned int[words_];
  clear();
}

BitMatrix::BitMatrix(size_t width, size_t height) :
    width_(width), height_(height), words_(0), bits_(NULL) {

  words_ = wordsForSize(width_, height_);
  bits_ = new unsigned int[words_];
  clear();
}

BitMatrix::~BitMatrix() {
  delete[] bits_;
}


bool BitMatrix::get(size_t x, size_t y) const {
  size_t offset = x + width_ * y;
  return ((bits_[offset >> logBits] >> (offset & bitsMask)) & 0x01) != 0;
}

void BitMatrix::set(size_t x, size_t y) {
  size_t offset = x + width_ * y;
  bits_[offset >> logBits] |= 1 << (offset & bitsMask);
}

void BitMatrix::flip(size_t x, size_t y) {
  size_t offset = x + width_ * y;
  bits_[offset >> logBits] ^= 1 << (offset & bitsMask);
}

void BitMatrix::clear() {
  std::fill(bits_, bits_+words_, 0);
}

void BitMatrix::setRegion(size_t left, size_t top, size_t width, size_t height) {
  if ((long)top < 0 || (long)left < 0) {
    throw IllegalArgumentException("topI and leftJ must be nonnegative");
  }
  if (height < 1 || width < 1) {
    throw IllegalArgumentException("height and width must be at least 1");
  }
  size_t right = left + width;
  size_t bottom = top + height;
  if (right > width_ || bottom > height_) {
    throw IllegalArgumentException("top + height and left + width must be <= matrix dimension");
  }
  for (size_t y = top; y < bottom; y++) {
    int yOffset = width_ * y;
    for (size_t x = left; x < right; x++) {
      size_t offset = x + yOffset;
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
  size_t start = y * width_;
  size_t end = start + width_ - 1; // end is inclusive
  size_t firstWord = start >> logBits;
  size_t lastWord = end >> logBits;
  size_t bitOffset = start & bitsMask;
  for (size_t i = firstWord; i <= lastWord; i++) {
    size_t firstBit = i > firstWord ? 0 : start & bitsMask;
    size_t lastBit = i < lastWord ? bitsPerWord - 1 : end & bitsMask;
    unsigned int mask;
    if (firstBit == 0 && lastBit == logBits) {
      mask = numeric_limits<unsigned int>::max();
    } else {
      mask = 0;
      for (size_t j = firstBit; j <= lastBit; j++) {
        mask |= 1 << j;
      }
    }
    row->setBulk((i - firstWord) << logBits, (bits_[i] & mask) >> bitOffset);
    if (firstBit == 0 && bitOffset != 0) {
      unsigned int prevBulk = row->getBitArray()[i - firstWord - 1];
      prevBulk |= (bits_[i] & mask) << (bitsPerWord - bitOffset);
      row->setBulk((i - firstWord - 1) << logBits, prevBulk);
    }
  }
  return row;
}

size_t BitMatrix::getWidth() const {
  return width_;
}

size_t BitMatrix::getHeight() const {
  return height_;
}

size_t BitMatrix::getDimension() const {
  return width_;
}

unsigned int* BitMatrix::getBits() const {
  return bits_;
}

ostream& operator<<(ostream &out, const BitMatrix &bm) {
  for (size_t y = 0; y < bm.height_; y++) {
    for (size_t x = 0; x < bm.width_; x++) {
      out << (bm.get(x, y) ? "X " : "  ");
    }
    out << "\n";
  }
  return out;
}
const char *BitMatrix::description() {
  ostringstream out;
  out << *this;
  return out.str().c_str();
}

}
