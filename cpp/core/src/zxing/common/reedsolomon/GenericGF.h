// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  GenericGF.h
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

#include <zxing/common/Counted.h>                    // for Ref, Counted
#include <vector>                                    // for vector

#include "zxing/common/reedsolomon/GenericGFPoly.h"  // for GenericGFPoly
#include "zxing/common/Error.hpp"

namespace pping {
  
  class GenericGF : public Counted {
    
  private:
    std::vector<int> expTable_;
    std::vector<int> logTable_;
    Ref<GenericGFPoly> zero_;
    Ref<GenericGFPoly> one_;
    int size_;
    int primitive_;
    int generatorBase_;
    bool initialized_;
    
    Fallible<void> initialize() MB_NOEXCEPT_EXCEPT_BADALLOC;
    Fallible<void> checkInit() MB_NOEXCEPT_EXCEPT_BADALLOC;

    GenericGF(int primitive, int size, int b) MB_NOEXCEPT_EXCEPT_BADALLOC;
    
  public:
    static Ref<GenericGF> AZTEC_DATA_12;
    static Ref<GenericGF> AZTEC_DATA_10;
    static Ref<GenericGF> AZTEC_DATA_8;
    static Ref<GenericGF> AZTEC_DATA_6;
    static Ref<GenericGF> AZTEC_PARAM;
    static Ref<GenericGF> QR_CODE_FIELD_256;
    static Ref<GenericGF> DATA_MATRIX_FIELD_256;
    static Ref<GenericGF> MAXICODE_FIELD_64;
    
    static FallibleRef<GenericGF> createGenericGF(int primitive, int size, int b) MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<GenericGFPoly> getZero() MB_NOEXCEPT_EXCEPT_BADALLOC;
    FallibleRef<GenericGFPoly> getOne() MB_NOEXCEPT_EXCEPT_BADALLOC;
    int getSize();
    int getGeneratorBase();
    FallibleRef<GenericGFPoly> buildMonomial(int degree, int coefficient) MB_NOEXCEPT_EXCEPT_BADALLOC;
    
    static int addOrSubtract(int a, int b);
    Fallible<int> exp(int a) MB_NOEXCEPT_EXCEPT_BADALLOC;
    Fallible<int> log(int a) MB_NOEXCEPT_EXCEPT_BADALLOC;
    Fallible<int> inverse(int a) MB_NOEXCEPT_EXCEPT_BADALLOC;
    Fallible<int> multiply(int a, int b) MB_NOEXCEPT_EXCEPT_BADALLOC;
      
    bool operator==(GenericGF other) {
      return (other.getSize() == this->size_ &&
              other.primitive_ == this->primitive_);
    }
    
    //#warning todo: add print method
    
  };
}


