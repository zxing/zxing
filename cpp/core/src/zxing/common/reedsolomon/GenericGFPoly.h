// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  GenericGFPoly.h
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

#pragma once

#include <zxing/common/Array.h>    // for ArrayRef
#include <zxing/common/Counted.h>  // for Ref, Counted

#include "zxing/common/Error.hpp"

#include <vector>                  // for vector

namespace pping {
  class GenericGF;
  
  class GenericGFPoly : public Counted {
  private:
    GenericGF& field_;
    ArrayRef<int> coefficients_;

    GenericGFPoly(GenericGF& field, ArrayRef<int> coefficients) MB_NOEXCEPT_EXCEPT_BADALLOC;
    
  public:
    static FallibleRef<GenericGFPoly> createGenericGFPoly(GenericGF& field, ArrayRef<int> coefficients) MB_NOEXCEPT_EXCEPT_BADALLOC;
    ArrayRef<int> getCoefficients();
    int getDegree();
    bool isZero();
    int getCoefficient(int degree);
    Fallible<int> evaluateAt(int a) MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<GenericGFPoly> addOrSubtract(Ref<GenericGFPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<GenericGFPoly> multiply(Ref<GenericGFPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<GenericGFPoly> multiply(int scalar) MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<GenericGFPoly> multiplyByMonomial(int degree, int coefficient) MB_NOEXCEPT_EXCEPT_BADALLOC;
    Fallible<std::vector<Ref<GenericGFPoly>>> divide(Ref<GenericGFPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC;
    
      //#warning todo: add print method
  };
}

