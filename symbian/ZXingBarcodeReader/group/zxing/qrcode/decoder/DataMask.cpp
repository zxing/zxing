/*
 *  DataMask.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 19/05/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include <zxing/qrcode/decoder/DataMask.h>

#include <zxing/common/IllegalArgumentException.h>

namespace zxing {
namespace qrcode {

using namespace std;

DataMask::DataMask() {
}

DataMask::~DataMask() {
}

vector<Ref<DataMask> > DataMask::DATA_MASKS;
static int N_DATA_MASKS = DataMask::buildDataMasks();

DataMask &DataMask::forReference(int reference) {
  if (reference < 0 || reference > 7) {
    throw IllegalArgumentException("reference must be between 0 and 7");
  }
  return *DATA_MASKS[reference];
}

void DataMask::unmaskBitMatrix(BitMatrix& bits, size_t dimension) {
  for (size_t y = 0; y < dimension; y++) {
    for (size_t x = 0; x < dimension; x++) {
      // TODO: check why the coordinates have to be swapped
      if (isMasked(y, x)) {
        bits.flip(x, y);
      }
    }
  }
}

/**
 * 000: mask bits for which (x + y) mod 2 == 0
 */
class DataMask000 : public DataMask {
public:
  bool isMasked(size_t x, size_t y) {
    //		return ((x + y) & 0x01) == 0;
    return ((x + y) % 2) == 0;
  }
};

/**
 * 001: mask bits for which x mod 2 == 0
 */
class DataMask001 : public DataMask {
public:
  bool isMasked(size_t x, size_t y) {
    //		return (x & 0x01) == 0;
    return (x % 2) == 0;
  }
};

/**
 * 010: mask bits for which y mod 3 == 0
 */
class DataMask010 : public DataMask {
public:
  bool isMasked(size_t x, size_t y) {
    return y % 3 == 0;
  }
};

/**
 * 011: mask bits for which (x + y) mod 3 == 0
 */
class DataMask011 : public DataMask {
public:
  bool isMasked(size_t x, size_t y) {
    return (x + y) % 3 == 0;
  }
};

/**
 * 100: mask bits for which (x/2 + y/3) mod 2 == 0
 */
class DataMask100 : public DataMask {
public:
  bool isMasked(size_t x, size_t y) {
    //		return (((x >> 1) + (y / 3)) & 0x01) == 0;
    return (((x >> 1) + (y / 3)) % 2) == 0;
  }
};

/**
 * 101: mask bits for which xy mod 2 + xy mod 3 == 0
 */
class DataMask101 : public DataMask {
public:
  bool isMasked(size_t x, size_t y) {
    size_t temp = x * y;
    //		return (temp & 0x01) + (temp % 3) == 0;
    return (temp % 2) + (temp % 3) == 0;

  }
};

/**
 * 110: mask bits for which (xy mod 2 + xy mod 3) mod 2 == 0
 */
class DataMask110 : public DataMask {
public:
  bool isMasked(size_t x, size_t y) {
    size_t temp = x * y;
    //		return (((temp & 0x01) + (temp % 3)) & 0x01) == 0;
    return (((temp % 2) + (temp % 3)) % 2) == 0;
  }
};

/**
 * 111: mask bits for which ((x+y)mod 2 + xy mod 3) mod 2 == 0
 */
class DataMask111 : public DataMask {
public:
  bool isMasked(size_t x, size_t y) {
    //		return ((((x + y) & 0x01) + ((x * y) % 3)) & 0x01) == 0;
    return ((((x + y) % 2) + ((x * y) % 3)) % 2) == 0;
  }
};

int DataMask::buildDataMasks() {
  DATA_MASKS.push_back(Ref<DataMask> (new DataMask000()));
  DATA_MASKS.push_back(Ref<DataMask> (new DataMask001()));
  DATA_MASKS.push_back(Ref<DataMask> (new DataMask010()));
  DATA_MASKS.push_back(Ref<DataMask> (new DataMask011()));
  DATA_MASKS.push_back(Ref<DataMask> (new DataMask100()));
  DATA_MASKS.push_back(Ref<DataMask> (new DataMask101()));
  DATA_MASKS.push_back(Ref<DataMask> (new DataMask110()));
  DATA_MASKS.push_back(Ref<DataMask> (new DataMask111()));
  return DATA_MASKS.size();
}

}
}
