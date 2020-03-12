#pragma once

/*
 * Copyright 2012 ZXing authors
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
 *
 * 2012-09-17 HFN translation from Java into C++
 */

#include <zxing/common/Array.h>    // for ArrayRef
#include <zxing/common/Counted.h>  // for Ref, Counted
#include "zxing/common/Error.hpp"

#include <vector>                  // for vector

namespace pping {
namespace pdf417 {

class ModulusGF;

/**
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.GenericGFPoly
 */
class ModulusPoly: public Counted {

  /**
   * We will allow ModulusGF to use our private constructor because it promises to worry about
   * possible failures
   */
  friend ModulusGF;

  private:
    ModulusGF &field_;
    ArrayRef<int> coefficients_;
    ModulusPoly(ModulusGF& field, ArrayRef<int> coefficients) MB_NOEXCEPT_EXCEPT_BADALLOC;
  public:
    static FallibleRef<ModulusPoly> createModulusPoly(ModulusGF& field, ArrayRef<int> coefficients) MB_NOEXCEPT_EXCEPT_BADALLOC;
    ~ModulusPoly();
    ArrayRef<int> getCoefficients();
    int getDegree();
    bool isZero();
    int getCoefficient(int degree);
    int evaluateAt(int a);
    FallibleRef<ModulusPoly> add(Ref<ModulusPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<ModulusPoly> subtract(Ref<ModulusPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<ModulusPoly> multiply(Ref<ModulusPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<ModulusPoly> negative();
    FallibleRef<ModulusPoly> multiply(int scalar);
    FallibleRef<ModulusPoly> multiplyByMonomial(int degree, int coefficient) MB_NOEXCEPT_EXCEPT_BADALLOC;
    Fallible<std::vector<Ref<ModulusPoly> > > divide(Ref<ModulusPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC;
    #if 0
    public String toString();
    #endif
};

} /* namespace pdf417 */
} /* namespace zxing */

