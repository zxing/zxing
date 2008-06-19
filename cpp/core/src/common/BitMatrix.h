#ifndef __BIT_MATRIX_H__
#define __BIT_MATRIX_H__

/*
 *  BitMatrix.h
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

#include "Counted.h"
#include <valarray>

namespace common {
  class BitMatrix : public Counted {
  private:
    
  public:
    size_t dimension_;
    valarray<unsigned int> bits_;
    BitMatrix(size_t dimension);
    ~BitMatrix();
    bool get(size_t i, size_t j);
    void set(size_t i, size_t j);
    void setRegion(size_t topI, size_t leftJ, size_t height, size_t width);
    size_t getDimension();
    valarray<unsigned int> &getBits();
    friend ostream& operator<<(ostream &out, BitMatrix &bm);
    const char *description();
  };
}

#endif // __BIT_MATRIX_H__
