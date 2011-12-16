/*
 *  GF256Poly.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 05/05/2008.
 *  Copyright 2008 Google UK. All rights reserved.
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
#include <sstream>
#include <zxing/common/reedsolomon/GF256Poly.h>
#include <zxing/common/reedsolomon/GF256.h>
#include <zxing/common/IllegalArgumentException.h>

namespace zxing {
using namespace std;

void GF256Poly::fixCoefficients() {
  int coefficientsLength = coefficients.size();
  if (coefficientsLength > 1 && coefficients[0] == 0) {
    // Leading term must be non-zero for anything except
    // the constant polynomial "0"
    int firstNonZero = 1;
    while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
      firstNonZero++;
    }
    if (firstNonZero == coefficientsLength) {
      coefficientsLength = field.getZero()->coefficients.size();
      coefficients.reset(new Array<int> (coefficientsLength));
      *coefficients = *(field.getZero()->coefficients);
    } else {
      ArrayRef<int> c(coefficients);
      coefficientsLength -= firstNonZero;
      coefficients.reset(new Array<int> (coefficientsLength));
      for (int i = 0; i < coefficientsLength; i++) {
        coefficients[i] = c[i + firstNonZero];
      }
    }
  }
}

GF256Poly::GF256Poly(GF256 &f, ArrayRef<int> c) :
    Counted(), field(f), coefficients(c) {
  fixCoefficients();
}

GF256Poly::~GF256Poly() {

}

int GF256Poly::getDegree() {
  return coefficients.size() - 1;
}

bool GF256Poly::isZero() {
  return coefficients[0] == 0;
}

int GF256Poly::getCoefficient(int degree) {
  return coefficients[coefficients.size() - 1 - degree];
}

int GF256Poly::evaluateAt(int a) {
  if (a == 0) {
    return getCoefficient(0);
  }
  int size = coefficients.size();
  if (a == 1) {
    // Just the sum of the coefficients
    int result = 0;
    for (int i = 0; i < size; i++) {
      result = GF256::addOrSubtract(result, coefficients[i]);
    }
    return result;
  }
  int result = coefficients[0];
  for (int i = 1; i < size; i++) {
    result = GF256::addOrSubtract(field.multiply(a, result), coefficients[i]);
  }
  return result;
}

Ref<GF256Poly> GF256Poly::addOrSubtract(Ref<GF256Poly> b) {
  if (&field != &b->field) {
    throw IllegalArgumentException("Fields must be the same");
  }
  if (isZero()) {
    return b;
  }
  if (b->isZero()) {
    return Ref<GF256Poly>(this);
  }

  ArrayRef<int> largerCoefficients = coefficients;
  ArrayRef<int> smallerCoefficients = b->coefficients;
  if (smallerCoefficients.size() > largerCoefficients.size()) {
    ArrayRef<int> tmp(smallerCoefficients);
    smallerCoefficients = largerCoefficients;
    largerCoefficients = tmp;
  }

  ArrayRef<int> sumDiff(new Array<int> (largerCoefficients.size()));

  unsigned lengthDiff = largerCoefficients.size() - smallerCoefficients.size();
  for (unsigned i = 0; i < lengthDiff; i++) {
    sumDiff[i] = largerCoefficients[i];
  }
  for (unsigned i = lengthDiff; i < largerCoefficients.size(); i++) {
    sumDiff[i] = GF256::addOrSubtract(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
  }
  return Ref<GF256Poly>(new GF256Poly(field, sumDiff));
}

Ref<GF256Poly> GF256Poly::multiply(Ref<GF256Poly> b) {
  if (&field != &b->field) {
    throw IllegalArgumentException("Fields must be the same");
  }
  if (isZero() || b->isZero()) {
    return field.getZero();
  }
  ArrayRef<int> aCoefficients = coefficients;
  int aLength = aCoefficients.size();
  ArrayRef<int> bCoefficients = b->coefficients;
  int bLength = bCoefficients.size();
  int productLength = aLength + bLength - 1;
  ArrayRef<int> product(new Array<int> (productLength));
  for (int i = 0; i < aLength; i++) {
    int aCoeff = aCoefficients[i];
    for (int j = 0; j < bLength; j++) {
      product[i + j] = GF256::addOrSubtract(product[i + j], field.multiply(aCoeff, bCoefficients[j]));
    }
  }

  return Ref<GF256Poly>(new GF256Poly(field, product));
}

Ref<GF256Poly> GF256Poly::multiply(int scalar) {
  if (scalar == 0) {
    return field.getZero();
  }
  if (scalar == 1) {
    return Ref<GF256Poly>(this);
  }
  int size = coefficients.size();
  ArrayRef<int> product(new Array<int> (size));
  for (int i = 0; i < size; i++) {
    product[i] = field.multiply(coefficients[i], scalar);
  }
  return Ref<GF256Poly>(new GF256Poly(field, product));
}

Ref<GF256Poly> GF256Poly::multiplyByMonomial(int degree, int coefficient) {
  if (degree < 0) {
    throw IllegalArgumentException("Degree must be non-negative");
  }
  if (coefficient == 0) {
    return field.getZero();
  }
  int size = coefficients.size();
  ArrayRef<int> product(new Array<int> (size + degree));
  for (int i = 0; i < size; i++) {
    product[i] = field.multiply(coefficients[i], coefficient);
  }
  return Ref<GF256Poly>(new GF256Poly(field, product));
}

const char *GF256Poly::description() const {
  ostringstream result;
  result << *this;
  return result.str().c_str();
}

ostream& operator<<(ostream& out, const GF256Poly& p) {
  GF256Poly &poly = const_cast<GF256Poly&>(p);
  out << "Poly[" << poly.coefficients.size() << "]";
  if (poly.coefficients.size() > 0) {
    out << "(" << poly.coefficients[0];
    for (unsigned i = 1; i < poly.coefficients.size(); i++) {
      out << "," << poly.coefficients[i];
    }
    out << ")";
  }
  return out;
}

}
