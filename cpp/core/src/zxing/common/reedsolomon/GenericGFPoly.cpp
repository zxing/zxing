// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  GenericGFPoly.cpp
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
#include <zxing/common/reedsolomon/GenericGFPoly.h>
#include <zxing/common/reedsolomon/GenericGF.h>
#include <zxing/common/IllegalArgumentException.h>

using zxing::GenericGFPoly;
using zxing::ArrayRef;
using zxing::Ref;

// VC++
using zxing::GenericGF;

GenericGFPoly::GenericGFPoly(Ref<GenericGF> field,
                             ArrayRef<int> coefficients)
  :  field_(field) {
  if (coefficients->size() == 0) {
    throw IllegalArgumentException("need coefficients");
  }
  int coefficientsLength = coefficients->size();
  if (coefficientsLength > 1 && coefficients[0] == 0) {
    // Leading term must be non-zero for anything except the constant polynomial "0"
    int firstNonZero = 1;
    while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
      firstNonZero++;
    }
    if (firstNonZero == coefficientsLength) {
      coefficients_ = field->getZero()->getCoefficients();
    } else {
      coefficients_ = ArrayRef<int>(new Array<int>(coefficientsLength-firstNonZero));
      for (int i = 0; i < (int)coefficients_->size(); i++) {
        coefficients_[i] = coefficients[i + firstNonZero];
      }
    }
  } else {
    coefficients_ = coefficients;
  }
}
  
ArrayRef<int> GenericGFPoly::getCoefficients() {
  return coefficients_;
}
  
int GenericGFPoly::getDegree() {
  return coefficients_->size() - 1;
}
  
bool GenericGFPoly::isZero() {
  return coefficients_[0] == 0;
}
  
int GenericGFPoly::getCoefficient(int degree) {
  return coefficients_[coefficients_->size() - 1 - degree];
}
  
int GenericGFPoly::evaluateAt(int a) {
  if (a == 0) {
    // Just return the x^0 coefficient
    return getCoefficient(0);
  }
    
  int size = coefficients_->size();
  if (a == 1) {
    // Just the sum of the coefficients
    int result = 0;
    for (int i = 0; i < size; i++) {
      result = GenericGF::addOrSubtract(result, coefficients_[i]);
    }
    return result;
  }
  int result = coefficients_[0];
  for (int i = 1; i < size; i++) {
    result = GenericGF::addOrSubtract(field_->multiply(a, result), coefficients_[i]);
  }
  return result;
}
  
Ref<GenericGFPoly> GenericGFPoly::addOrSubtract(Ref<zxing::GenericGFPoly> other) {
  if (!(field_.object_ == other->field_.object_)) {
    throw IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
  }
  if (isZero()) {
    return other;
  }
  if (other->isZero()) {
    return Ref<GenericGFPoly>(this);
  }
    
  ArrayRef<int> smallerCoefficients = coefficients_;
  ArrayRef<int> largerCoefficients = other->getCoefficients();
  if (smallerCoefficients->size() > largerCoefficients->size()) {
    ArrayRef<int> temp = smallerCoefficients;
    smallerCoefficients = largerCoefficients;
    largerCoefficients = temp;
  }
    
  ArrayRef<int> sumDiff(new Array<int>(largerCoefficients->size()));
  int lengthDiff = largerCoefficients->size() - smallerCoefficients->size();
  // Copy high-order terms only found in higher-degree polynomial's coefficients
  for (int i = 0; i < lengthDiff; i++) {
    sumDiff[i] = largerCoefficients[i];
  }
    
  for (int i = lengthDiff; i < (int)largerCoefficients->size(); i++) {
    sumDiff[i] = GenericGF::addOrSubtract(smallerCoefficients[i-lengthDiff],
                                          largerCoefficients[i]);
  }
    
  return Ref<GenericGFPoly>(new GenericGFPoly(field_, sumDiff));
}
  
Ref<GenericGFPoly> GenericGFPoly::multiply(Ref<zxing::GenericGFPoly> other) {
  if (!(field_.object_ == other->field_.object_)) {
    throw IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
  }
    
  if (isZero() || other->isZero()) {
    return field_->getZero();
  }
    
  ArrayRef<int> aCoefficients = coefficients_;
  int aLength = aCoefficients->size();
    
  ArrayRef<int> bCoefficients = other->getCoefficients();
  int bLength = bCoefficients->size();
    
  ArrayRef<int> product(new Array<int>(aLength + bLength - 1));
  for (int i = 0; i < aLength; i++) {
    int aCoeff = aCoefficients[i];
    for (int j = 0; j < bLength; j++) {
      product[i+j] = GenericGF::addOrSubtract(product[i+j], 
                                              field_->multiply(aCoeff, bCoefficients[j]));
    }
  }
    
  return Ref<GenericGFPoly>(new GenericGFPoly(field_, product));
}
  
Ref<GenericGFPoly> GenericGFPoly::multiply(int scalar) {
  if (scalar == 0) {
    return field_->getZero();
  }
  if (scalar == 1) {
    return Ref<GenericGFPoly>(this);
  }
  int size = coefficients_->size();
  ArrayRef<int> product(new Array<int>(size));
  for (int i = 0; i < size; i++) {
    product[i] = field_->multiply(coefficients_[i], scalar);
  }
  return Ref<GenericGFPoly>(new GenericGFPoly(field_, product));
}
  
Ref<GenericGFPoly> GenericGFPoly::multiplyByMonomial(int degree, int coefficient) {
  if (degree < 0) {
    throw IllegalArgumentException("degree must not be less then 0");
  }
  if (coefficient == 0) {
    return field_->getZero();
  }
  int size = coefficients_->size();
  ArrayRef<int> product(new Array<int>(size+degree));
  for (int i = 0; i < size; i++) {
    product[i] = field_->multiply(coefficients_[i], coefficient);
  }
  return Ref<GenericGFPoly>(new GenericGFPoly(field_, product));
}
  
std::vector<Ref<GenericGFPoly> > GenericGFPoly::divide(Ref<GenericGFPoly> other) {
  if (!(field_.object_ == other->field_.object_)) {
    throw IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
  }
  if (other->isZero()) {
    throw IllegalArgumentException("divide by 0");
  }
    
  Ref<GenericGFPoly> quotient = field_->getZero();
  Ref<GenericGFPoly> remainder = Ref<GenericGFPoly>(this);
    
  int denominatorLeadingTerm = other->getCoefficient(other->getDegree());
  int inverseDenominatorLeadingTerm = field_->inverse(denominatorLeadingTerm);
    
  while (remainder->getDegree() >= other->getDegree() && !remainder->isZero()) {
    int degreeDifference = remainder->getDegree() - other->getDegree();
    int scale = field_->multiply(remainder->getCoefficient(remainder->getDegree()),
                                 inverseDenominatorLeadingTerm);
    Ref<GenericGFPoly> term = other->multiplyByMonomial(degreeDifference, scale);
    Ref<GenericGFPoly> iterationQuotiont = field_->buildMonomial(degreeDifference,
                                                                 scale);
    quotient = quotient->addOrSubtract(iterationQuotiont);
    remainder = remainder->addOrSubtract(term);
  }
    
  std::vector<Ref<GenericGFPoly> > returnValue;
  returnValue[0] = quotient;
  returnValue[1] = remainder;
  return returnValue;
}
