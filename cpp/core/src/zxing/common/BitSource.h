#pragma once

/*
 *  BitSource.h
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

#include <zxing/common/Array.h>    // for ArrayRef

#include "zxing/common/Counted.h"  // for Counted
#include "zxing/common/Error.hpp"

namespace pping {
/**
 * <p>This provides an easy abstraction to read bits at a time from a sequence of bytes, where the
 * number of bits read is not often a multiple of 8.</p>
 *
 * <p>This class is not thread-safe.</p>
 *
 * @author srowen@google.com (Sean Owen)
 * @author christian.brunschen@gmail.com (Christian Brunschen)
 */
class BitSource : public Counted {
  typedef unsigned char byte;
private:
  ArrayRef<byte> bytes_;
  int byteOffset_;
  int bitOffset_;
public:
  /**
   * @param bytes bytes from which this will read bits. Bits will be read from the first byte first.
   * Bits are read within a byte from most-significant to least-significant bit.
   */
  BitSource(ArrayRef<byte> &bytes) :
      bytes_(bytes), byteOffset_(0), bitOffset_(0) {
  }

  int getByteOffset() {
    return byteOffset_;
  }

  /**
   * @param numBits number of bits to read
   * @return int representing the bits read. The bits will appear as the least-significant
   *         bits of the int
   */
  Fallible<int> readBits(int numBits) MB_NOEXCEPT_EXCEPT_BADALLOC;

  /**
   * @return number of bits that can be read successfully
   */
  int available();
};

}

