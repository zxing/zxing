/*
 *  BitSource.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 09/05/2008.
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

#include <zxing/common/BitSource.h>
#include <zxing/common/IllegalArgumentException.h>

namespace zxing {

int BitSource::readBits(int numBits) {
  if (numBits < 0 || numBits > 32) {
    throw IllegalArgumentException("cannot read <1 or >32 bits");
  } else if (numBits > available()) {
    throw IllegalArgumentException("reading more bits than are available");
  }

  int result = 0;

  // First, read remainder from current byte
  if (bitOffset_ > 0) {
    int bitsLeft = 8 - bitOffset_;
    int toRead = numBits < bitsLeft ? numBits : bitsLeft;
    int bitsToNotRead = bitsLeft - toRead;
    int mask = (0xFF >> (8 - toRead)) << bitsToNotRead;
    result = (bytes_[byteOffset_] & mask) >> bitsToNotRead;
    numBits -= toRead;
    bitOffset_ += toRead;
    if (bitOffset_ == 8) {
      bitOffset_ = 0;
      byteOffset_++;
    }
  }

  // Next read whole bytes
  if (numBits > 0) {
    while (numBits >= 8) {
      result = (result << 8) | (bytes_[byteOffset_] & 0xFF);
      byteOffset_++;
      numBits -= 8;
    }


    // Finally read a partial byte
    if (numBits > 0) {
      int bitsToNotRead = 8 - numBits;
      int mask = (0xFF >> bitsToNotRead) << bitsToNotRead;
      result = (result << numBits) | ((bytes_[byteOffset_] & mask) >> bitsToNotRead);
      bitOffset_ += numBits;
    }
  }

  return result;
}

int BitSource::available() {
  return 8 * (bytes_.size() - byteOffset_) - bitOffset_;
}
}
