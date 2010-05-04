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

#include <zxing/common/Counted.h>
#include <limits>

namespace zxing {

class BitMatrix : public Counted {
private:
  size_t width_;
  size_t height_;
  size_t words_;
  unsigned int* bits_;

public:
  BitMatrix(size_t dimension);
  BitMatrix(size_t width, size_t height);

  ~BitMatrix();
  // Inlining this does not really improve performance.
  bool get(size_t x, size_t y) const;
  void set(size_t x, size_t y);
  void flip(size_t x, size_t y);
  void clear();
  void setRegion(size_t left, size_t top, size_t width, size_t height);

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
