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

#include <zxing/common/Array.h>                   // for ArrayRef
#include <zxing/common/Counted.h>                 // for Ref
#include "zxing/common/Error.hpp"

#include "zxing/pdf417/decoder/ec/ModulusPoly.h"  // for ModulusPoly

namespace pping {
namespace pdf417 {

/**
 * <p>A field based on powers of a generator integer, modulo some modulus.</p>
 *
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.GenericGF
 */
class ModulusGF {

  public: 
    static ModulusGF PDF417_GF;

  private:
    ArrayRef<int> expTable_;
    ArrayRef<int> logTable_;
    Ref<ModulusPoly> zero_;
    Ref<ModulusPoly> one_;
    int modulus_;
    ModulusGF(int modulus, int generator);

  public:
    static FallibleRef<ModulusGF> createModulusGF(int modulus, int generator) MB_NOEXCEPT_EXCEPT_BADALLOC;
    Ref<ModulusPoly> getZero();
    Ref<ModulusPoly> getOne();
    FallibleRef<ModulusPoly> buildMonomial(int degree, int coefficient) MB_NOEXCEPT_EXCEPT_BADALLOC;

    int add(int a, int b);
    int subtract(int a, int b);
    int exp(int a);
    Fallible<int> log(int a) noexcept;
    Fallible<int> inverse(int a) noexcept;
    int multiply(int a, int b);
    int getSize();
  
};

} /* namespace pdf417 */
} /* namespace zxing */

