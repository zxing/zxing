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

#include <stddef.h>                                 // for size_t
#include <zxing/pdf417/decoder/ec/ModulusGF.h>      // for ModulusGF
#include <zxing/pdf417/decoder/ec/ModulusPoly.h>

#include "zxing/common/Array.h"                     // for ArrayRef, Array
#include "zxing/common/Counted.h"                   // for Ref
#include "zxing/common/IllegalArgumentException.h"  // for IllegalArgumentException

#include <Utils/Macros.h>

/**
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.GenericGFPoly
 */

namespace pping {
namespace pdf417 {


ModulusPoly::ModulusPoly(ModulusGF& field, ArrayRef<int> coefficients) MB_NOEXCEPT_EXCEPT_BADALLOC
: field_(field)
{
    MB_ASSERTM(coefficients.size() != 0, "%s", "ModulusPoly needs coefficients");

    int coefficientsLength = (int)coefficients.size();
    if (coefficientsLength > 1 && coefficients[0] == 0) {
      // Leading term must be non-zero for anything except the constant polynomial "0"
      int firstNonZero = 1;
      while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
        firstNonZero++;
      }
      if (firstNonZero == coefficientsLength) {
        coefficientsLength = (int)field_.getZero()->getCoefficients()->size();
        coefficients_.reset(new Array<int> (coefficientsLength));
        *coefficients_ = *(field_.getZero()->getCoefficients());
      } else {
        ArrayRef<int> c(coefficients);
        coefficientsLength -= firstNonZero;
        coefficients_.reset(new Array<int> (coefficientsLength));
        for (int i = 0; i < coefficientsLength; i++) {
          coefficients_[i] = c[i + firstNonZero];
        }
      /*
        coefficientsLength -= firstNonZero;
        coefficients_.reset(new Array<int>(coefficientsLength - firstNonZero));
        for (int i = 0; i < coefficientsLength; i++) {
          coefficients_[i] = coefficients[i + firstNonZero];
        }
      */
      }
    } else {
      coefficients_ = coefficients;
    }
}

FallibleRef<ModulusPoly> ModulusPoly::createModulusPoly(ModulusGF &field, ArrayRef<int> coefficients) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    if (coefficients.size() == 0) {
      return failure<IllegalArgumentException>("no coefficients!");
    }
    return new ModulusPoly(field, coefficients);
}

  ArrayRef<int> ModulusPoly::getCoefficients() {
    return coefficients_;
  }

  /**
   * @return degree of this polynomial
   */
  int ModulusPoly::getDegree() {
    return (int)coefficients_.size() - 1;
  }

  /**
   * @return true iff this polynomial is the monomial "0"
   */
  bool ModulusPoly::isZero() {
    return coefficients_[0] == 0;
  }

  /**
   * @return coefficient of x^degree term in this polynomial
   */
  int ModulusPoly::getCoefficient(int degree) {
    return coefficients_[coefficients_.size() - 1 - degree];
  }

  /**
   * @return evaluation of this polynomial at a given point
   */
  int ModulusPoly::evaluateAt(int a) {
    int i;
    if (a == 0) {
      // Just return the x^0 coefficient
      return getCoefficient(0);
    }
    int size = (int)coefficients_.size();
    if (a == 1) {
      // Just the sum of the coefficients
      int result = 0;
      for (i = 0; i < size; i++) {
        result = field_.add(result, coefficients_[i]);
      }
      return result;
    }
    int result = coefficients_[0];
    for (i = 1; i < size; i++) {
      result = field_.add(field_.multiply(a, result), coefficients_[i]);
    }
    return result;
  }

  FallibleRef<ModulusPoly> ModulusPoly::add(Ref<ModulusPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC {
    if (&field_ != &other->field_) {
      return failure<IllegalArgumentException>("ModulusPolys do not have same ModulusGF field");
    }
    if (isZero()) {
      return other;
    }
    if (other->isZero()) {
      return Ref<ModulusPoly>(this);
    }

    ArrayRef<int> smallerCoefficients = coefficients_;
    ArrayRef<int> largerCoefficients = other->coefficients_;
    if (smallerCoefficients.size() > largerCoefficients.size()) {
      ArrayRef<int> temp(smallerCoefficients);
      smallerCoefficients = largerCoefficients;
      largerCoefficients = temp;
    }
    ArrayRef<int>  sumDiff (new Array<int>(largerCoefficients.size()));
    int lengthDiff = (int)(largerCoefficients.size() - smallerCoefficients.size());
    // Copy high-order terms only found in higher-degree polynomial's coefficients
    for (int i = 0; i < lengthDiff; i++) {
        sumDiff[i] = largerCoefficients[i];
    }

    for (size_t i = lengthDiff; i < largerCoefficients.size(); i++) {
      sumDiff[i] = field_.add(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
    }

    return createModulusPoly(field_, sumDiff);
  }

  FallibleRef<ModulusPoly> ModulusPoly::subtract(Ref<ModulusPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC {
    if (&field_ != &other->field_) {
      return failure<IllegalArgumentException>("ModulusPolys do not have same ModulusGF field");
    }
    if (other->isZero()) {
      return Ref<ModulusPoly>(this);
    }
    auto const tryCreateNegative(other->negative());
    if(!tryCreateNegative)
        return tryCreateNegative.error();

    return add(*tryCreateNegative);
  }

  FallibleRef<ModulusPoly> ModulusPoly::multiply(Ref<ModulusPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC {
    if (&field_ != &other->field_) {
      return failure<IllegalArgumentException>("ModulusPolys do not have same ModulusGF field");
    }
    if (isZero() || other->isZero()) {
      return field_.getZero();
    }
    int i,j;
    ArrayRef<int> aCoefficients = coefficients_;
    int aLength = (int)aCoefficients.size();
    ArrayRef<int> bCoefficients = other->coefficients_;
    int bLength = (int)bCoefficients.size();
    ArrayRef<int> product (new Array<int>(aLength + bLength - 1));
    for (i = 0; i < aLength; i++) {
      int aCoeff = aCoefficients[i];
      for (j = 0; j < bLength; j++) {
        product[i + j] = field_.add(product[i + j], field_.multiply(aCoeff, bCoefficients[j]));
      }
    }
    return createModulusPoly(field_, product);
  }

  FallibleRef<ModulusPoly> ModulusPoly::negative() {
    int size = (int)coefficients_.size();
    ArrayRef<int> negativeCoefficients (new Array<int>(size));
    for (int i = 0; i < size; i++) {
      negativeCoefficients[i] = field_.subtract(0, coefficients_[i]);
    }
    return createModulusPoly(field_, negativeCoefficients);
  }

  FallibleRef<ModulusPoly> ModulusPoly::multiply(int scalar) {
    if (scalar == 0) {
      return field_.getZero();
    }
    if (scalar == 1) {
      return Ref<ModulusPoly>(this);
    }
    int size = (int)coefficients_.size();
    ArrayRef<int> product( new Array<int>(size));
    for (int i = 0; i < size; i++) {
      product[i] = field_.multiply(coefficients_[i], scalar);
    }
    return createModulusPoly(field_, product);
  }

  FallibleRef<ModulusPoly> ModulusPoly::multiplyByMonomial(int degree, int coefficient) MB_NOEXCEPT_EXCEPT_BADALLOC {
    if (degree < 0) {
      return failure<IllegalArgumentException>("negative degree!");
    }
    if (coefficient == 0) {
      return field_.getZero();
    }
    int size = (int)coefficients_.size();
    ArrayRef<int> product (new Array<int>(size + degree));
    for (int i = 0; i < size; i++) {
      product[i] = field_.multiply(coefficients_[i], coefficient);
    }
    return createModulusPoly(field_, product);
  }

  Fallible<std::vector<Ref<ModulusPoly> >> ModulusPoly::divide(Ref<ModulusPoly> other) MB_NOEXCEPT_EXCEPT_BADALLOC {
    if (&field_ != &other->field_) {
      return failure<IllegalArgumentException>("ModulusPolys do not have same ModulusGF field");
    }
    if (other->isZero()) {
      return failure<IllegalArgumentException>("Divide by 0");
    }

    Ref<ModulusPoly> quotient (field_.getZero());
    Ref<ModulusPoly> remainder (*this);

    int denominatorLeadingTerm = other->getCoefficient(other->getDegree());

    auto const tryInverse(field_.inverse(denominatorLeadingTerm));
    if(!tryInverse)
        return tryInverse.error();

    int inverseDenominatorLeadingTerm = *tryInverse;

    while (remainder->getDegree() >= other->getDegree() && !remainder->isZero()) {
      int degreeDifference = remainder->getDegree() - other->getDegree();
      int scale = field_.multiply(remainder->getCoefficient(remainder->getDegree()), inverseDenominatorLeadingTerm);

      auto const tryMult(other->multiplyByMonomial(degreeDifference, scale));
      if(!tryMult)
          return tryMult.error();

      Ref<ModulusPoly> term (*tryMult);

      auto const tryBuildMonomial(field_.buildMonomial(degreeDifference, scale));
      if(!tryBuildMonomial)
          return tryBuildMonomial.error();

      Ref<ModulusPoly> iterationQuotient (*tryBuildMonomial);

      auto const tryAdd(quotient->add(iterationQuotient));
      if(!tryAdd)
          return tryAdd.error();
      quotient = *tryAdd;

      auto const trySub(remainder->subtract(term));
      if(!trySub)
          return trySub.error();
      remainder = *trySub;
    }

    std::vector<Ref<ModulusPoly> > result(2);
    result[0] = quotient;
    result[1] = remainder;
    return result;
  }

#if 0
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(8 * getDegree());
    for (int degree = getDegree(); degree >= 0; degree--) {
      int coefficient = getCoefficient(degree);
      if (coefficient != 0) {
        if (coefficient < 0) {
          result.append(" - ");
          coefficient = -coefficient;
        } else {
          if (result.length() > 0) {
            result.append(" + ");
          }
        }
        if (degree == 0 || coefficient != 1) {
          result.append(coefficient);
        }
        if (degree != 0) {
          if (degree == 1) {
            result.append('x');
          } else {
            result.append("x^");
            result.append(degree);
          }
        }
      }
    }
    return result.toString();
  }
#endif

ModulusPoly::~ModulusPoly()
{

}

} /* namespace pdf417 */
} /* namespace zxing */

