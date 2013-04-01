// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#ifndef __BIT_ARRAY_H__
#define __BIT_ARRAY_H__

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

#include <zxing/common/Counted.h>
#include <zxing/common/IllegalArgumentException.h>
#include <vector>
#include <limits>
#include <iostream>

namespace zxing {
  class BitArray;
  std::ostream& operator << (std::ostream&, BitArray const&);
}

#define ZX_LOG_DIGITS(digits) \
    ((digits == 8) ? 3 : \
     ((digits == 16) ? 4 : \
      ((digits == 32) ? 5 : \
       ((digits == 64) ? 6 : \
        ((digits == 128) ? 7 : \
         (-1))))))

class zxing::BitArray : public Counted {
private:
  int size_;
  std::vector<int> bits_;
  static const int bitsPerWord_ =
    std::numeric_limits<unsigned int>::digits;
  static const int logBits_ = ZX_LOG_DIGITS(bitsPerWord_);
  static const int bitsMask_ = (1 << logBits_) - 1;
  static int wordsForBits(int bits);
  explicit BitArray();

public:
  BitArray(int size);
  ~BitArray();
  int getSize() const;

  bool get(int i) const {
    return (bits_[i >> logBits_] & (1 << (i & bitsMask_))) != 0;
  }

  void set(int i) {
    bits_[i >> logBits_] |= 1 << (i & bitsMask_);
  }

  int getNextSet(int from);
  int getNextUnset(int from);

  void setBulk(int i, int newBits);
  void setRange(int start, int end);
  void clear();
  bool isRange(int start, int end, bool value);
  std::vector<int>& getBitArray();
  
  void reverse();
  class Reverse;
};

class zxing::BitArray::Reverse {
private:
  Ref<BitArray> array;
public:
  Reverse(Ref<BitArray> array);
  ~Reverse();
};

#endif // __BIT_ARRAY_H__
