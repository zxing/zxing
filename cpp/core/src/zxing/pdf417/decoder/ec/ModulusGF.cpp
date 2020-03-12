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
 * 2012-09-19 HFN translation from Java into C++
 */

#include "zxing/pdf417/decoder/ec/ModulusGF.h"

#include "zxing/common/Array.h"                     // for ArrayRef, Array
#include "zxing/common/Counted.h"                   // for Ref
#include "zxing/common/IllegalArgumentException.h"  // for IllegalArgumentException
#include "zxing/pdf417/decoder/ec/ModulusPoly.h"    // for ModulusPoly

#include <Utils/Macros.h>

namespace pping {
namespace pdf417 {

/**
 * The central Modulus Galois Field for PDF417 with prime number 929
 * and generator 3.
 */
ModulusGF ModulusGF::PDF417_GF(929,3);


/**
 * <p>A field based on powers of a generator integer, modulo some modulus.</p>
 *
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.GenericGF
 */
 
ModulusGF::ModulusGF(int modulus, int generator)
    : modulus_(modulus) {
    expTable_ = new Array<int>(modulus_);
    logTable_ = new Array<int>(modulus_);
    int x = 1,i;
    for (i = 0; i < modulus_; i++) {
      expTable_[i] = x;
      x = (x * generator) % modulus_;
    }
    for (i = 0; i < modulus_-1; i++) {
      logTable_[expTable_[i]] = i;
    }
    // logTable[0] == 0 but this should never be used
    ArrayRef<int>aZero(new Array<int>(1)),aOne(new Array<int>(1));
    aZero[0]=0;aOne[0]=1;

    /* calling ModulusPoly constructor directly beacuse coefficients are definitely not empty at this point */
    zero_ = new ModulusPoly(*this, aZero);
    one_ = new ModulusPoly(*this, aOne);
}
 
Ref<ModulusPoly> ModulusGF::getZero() {
    return zero_;
}

Ref<ModulusPoly> ModulusGF::getOne() {
    return one_;
}

FallibleRef<ModulusPoly> ModulusGF::buildMonomial(int degree, int coefficient) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    if (degree < 0) {
        return failure<IllegalArgumentException>("monomial: degree < 0!");
    }
    if (coefficient == 0) {
      return zero_;
    }
    int nCoefficients = degree + 1;
    ArrayRef<int> coefficients (new Array<int>(nCoefficients));
    coefficients[0] = coefficient;


    /* calling ModulusPoly constructor directly beacuse coefficients are definitely not empty at this point */
    Ref<ModulusPoly> result(new ModulusPoly(*this,coefficients));

    return result;
}

 

  int ModulusGF::add(int a, int b) {
    return (a + b) % modulus_;
  }

  int ModulusGF::subtract(int a, int b) {
    return (modulus_ + a - b) % modulus_;
  }

  int ModulusGF::exp(int a) {
    return expTable_[a];
  }

  Fallible<int> ModulusGF::log(int a) noexcept {
    if (a == 0) {
      return failure<IllegalArgumentException>("log of zero!");
    }
    return logTable_[a];
  }

  Fallible<int> ModulusGF::inverse(int a) noexcept {
    if (a == 0) {
      return failure<IllegalArgumentException>("inverse of zero!");;
    }
    return expTable_[modulus_ - logTable_[a] - 1];
  }

  int ModulusGF::multiply(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }
    return expTable_[(logTable_[a] + logTable_[b]) % (modulus_ - 1)];
  }

  int ModulusGF::getSize() {
    return modulus_;
  }

} /* namespace pdf417 */
} /* namespace zxing */
