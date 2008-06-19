/*
 *  BitMatrix.cpp
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

#include "BitMatrix.h"
#include "IllegalArgumentException.h"
#include <limits>
#include <iostream>
#include <sstream>

namespace common {
  static unsigned int logDigits(unsigned digits) {
    unsigned log = 0;
    unsigned val = 1;
    while (val < digits) {
      log++;
      val <<= 1;
    }
    return log;
  }
  
  static const unsigned int bitsPerWord_ = 
    numeric_limits<unsigned int>::digits;
  static const unsigned int logBits_ = logDigits(bitsPerWord_);
  static const unsigned int bitsMask_ = (1 << logBits_) - 1;
  
  static size_t wordsForDimension(size_t dimension) {
    size_t bits = dimension * dimension;
    int arraySize = bits >> logBits_;
    if (bits - (arraySize << logBits_) != 0) {
      arraySize++;
    }
    return arraySize;
  }
  
  BitMatrix::BitMatrix(size_t dimension) : 
  dimension_(dimension), bits_(wordsForDimension(dimension)) { 
  }
  
  BitMatrix::~BitMatrix() {
    
  }
  
  bool BitMatrix::get(size_t i, size_t j) {
    size_t offset = i + dimension_ * j;
    return ((bits_[offset >> logBits_] >> (offset & bitsMask_)) & 0x01) != 0;
  }
  
  void BitMatrix::set(size_t i, size_t j) {
    size_t offset = i + dimension_ * j;
    bits_[offset >> logBits_] |= 1 << (offset & bitsMask_);
  }
  
  void BitMatrix::setRegion(size_t topI, 
                            size_t leftJ, 
                            size_t height, 
                            size_t width) {
    if (topI < 0 || leftJ < 0) {
      throw new IllegalArgumentException("topI and leftJ must be nonnegative");
    }
    if (height < 1 || width < 1) {
      throw new IllegalArgumentException("height and width must be at least 1");
    }
    size_t maxJ = leftJ + width;
    size_t maxI = topI + height;
    if (maxI > dimension_ || maxJ > dimension_) {
      throw new IllegalArgumentException
        ("topI + height and leftJ + width must be <= matrix dimension");
    }
    for (size_t j = leftJ; j < maxJ; j++) {
      int jOffset = dimension_ * j;
      for (size_t i = topI; i < maxI; i++) {
        size_t offset = i + jOffset;
        bits_[offset >> logBits_] |= 1 << (offset & bitsMask_);
      }
    }
  }
  
  size_t BitMatrix::getDimension() {
    return dimension_;
  }
  
  valarray<unsigned int> &BitMatrix::getBits() {
    return bits_;
  }
  
  ostream& operator<<(ostream &out, BitMatrix &bm) {
    for (size_t i = 0; i < bm.dimension_; i++) {
      for (size_t j = 0; j < bm.dimension_; j++) {
        out << (bm.get(i, j) ? "* " : "- ");
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
