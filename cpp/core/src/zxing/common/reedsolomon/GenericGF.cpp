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

#include <iostream>
#include <zxing/common/reedsolomon/GenericGF.h>
#include <zxing/common/reedsolomon/GenericGFPoly.h>
#include <zxing/common/IllegalArgumentException.h>

using zxing::GenericGF;
using zxing::GenericGFPoly;
using zxing::Ref;

Ref<GenericGF> GenericGF::QR_CODE_FIELD_256(new GenericGF(0x011D, 256));
Ref<GenericGF> GenericGF::DATA_MATRIX_FIELD_256(new GenericGF(0x012D, 256));
Ref<GenericGF> GenericGF::AZTEC_PARAM(new GenericGF(0x13, 16));
Ref<GenericGF> GenericGF::AZTEC_DATA_6(new GenericGF(0x43, 64));
Ref<GenericGF> GenericGF::AZTEC_DATA_8(GenericGF::DATA_MATRIX_FIELD_256);
Ref<GenericGF> GenericGF::AZTEC_DATA_10(new GenericGF(0x409, 1024));
Ref<GenericGF> GenericGF::AZTEC_DATA_12(new GenericGF(0x1069, 4096));
  
  
static int INITIALIZATION_THRESHOLD = 0;
  
GenericGF::GenericGF(int primitive, int size)
  : size_(size), primitive_(primitive), initialized_(false) {
  if (size <= INITIALIZATION_THRESHOLD) {
    initialize();
  }
}
  
void GenericGF::initialize() {
  //expTable_ = std::vector<int>(size_, (const int) 0);
  //logTable_ = std::vector<int>(size_, (const int) 0);
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
    
  zero_ = Ref<GenericGFPoly>(new GenericGFPoly(Ref<GenericGF>(this), ArrayRef<int>(new Array<int>(1))));
  zero_->getCoefficients()[0] = 0;
  one_ = Ref<GenericGFPoly>(new GenericGFPoly(Ref<GenericGF>(this), ArrayRef<int>(new Array<int>(1))));
  one_->getCoefficients()[0] = 1;
    
  initialized_ = true;
}
  
void GenericGF::checkInit() {
  if (!initialized_) {
    initialize();
  }
}
  
Ref<GenericGFPoly> GenericGF::getZero() {
  checkInit();
  return zero_;
}
  
Ref<GenericGFPoly> GenericGF::getOne() {
  checkInit();
  return one_;
}
  
Ref<GenericGFPoly> GenericGF::buildMonomial(int degree, int coefficient) {
  checkInit();
    
  if (degree < 0) {
    throw IllegalArgumentException("Degree must be non-negative");
  }
  if (coefficient == 0) {
    return zero_;
  }
  ArrayRef<int> coefficients(new Array<int>(degree + 1));
  coefficients[0] = coefficient;
    
  //return new GenericGFPoly(this, coefficients);
  return Ref<GenericGFPoly>(new GenericGFPoly(Ref<GenericGF>(this), coefficients));
}
  
int GenericGF::addOrSubtract(int a, int b) {
  return a ^ b;
}
  
int GenericGF::exp(int a) {
  checkInit();
  return expTable_[a];
}
  
int GenericGF::log(int a) {
  checkInit();
  if (a == 0) {
    throw IllegalArgumentException("cannot give log(0)");
  }
  return logTable_[a];
}
  
int GenericGF::inverse(int a) {
  checkInit();
  if (a == 0) {
    throw IllegalArgumentException("Cannot calculate the inverse of 0");
  }
  return expTable_[size_ - logTable_[a] - 1];
}
  
int GenericGF::multiply(int a, int b) {
  checkInit();
    
  if (a == 0 || b == 0) {
    return 0;
  }
    
  return expTable_[(logTable_[a] + logTable_[b]) % (size_ - 1)];
  }
    
int GenericGF::getSize() {
  return size_;
}
