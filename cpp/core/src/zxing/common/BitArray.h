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

namespace zxing {

#define ZX_LOG_DIGITS(digits) \
    ((digits == 8) ? 3 : \
     ((digits == 16) ? 4 : \
      ((digits == 32) ? 5 : \
       ((digits == 64) ? 6 : \
        ((digits == 128) ? 7 : \
         (-1))))))

class BitArray : public Counted {
private:
  size_t size_;
  std::vector<unsigned int> bits_;
  static const unsigned int bitsPerWord_ =
    std::numeric_limits<unsigned int>::digits;
  static const unsigned int logBits_ = ZX_LOG_DIGITS(bitsPerWord_);
  static const unsigned int bitsMask_ = (1 << logBits_) - 1;
  static size_t wordsForBits(size_t bits);
  explicit BitArray();

public:
  BitArray(size_t size);
  ~BitArray();
  size_t getSize();

  bool get(size_t i) {
    return (bits_[i >> logBits_] & (1 << (i & bitsMask_))) != 0;
  }

  void set(size_t i) {
    bits_[i >> logBits_] |= 1 << (i & bitsMask_);
  }

  void setBulk(size_t i, unsigned int newBits);
  void setRange(int start, int end);
  void clear();
  bool isRange(size_t start, size_t end, bool value);
  std::vector<unsigned int>& getBitArray();
  void reverse();
};

}

#endif // __BIT_ARRAY_H__
