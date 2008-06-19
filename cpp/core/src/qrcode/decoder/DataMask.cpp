/*
 *  DataMask.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 19/05/2008.
 *  Copyright 2008 Google Inc. All rights reserved.
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

#include "DataMask.h"
#include "../../common/IllegalArgumentException.h"

namespace qrcode {
  namespace decoder {
    
    
    vector<DataMask*> DataMask::DATA_MASKS;
    static int N_DATA_MASKS = DataMask::buildDataMasks();
    
    DataMask &DataMask::forReference(int reference) {
      if (reference < 0 || reference > 7) {
        throw new IllegalArgumentException("reference must be between 0 and 7");
      }
      return *DATA_MASKS[reference];
    }
    
    
    class DataMask000 : public DataMask {
    private:
      static unsigned int BITMASK;
    public:
      DataMask000() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits, 
                                   int dimension) {
        size_t max = bits.size();
        for (size_t i = 0; i < max; i++) {
          bits[i] ^= BITMASK;
        }
      }
    };
    unsigned int DataMask000::BITMASK = 0x55555555U;    
    
    class DataMask001 : public DataMask {
    public:
      DataMask001() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits, 
                                   int dimension) {
        unsigned int bitMask = 0;
        int count = 0;
        int offset = 0;
        for (int j = 0; j < dimension; j++) {
          for (int i = 0; i < dimension; i++) {
            if ((i & 0x01) == 0) {
              bitMask |= 1 << count;
            }
            if (++count == 32) {
              bits[offset++] ^= bitMask;
              count = 0;
              bitMask = 0;
            }
          }
        }
        bits[offset] ^= bitMask;
      }
    };
    
    
    class DataMask010 : public DataMask {
    public:
      DataMask010() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits, 
                                   int dimension) {
        unsigned bitMask = 0;
        int count = 0;
        int offset = 0;
        for (int j = 0; j < dimension; j++) {
          bool columnMasked = j % 3 == 0;
          for (int i = 0; i < dimension; i++) {
            if (columnMasked) {
              bitMask |= 1 << count;
            }
            if (++count == 32) {
              bits[offset++] ^= bitMask;
              count = 0;
              bitMask = 0;
            }
          }
        }
        bits[offset] ^= bitMask;
      }
    };
    
    
    class DataMask011 : public DataMask {
    public:
      DataMask011() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits, 
                                   int dimension) {
        unsigned int bitMask = 0;
        int count = 0;
        int offset = 0;
        for (int j = 0; j < dimension; j++) {
          for (int i = 0; i < dimension; i++) {
            if ((i + j) % 3 == 0) {
              bitMask |= 1 << count;
            }
            if (++count == 32) {
              bits[offset++] ^= bitMask;
              count = 0;
              bitMask = 0;
            }
          }
        }
        bits[offset] ^= bitMask;
      }
    };
    
    
    class DataMask100 : public DataMask {
    public:
      DataMask100() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits, 
                                   int dimension) {
        unsigned int bitMask = 0;
        int count = 0;
        int offset = 0;
        for (int j = 0; j < dimension; j++) {
          int jComponentParity = (j / 3) & 0x01;
          for (int i = 0; i < dimension; i++) {
            if (((i >> 1) & 0x01) == jComponentParity) {
              bitMask |= 1 << count;
            }
            if (++count == 32) {
              bits[offset++] ^= bitMask;
              count = 0;
              bitMask = 0;
            }
          }
        }
        bits[offset] ^= bitMask;
      }
    };
    
    
    class DataMask101 : public DataMask {
    public:
      DataMask101() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits, 
                                   int dimension) {
        unsigned int bitMask = 0;
        int count = 0;
        int offset = 0;
        for (int j = 0; j < dimension; j++) {
          for (int i = 0; i < dimension; i++) {
            int product = i * j;
            if (((product & 0x01) == 0) && product % 3 == 0) {
              bitMask |= 1 << count;
            }
            if (++count == 32) {
              bits[offset++] ^= bitMask;
              count = 0;
              bitMask = 0;
            }
          }
        }
        bits[offset] ^= bitMask;
      }
    };
    
    
    class DataMask110 : public DataMask {
    public:
      DataMask110() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits, 
                                   int dimension) {
        unsigned int bitMask = 0;
        int count = 0;
        int offset = 0;
        for (int j = 0; j < dimension; j++) {
          for (int i = 0; i < dimension; i++) {
            int product = i * j;
            if ((((product & 0x01) + product % 3) & 0x01) == 0) {
              bitMask |= 1 << count;
            }
            if (++count == 32) {
              bits[offset++] ^= bitMask;
              count = 0;
              bitMask = 0;
            }
          }
        }
        bits[offset] ^= bitMask;
      }
    };
    
    
    class DataMask111 : public DataMask {
    public:
      DataMask111() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits, 
                                   int dimension) {
        unsigned int bitMask = 0;
        int count = 0;
        int offset = 0;
        for (int j = 0; j < dimension; j++) {
          for (int i = 0; i < dimension; i++) {
            if (((((i + j) & 0x01) + (i * j) % 3) & 0x01) == 0) {
              bitMask |= 1 << count;
            }
            if (++count == 32) {
              bits[offset++] ^= bitMask;
              count = 0;
              bitMask = 0;
            }
          }
        }
        bits[offset] ^= bitMask;
      }
    };
    
    int DataMask::buildDataMasks() {
      DATA_MASKS.push_back(new DataMask000());
      DATA_MASKS.push_back(new DataMask001());
      DATA_MASKS.push_back(new DataMask010());
      DATA_MASKS.push_back(new DataMask011());
      DATA_MASKS.push_back(new DataMask100());
      DATA_MASKS.push_back(new DataMask101());
      DATA_MASKS.push_back(new DataMask110());
      DATA_MASKS.push_back(new DataMask111());
      return DATA_MASKS.size();
    }    
    
  }
}
