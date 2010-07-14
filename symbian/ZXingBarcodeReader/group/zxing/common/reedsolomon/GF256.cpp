/*
 *  GF256.cpp
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

#include <vector>
#include <iostream>
#include <zxing/common/reedsolomon/GF256.h>
#include <zxing/common/reedsolomon/GF256Poly.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/common/Array.h>
#include <zxing/common/Counted.h>

namespace zxing {
using namespace std;

static inline ArrayRef<int> makeArray(int value) {
  ArrayRef<int> valuesRef(new Array<int> (value, 1));
  return valuesRef;
}

static inline Ref<GF256Poly> refPoly(GF256 &field, int value) {
  ArrayRef<int> values(makeArray(value));
  Ref<GF256Poly> result(new GF256Poly(field, values));
  return result;
}

GF256::GF256(int primitive) :
    exp_(256, (const int)0), log_(256, (const int)0), zero_(refPoly(*this, 0)), one_(refPoly(*this, 1)) {
  int x = 1;
  for (int i = 0; i < 256; i++) {
    exp_[i] = x;
    x <<= 1;
    if (x >= 0x100) {
      x ^= primitive;
    }
  }

  // log(0) == 0, but should never be used
  log_[0] = 0;
  for (int i = 0; i < 255; i++) {
    log_[exp_[i]] = i;
  }
}

Ref<GF256Poly> GF256::getZero() {
  return zero_;
}

Ref<GF256Poly> GF256::getOne() {
  return one_;
}

Ref<GF256Poly> GF256::buildMonomial(int degree, int coefficient) {
#ifdef DEBUG
  cout << __FUNCTION__ << "\n";
#endif
  if (degree < 0) {
    throw IllegalArgumentException("Degree must be non-negative");
  }
  if (coefficient == 0) {
    return zero_;
  }
  int nCoefficients = degree + 1;
  ArrayRef<int> coefficients(new Array<int> (nCoefficients));
  coefficients[0] = coefficient;
  Ref<GF256Poly> result(new GF256Poly(*this, coefficients));
  return result;
}

int GF256::addOrSubtract(int a, int b) {
  return a ^ b;
}

int GF256::exp(int a) {
  return exp_[a];
}

int GF256::log(int a) {
  if (a == 0) {
    throw IllegalArgumentException("Cannot take the logarithm of 0");
  }
  return log_[a];
}

int GF256::inverse(int a) {
  if (a == 0) {
    throw IllegalArgumentException("Cannot calculate the inverse of 0");
  }
  return exp_[255 - log_[a]];
}

int GF256::multiply(int a, int b) {
  if (a == 0 || b == 0) {
    return 0;
  }
  int logSum = log_[a] + log_[b];
  // index is a sped-up alternative to logSum % 255 since sum
  // is in [0,510]. Thanks to jmsachs for the idea
  return exp_[(logSum & 0xFF) + (logSum >> 8)];
}

GF256 GF256::QR_CODE_FIELD(0x011D); // x^8 + x^4 + x^3 + x^2 + 1
GF256 GF256::DATA_MATRIX_FIELD(0x012D); // x^8 + x^5 + x^3 + x^2 + 1

ostream& operator<<(ostream& out, const GF256& field) {
  out << "Field[\nexp=(";
  out << field.exp_[0];
  for (int i = 1; i < 256; i++) {
    out << "," << field.exp_[i];
  }
  out << "),\nlog=(";
  out << field.log_[0];
  for (int i = 1; i < 256; i++) {
    out << "," << field.log_[i];
  }
  out << ")\n]";
  return out;
}

}
