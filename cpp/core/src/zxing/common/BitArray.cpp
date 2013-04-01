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

#include <zxing/common/BitArray.h>

using std::vector;
using zxing::BitArray;

int BitArray::wordsForBits(int bits) {
  int arraySize = (bits + bitsPerWord_ - 1) >> logBits_;
  return arraySize;
}

BitArray::BitArray(int size) :
  size_(size), bits_(wordsForBits(size), 0) {
}

BitArray::~BitArray() {
}

int BitArray::getSize() const {
  return size_;
}

void BitArray::setBulk(int i, int newBits) {
  bits_[i >> logBits_] = newBits;
}

void BitArray::setRange(int start, int end) {
  if (end < start) {
    throw IllegalArgumentException("invalid call to BitArray::setRange");
  }
  if (end == start) {
    return;
  }
  end--; // will be easier to treat this as the last actually set bit -- inclusive
  int firstInt = start >> 5;
  int lastInt = end >> 5;
  for (int i = firstInt; i <= lastInt; i++) {
    int firstBit = i > firstInt ? 0 : start & 0x1F;
    int lastBit = i < lastInt ? 31 : end & 0x1F;
    int mask;
    if (firstBit == 0 && lastBit == 31) {
      mask = -1;
    } else {
      mask = 0;
      for (int j = firstBit; j <= lastBit; j++) {
        mask |= 1 << j;
      }
    }
    bits_[i] |= mask;
  }
}

void BitArray::clear() {
  int max = bits_.size();
  for (int i = 0; i < max; i++) {
    bits_[i] = 0;
  }
}

bool BitArray::isRange(int start, int end, bool value) {
  if (end < start) {
    throw IllegalArgumentException("end must be after start");
  }
  if (end == start) {
    return true;
  }
  // treat the 'end' as inclusive, rather than exclusive
  end--;
  int firstWord = start >> logBits_;
  int lastWord = end >> logBits_;
  for (int i = firstWord; i <= lastWord; i++) {
    int firstBit = i > firstWord ? 0 : start & bitsMask_;
    int lastBit = i < lastWord ? bitsPerWord_ - 1: end & bitsMask_;
    int mask;
    if (firstBit == 0 && lastBit == bitsPerWord_ - 1) {
      mask = -1;
    } else {
      mask = 0;
      for (int j = firstBit; j <= lastBit; j++) {
        mask |= 1 << j;
      }
    }
    if (value) {
      if ((bits_[i] & mask) != mask) {
        return false;
      }
    } else {
      if ((bits_[i] & mask) != 0) {
        return false;
      }
    }
  }
  return true;
}

vector<int>& BitArray::getBitArray() {
  return bits_;
}

void BitArray::reverse() {
  // std::cerr << "reverse" << std::endl;
  std::vector<int> newBits(bits_.size(), 0);
  for (int i = 0; i < size_; i++) {
    if (get(size_ - i - 1)) {
      newBits[i >> logBits_] |= 1<< (i & bitsMask_);
    }
  }
  bits_ = newBits;
}

BitArray::Reverse::Reverse(Ref<BitArray> array_) : array(array_) {
  array->reverse();
}

BitArray::Reverse::~Reverse() {
  array->reverse();
}

namespace {
  int numberOfTrailingZeros(int i) {
    // HD, Figure 5-14
    int y;
    if (i == 0) return 32;
    int n = 31;
    y = i <<16; if (y != 0) { n = n -16; i = y; }
    y = i << 8; if (y != 0) { n = n - 8; i = y; }
    y = i << 4; if (y != 0) { n = n - 4; i = y; }
    y = i << 2; if (y != 0) { n = n - 2; i = y; }
    return n - (((unsigned int)(i << 1)) >> 31);
  }
}

int BitArray::getNextSet(int from) {
  if (from >= size_) {
    return size_;
  }
  int bitsOffset = from >> 5;
  int currentBits = bits_[bitsOffset];
  // mask off lesser bits first
  currentBits &= ~((1 << (from & 0x1F)) - 1);
  while (currentBits == 0) {
    if (++bitsOffset == (int)bits_.size()) {
      return size_;
    }
    currentBits = bits_[bitsOffset];
  }
  int result = (bitsOffset << 5) + numberOfTrailingZeros(currentBits);
  return result > size_ ? size_ : result;
}

int BitArray::getNextUnset(int from) {
  if (from >= size_) {
    return size_;
  }
  int bitsOffset = from >> 5;
  int currentBits = ~bits_[bitsOffset];
  // mask off lesser bits first
  currentBits &= ~((1 << (from & 0x1F)) - 1);
  while (currentBits == 0) {
    if (++bitsOffset == (int)bits_.size()) {
      return size_;
    }
    currentBits = ~bits_[bitsOffset];
  }
  int result = (bitsOffset << 5) + numberOfTrailingZeros(currentBits);
  return result > size_ ? size_ : result;
}
