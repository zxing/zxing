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
#include <zxing/common/Array.h>
#include <limits>

namespace zxing {
  class BitMatrix;
}

class zxing::BitMatrix : public Counted {
private:
  int width_;
  int height_;
  int words_;
  ArrayRef<int> bits_;

#define ZX_LOG_DIGITS(digits) \
    ((digits == 8) ? 3 : \
     ((digits == 16) ? 4 : \
      ((digits == 32) ? 5 : \
       ((digits == 64) ? 6 : \
        ((digits == 128) ? 7 : \
         (-1))))))

  static const int bitsPerWord =
    std::numeric_limits<unsigned int>::digits;
  static const int logBits = ZX_LOG_DIGITS(bitsPerWord);
  static const int bitsMask = (1 << logBits) - 1;

public:
  BitMatrix(int dimension);
  BitMatrix(int width, int height);

  ~BitMatrix();

  bool get(int x, int y) const {
    int offset = x + width_ * y;
    return ((bits_[offset >> logBits] >> (offset & bitsMask)) & 0x01) != 0;
  }

  void set(int x, int y) {
    int offset = x + width_ * y;
    bits_[offset >> logBits] |= 1 << (offset & bitsMask);
  }

  void flip(int x, int y);
  void clear();
  void setRegion(int left, int top, int width, int height);
  Ref<BitArray> getRow(int y, Ref<BitArray> row);

  int getDimension() const;
  int getWidth() const;
  int getHeight() const;

  ArrayRef<int> getBits() const;

  friend std::ostream& operator<<(std::ostream &out, const BitMatrix &bm);
  const char *description();

private:
  BitMatrix(const BitMatrix&);
  BitMatrix& operator =(const BitMatrix&);
};

#endif // __BIT_MATRIX_H__
