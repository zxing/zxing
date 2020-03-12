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

#include <zxing/common/IllegalArgumentException.h>  // for IllegalArgumentException
#include <zxing/common/reedsolomon/GenericGF.h>     // for GenericGF
#include <zxing/common/reedsolomon/GenericGFPoly.h>

#include "zxing/common/Array.h"                     // for ArrayRef, Array
#include "zxing/common/Counted.h"                   // for Ref

#include <Utils/Macros.h>

using pping::GenericGFPoly;
using pping::GenericGF;
using pping::ArrayRef;
using pping::Ref;

GenericGFPoly::GenericGFPoly(pping::GenericGF &field,
                             ArrayRef<int> coefficients) MB_NOEXCEPT_EXCEPT_BADALLOC
  :  field_(field) {

  MB_ASSERTM(coefficients.size() != 0, "%s", "GenericGFPoly requires coefficients");

  int coefficientsLength = (int)coefficients.size();
  if (coefficientsLength > 1 && coefficients[0] == 0) {
    // Leading term must be non-zero for anything except the constant polynomial "0"
    int firstNonZero = 1;
    while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
      firstNonZero++;
    }
    if (firstNonZero == coefficientsLength) {
      coefficients_ = field.getZero()->getCoefficients();
    } else {
      coefficients_ = ArrayRef<int>(new Array<int>(coefficientsLength-firstNonZero));
      for (int i = 0; i < (int)coefficients_.size(); i++) {
        coefficients_[i] = coefficients[i + firstNonZero];
      }
    }
  } else {
    coefficients_ = coefficients;
  }
}

pping::FallibleRef<pping::GenericGFPoly> GenericGFPoly::createGenericGFPoly(pping::GenericGF &field, ArrayRef<int> coefficients) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    if (coefficients.size() == 0) {
      return failure<IllegalArgumentException>("need coefficients");
    }
    return new GenericGFPoly(field, coefficients);
}
  
ArrayRef<int> GenericGFPoly::getCoefficients() {
  return coefficients_;
}
  
int GenericGFPoly::getDegree() {
  return (int)coefficients_.size() - 1;
}
  
bool GenericGFPoly::isZero() {
  return coefficients_[0] == 0;
}
  
int GenericGFPoly::getCoefficient(int degree) {
  return coefficients_[coefficients_.size() - 1 - degree];
}
  
pping::Fallible<int> GenericGFPoly::evaluateAt(int a) MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (a == 0) {
    // Just return the x^0 coefficient
    return getCoefficient(0);
  }
    
  int size = (int)coefficients_.size();
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

    auto const tryMult(field_.multiply(a, result));
    if(!tryMult)
        return tryMult.error();

    result = GenericGF::addOrSubtract(*tryMult, coefficients_[i]);
  }
  return result;
}
  
pping::FallibleRef<GenericGFPoly> GenericGFPoly::addOrSubtract(Ref<pping::GenericGFPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (!(&field_ == &other->field_)) {
    return failure<IllegalArgumentException>("GenericGFPolys do not have same GenericGF field");
  }
  if (isZero()) {
    return other;
  }
  if (other->isZero()) {
    return Ref<GenericGFPoly>(this);
  }
    
  ArrayRef<int> smallerCoefficients = coefficients_;
  ArrayRef<int> largerCoefficients = other->getCoefficients();
  if (smallerCoefficients.size() > largerCoefficients.size()) {
    ArrayRef<int> temp = smallerCoefficients;
    smallerCoefficients = largerCoefficients;
    largerCoefficients = temp;
  }
    
  ArrayRef<int> sumDiff(new Array<int>(largerCoefficients.size()));
  int lengthDiff = (int)largerCoefficients.size() - (int)smallerCoefficients.size();
  // Copy high-order terms only found in higher-degree polynomial's coefficients
  for (int i = 0; i < lengthDiff; i++) {
    sumDiff[i] = largerCoefficients[i];
  }
    
  for (int i = lengthDiff; i < (int)largerCoefficients.size(); i++) {
    sumDiff[i] = GenericGF::addOrSubtract(smallerCoefficients[i-lengthDiff],
                                          largerCoefficients[i]);
  }
    
  return createGenericGFPoly(field_, sumDiff);
}
  
pping::FallibleRef<GenericGFPoly> GenericGFPoly::multiply(Ref<pping::GenericGFPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (!(&field_ == &other->field_)) {
    return failure<IllegalArgumentException>("GenericGFPolys do not have same GenericGF field");
  }
    
  if (isZero() || other->isZero()) {
    return field_.getZero();
  }
    
  ArrayRef<int> aCoefficients = coefficients_;
  int aLength = (int)aCoefficients.size();
    
  ArrayRef<int> bCoefficients = other->getCoefficients();
  int bLength = (int)bCoefficients.size();

  if(aLength + bLength < 1) return failure<IllegalArgumentException>("Invalid coefficients length");
    
  ArrayRef<int> product(new Array<int>(aLength + bLength - 1));
  for (int i = 0; i < aLength; i++) {
    int aCoeff = aCoefficients[i];
    for (int j = 0; j < bLength; j++) {

      auto const tryMult(field_.multiply(aCoeff, bCoefficients[j]));
      if(!tryMult)
          return tryMult.error();

      product[i+j] = GenericGF::addOrSubtract(product[i+j], *tryMult);
    }
  }
  return createGenericGFPoly(field_, product);
}
  
pping::FallibleRef<GenericGFPoly> GenericGFPoly::multiply(int scalar) MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (scalar == 0) {
    return field_.getZero();
  }
  if (scalar == 1) {
    return Ref<GenericGFPoly>(this);
  }
  int size = (int)coefficients_.size();
  ArrayRef<int> product(new Array<int>(size));
  for (int i = 0; i < size; i++) {

    auto const tryMult(field_.multiply(coefficients_[i], scalar));
    if(!tryMult)
        return tryMult.error();

    product[i] = *tryMult;
  }
  return createGenericGFPoly(field_, product);
}
  
pping::FallibleRef<GenericGFPoly> GenericGFPoly::multiplyByMonomial(int degree, int coefficient) MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (degree < 0) {
    return failure<IllegalArgumentException>("degree must not be less then 0");
  }
  if (coefficient == 0) {
    return field_.getZero();
  }
  int size = (int)coefficients_.size();
  ArrayRef<int> product(new Array<int>(size+degree));
  for (int i = 0; i < size; i++) {

    auto const tryMult(field_.multiply(coefficients_[i], coefficient));
    if(!tryMult)
        return tryMult.error();

    product[i] = *tryMult;
  }
  return createGenericGFPoly(field_, product);
}
  
pping::Fallible<std::vector<Ref<GenericGFPoly>>> GenericGFPoly::divide(Ref<GenericGFPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (!(&field_ == &other->field_))
    return failure<IllegalArgumentException>("GenericGFPolys do not have same GenericGF field");

  if (other->isZero())
    return failure<IllegalArgumentException>("divide by 0");
    
  auto const tryGetZero(field_.getZero());
  if(!tryGetZero)
      return tryGetZero.error();

  Ref<GenericGFPoly> quotient = *tryGetZero;
  Ref<GenericGFPoly> remainder = Ref<GenericGFPoly>(this);
    
  int denominatorLeadingTerm = other->getCoefficient(other->getDegree());
  if(denominatorLeadingTerm == 0)
      return failure<IllegalArgumentException>("Denominator leading term is zero");

  auto const inverseDenominatorLeadingTerm(field_.inverse(denominatorLeadingTerm));
  if(!inverseDenominatorLeadingTerm)
      return inverseDenominatorLeadingTerm.error();
    
  while (remainder->getDegree() >= other->getDegree() && !remainder->isZero()) {
    int degreeDifference = remainder->getDegree() - other->getDegree();
    auto const tryMultRemainder(field_.multiply(remainder->getCoefficient(remainder->getDegree()),
                                       *inverseDenominatorLeadingTerm));
    if(!tryMultRemainder)
        return tryMultRemainder.error();

    int scale = *tryMultRemainder;

    auto const tryMult(other->multiplyByMonomial(degreeDifference, scale));
    if(!tryMult)
        return tryMult.error();
    Ref<GenericGFPoly> term = *tryMult;

    auto const tryBuildMonomial(field_.buildMonomial(degreeDifference,
                                                     scale));
    if(!tryBuildMonomial)
        return tryBuildMonomial.error();

    Ref<GenericGFPoly> iterationQuotiont = *tryBuildMonomial;

    auto const tryOp1(quotient->addOrSubtract(iterationQuotiont));
    if(!tryOp1)
        return tryOp1.error();

    quotient = *tryOp1;

    auto const tryOp2(remainder->addOrSubtract(term));
    if(!tryOp2)
        return tryOp2.error();

    remainder = *tryOp2;
  }
    
  std::vector<Ref<GenericGFPoly> > returnValue;
  returnValue[0] = quotient;
  returnValue[1] = remainder;
  return returnValue;
}
