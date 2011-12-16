// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#ifndef __BIT_MATRIX_H__
#define __BIT_MATRIX_H__

/*
 *  BitMatrix.h
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

#include <zxing/common/Counted.h>
#include <zxing/common/BitArray.h>
#include <limits>

namespace zxing {

class BitMatrix : public Counted {
private:
  size_t width_;
  size_t height_;
  size_t words_;
  unsigned int* bits_;

#define ZX_LOG_DIGITS(digits) \
    ((digits == 8) ? 3 : \
     ((digits == 16) ? 4 : \
      ((digits == 32) ? 5 : \
       ((digits == 64) ? 6 : \
        ((digits == 128) ? 7 : \
         (-1))))))

  static const unsigned int bitsPerWord =
    std::numeric_limits<unsigned int>::digits;
  static const unsigned int logBits = ZX_LOG_DIGITS(bitsPerWord);
  static const unsigned int bitsMask = (1 << logBits) - 1;

public:
  BitMatrix(size_t dimension);
  BitMatrix(size_t width, size_t height);

  ~BitMatrix();

  bool get(size_t x, size_t y) const {
    size_t offset = x + width_ * y;
    return ((bits_[offset >> logBits] >> (offset & bitsMask)) & 0x01) != 0;
  }

  void set(size_t x, size_t y) {
    size_t offset = x + width_ * y;
    bits_[offset >> logBits] |= 1 << (offset & bitsMask);
  }

  void flip(size_t x, size_t y);
  void clear();
  void setRegion(size_t left, size_t top, size_t width, size_t height);
  Ref<BitArray> getRow(int y, Ref<BitArray> row);

  size_t getDimension() const;
  size_t getWidth() const;
  size_t getHeight() const;

  unsigned int* getBits() const;

  friend std::ostream& operator<<(std::ostream &out, const BitMatrix &bm);
  const char *description();

private:
  BitMatrix(const BitMatrix&);
  BitMatrix& operator =(const BitMatrix&);
};

}

#endif // __BIT_MATRIX_H__
