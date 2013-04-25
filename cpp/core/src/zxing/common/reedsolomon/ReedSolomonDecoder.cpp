// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
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

#include <memory>
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>
#include <zxing/common/reedsolomon/ReedSolomonException.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/IllegalStateException.h>

using std::vector;
using zxing::Ref;
using zxing::ArrayRef;
using zxing::ReedSolomonDecoder;
using zxing::GenericGFPoly;
using zxing::IllegalStateException;

// VC++
using zxing::GenericGF;

ReedSolomonDecoder::ReedSolomonDecoder(Ref<GenericGF> field_) : field(field_) {}

ReedSolomonDecoder::~ReedSolomonDecoder() {
}

void ReedSolomonDecoder::decode(ArrayRef<int> received, int twoS) {
  Ref<GenericGFPoly> poly(new GenericGFPoly(field, received));
  ArrayRef<int> syndromeCoefficients(twoS);
  bool noError = true;
  for (int i = 0; i < twoS; i++) {
    int eval = poly->evaluateAt(field->exp(i + field->getGeneratorBase()));
    syndromeCoefficients[syndromeCoefficients->size() - 1 - i] = eval;
    if (eval != 0) {
      noError = false;
    }
  }
  if (noError) {
    return;
  }
  Ref<GenericGFPoly> syndrome(new GenericGFPoly(field, syndromeCoefficients));
  vector<Ref<GenericGFPoly> > sigmaOmega =
    runEuclideanAlgorithm(field->buildMonomial(twoS, 1), syndrome, twoS);
  Ref<GenericGFPoly> sigma = sigmaOmega[0];
  Ref<GenericGFPoly> omega = sigmaOmega[1];
  ArrayRef<int> errorLocations = findErrorLocations(sigma);
  ArrayRef<int> errorMagitudes = findErrorMagnitudes(omega, errorLocations);
  for (int i = 0; i < errorLocations->size(); i++) {
    int position = received->size() - 1 - field->log(errorLocations[i]);
    if (position < 0) {
      throw ReedSolomonException("Bad error location");
    }
    received[position] = GenericGF::addOrSubtract(received[position], errorMagitudes[i]);
  }
}

vector<Ref<GenericGFPoly> > ReedSolomonDecoder::runEuclideanAlgorithm(Ref<GenericGFPoly> a,
                                                                      Ref<GenericGFPoly> b,
                                                                      int R) {
  // Assume a's degree is >= b's
  if (a->getDegree() < b->getDegree()) {
    Ref<GenericGFPoly> tmp = a;
    a = b;
    b = tmp;
  }

  Ref<GenericGFPoly> rLast(a);
  Ref<GenericGFPoly> r(b);
  Ref<GenericGFPoly> tLast(field->getZero());
  Ref<GenericGFPoly> t(field->getOne());

  // Run Euclidean algorithm until r's degree is less than R/2
  while (r->getDegree() >= R / 2) {
    Ref<GenericGFPoly> rLastLast(rLast);
    Ref<GenericGFPoly> tLastLast(tLast);
    rLast = r;
    tLast = t;

    // Divide rLastLast by rLast, with quotient q and remainder r
    if (rLast->isZero()) {
      // Oops, Euclidean algorithm already terminated?
      throw ReedSolomonException("r_{i-1} was zero");
    }
    r = rLastLast;
    Ref<GenericGFPoly> q = field->getZero();
    int denominatorLeadingTerm = rLast->getCoefficient(rLast->getDegree());
    int dltInverse = field->inverse(denominatorLeadingTerm);
    while (r->getDegree() >= rLast->getDegree() && !r->isZero()) {
      int degreeDiff = r->getDegree() - rLast->getDegree();
      int scale = field->multiply(r->getCoefficient(r->getDegree()), dltInverse);
      q = q->addOrSubtract(field->buildMonomial(degreeDiff, scale));
      r = r->addOrSubtract(rLast->multiplyByMonomial(degreeDiff, scale));
    }

    t = q->multiply(tLast)->addOrSubtract(tLastLast);

    if (r->getDegree() >= rLast->getDegree()) {
      throw IllegalStateException("Division algorithm failed to reduce polynomial?");
    }
  }

  int sigmaTildeAtZero = t->getCoefficient(0);
  if (sigmaTildeAtZero == 0) {
    throw ReedSolomonException("sigmaTilde(0) was zero");
  }

  int inverse = field->inverse(sigmaTildeAtZero);
  Ref<GenericGFPoly> sigma(t->multiply(inverse));
  Ref<GenericGFPoly> omega(r->multiply(inverse));
  vector<Ref<GenericGFPoly> > result(2);
  result[0] = sigma;
  result[1] = omega;
  return result;
}

ArrayRef<int> ReedSolomonDecoder::findErrorLocations(Ref<GenericGFPoly> errorLocator) {
  // This is a direct application of Chien's search
  int numErrors = errorLocator->getDegree();
  if (numErrors == 1) { // shortcut
    ArrayRef<int> result(new Array<int>(1));
    result[0] = errorLocator->getCoefficient(1);
    return result;
  }
  ArrayRef<int> result(new Array<int>(numErrors));
  int e = 0;
  for (int i = 1; i < field->getSize() && e < numErrors; i++) {
    if (errorLocator->evaluateAt(i) == 0) {
      result[e] = field->inverse(i);
      e++;
    }
  }
  if (e != numErrors) {
    throw ReedSolomonException("Error locator degree does not match number of roots");
  }
  return result;
}

ArrayRef<int> ReedSolomonDecoder::findErrorMagnitudes(Ref<GenericGFPoly> errorEvaluator, ArrayRef<int> errorLocations) {
  // This is directly applying Forney's Formula
  int s = errorLocations->size();
  ArrayRef<int> result(new Array<int>(s));
  for (int i = 0; i < s; i++) {
    int xiInverse = field->inverse(errorLocations[i]);
    int denominator = 1;
    for (int j = 0; j < s; j++) {
      if (i != j) {
        int term = field->multiply(errorLocations[j], xiInverse);
        int termPlus1 = (term & 0x1) == 0 ? term | 1 : term & ~1;
        denominator = field->multiply(denominator, termPlus1);
      }
    }
    result[i] = field->multiply(errorEvaluator->evaluateAt(xiInverse),
                                field->inverse(denominator));
    if (field->getGeneratorBase() != 0) {
      result[i] = field->multiply(result[i], xiInverse);
    }
  }
  return result;
}
