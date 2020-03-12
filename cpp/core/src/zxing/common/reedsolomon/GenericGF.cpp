// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  GenericGF.cpp
 *  zxing
 *
 *  Created by Lukas Stabe on 13/02/2012.
 *  Copyright 2012 ZXing authors All rights reserved.
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
#include "GenericGF.h"

#include <zxing/common/IllegalArgumentException.h>   // for IllegalArgumentException
#include <zxing/common/reedsolomon/GenericGFPoly.h>  // for GenericGFPoly

#include "zxing/common/Array.h"                      // for ArrayRef, Array
#include "zxing/common/Counted.h"                    // for Ref

#include <Utils/Macros.h>

using pping::GenericGF;
using pping::GenericGFPoly;
using pping::Ref;

Ref<GenericGF> GenericGF::QR_CODE_FIELD_256(new GenericGF(0x011D, 256, 0));
Ref<GenericGF> GenericGF::DATA_MATRIX_FIELD_256(new GenericGF(0x012D, 256, 1));
Ref<GenericGF> GenericGF::AZTEC_PARAM(new GenericGF(0x13, 16, 1));
Ref<GenericGF> GenericGF::AZTEC_DATA_6(new GenericGF(0x43, 64, 1));
Ref<GenericGF> GenericGF::AZTEC_DATA_8(GenericGF::DATA_MATRIX_FIELD_256);
Ref<GenericGF> GenericGF::AZTEC_DATA_10(new GenericGF(0x409, 1024, 1));
Ref<GenericGF> GenericGF::AZTEC_DATA_12(new GenericGF(0x1069, 4096, 1));
Ref<GenericGF> GenericGF::MAXICODE_FIELD_64(GenericGF::AZTEC_DATA_6);
  
namespace {
  int INITIALIZATION_THRESHOLD = 0;
}

/* Dont't use this directly, use the factory method! */
GenericGF::GenericGF(int primitive, int size, int b) MB_NOEXCEPT_EXCEPT_BADALLOC
  : size_(size), primitive_(primitive), generatorBase_(b), initialized_(false) {}

pping::FallibleRef<GenericGF> GenericGF::createGenericGF(int primitive, int size, int b) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    auto genericGF(Ref<GenericGF>(new GenericGF(primitive, size, b)));

    if (size <= INITIALIZATION_THRESHOLD) {
        auto const init(genericGF->initialize());
        if(!init)
            return init.error();
    }
    return genericGF;
}
  
pping::Fallible<void> GenericGF::initialize() MB_NOEXCEPT_EXCEPT_BADALLOC {
  expTable_.resize(size_);
  logTable_.resize(size_);
    
  int x = 1;
    
  for (int i = 0; i < size_; i++) {
    expTable_[i] = x;
    x <<= 1; // x = x * 2; we're assuming the generator alpha is 2
    if (x >= size_) {
      x ^= primitive_;
      x &= size_-1;
    }
  }
  for (int i = 0; i < size_-1; i++) {
    logTable_[expTable_[i]] = i;
  }
  //logTable_[0] == 0 but this should never be used
    
  auto const tryCreateGenericPoly(GenericGFPoly::createGenericGFPoly(*this, ArrayRef<int>(new Array<int>(1))));
  if(!tryCreateGenericPoly)
      return tryCreateGenericPoly.error();

  zero_ = *tryCreateGenericPoly;
  zero_->getCoefficients()[0] = 0;

  auto const tryCreateGenericPolyOne(GenericGFPoly::createGenericGFPoly(*this, ArrayRef<int>(new Array<int>(1))));
  if(!tryCreateGenericPolyOne)
      return tryCreateGenericPolyOne.error();

  one_ = *tryCreateGenericPolyOne;
  one_->getCoefficients()[0] = 1;
    
  initialized_ = true;

  return success();
}
  
pping::Fallible<void> GenericGF::checkInit() MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (!initialized_) {
    auto const init(initialize());
    if(!init)
        return init.error();
  }
  return success();
}
  
pping::FallibleRef<GenericGFPoly> GenericGF::getZero() MB_NOEXCEPT_EXCEPT_BADALLOC {
  auto const check(checkInit());
  if(!check)
      return check.error();
  return zero_;
}
  
pping::FallibleRef<GenericGFPoly> GenericGF::getOne() MB_NOEXCEPT_EXCEPT_BADALLOC {
    auto const check(checkInit());
    if(!check)
        return check.error();
    return one_;
}
  
pping::FallibleRef<GenericGFPoly> GenericGF::buildMonomial(int degree, int coefficient) MB_NOEXCEPT_EXCEPT_BADALLOC {
  auto const check(checkInit());
  if(!check)
    return check.error();
    
  if (degree < 0) {
    return failure<IllegalArgumentException>("Degree must be non-negative");
  }
  if (coefficient == 0) {
    return zero_;
  }
  ArrayRef<int> coefficients(new Array<int>(degree + 1));
  coefficients[0] = coefficient;
    
  return GenericGFPoly::createGenericGFPoly(*this, coefficients);
}
  
int GenericGF::addOrSubtract(int a, int b) {
  return a ^ b;
}
  
pping::Fallible<int> GenericGF::exp(int a) MB_NOEXCEPT_EXCEPT_BADALLOC {
  auto const check(checkInit());
  if(!check)
    return check.error();

  return expTable_[a];
}
  
pping::Fallible<int> GenericGF::log(int a) MB_NOEXCEPT_EXCEPT_BADALLOC {
    if(a == 0)
        return failure<IllegalArgumentException>("cannot give log(0)");

    auto const check(checkInit());
    if(!check)
      return check.error();

    return logTable_[a];
}
  
pping::Fallible<int> GenericGF::inverse(int a) MB_NOEXCEPT_EXCEPT_BADALLOC {
    if(a == 0)
        return failure<IllegalArgumentException>("Cannot calculate the inverse of 0");

    auto const check(checkInit());
    if(!check)
      return check.error();

    return expTable_[size_ - logTable_[a] - 1];
}
  
pping::Fallible<int> GenericGF::multiply(int a, int b) MB_NOEXCEPT_EXCEPT_BADALLOC {
  auto const check(checkInit());
  if(!check)
    return check.error();
    
  if (a == 0 || b == 0) {
    return 0;
  }
    
  return expTable_[(logTable_[a] + logTable_[b]) % (size_ - 1)];
  }
    
int GenericGF::getSize() {
  return size_;
}

int GenericGF::getGeneratorBase() {
  return generatorBase_;
}
