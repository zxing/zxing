#pragma once

/*
 *  Binarizer.h
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

#include <zxing/common/Counted.h>  // for Ref, Counted
#include "zxing/common/Error.hpp"

namespace pping {

class BitArray;
class BitMatrix;
class LuminanceSource;

class Binarizer : public Counted {
 private:
  Ref<LuminanceSource> source_;

 public:
  Binarizer(Ref<LuminanceSource> source) noexcept;
  virtual ~Binarizer() = default;

  virtual FallibleRef<BitArray > getBlackRow   (int y, Ref<BitArray> row) const MB_NOEXCEPT_EXCEPT_BADALLOC = 0;
  virtual FallibleRef<BitMatrix> getBlackMatrix(                        ) const MB_NOEXCEPT_EXCEPT_BADALLOC = 0;

  auto const & getLuminanceSource() const noexcept { return source_; }
  virtual Ref<Binarizer> createBinarizer(Ref<LuminanceSource> source) const MB_NOEXCEPT_EXCEPT_BADALLOC = 0;

  int getWidth () const noexcept;
  int getHeight() const noexcept;

};

}
