/*
 *  ReedSolomonDecoder.cpp
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

#include "zxing/common/reedsolomon/ReedSolomonDecoder.h"
#include "zxing/common/Array.h"                             // for ArrayRef, Array
#include "zxing/common/Counted.h"                           // for Ref
#include "zxing/common/reedsolomon/GenericGF.h"             // for GenericGF, GenericGF::DATA_MATRIX_FIELD_256
#include "zxing/common/reedsolomon/GenericGFPoly.h"         // for GenericGFPoly
#include "zxing/common/reedsolomon/ReedSolomonException.h"  // for ReedSolomonException

#include <Log.h>                                            // for LOGV
#include <Utils/Macros.h>

using pping::Ref;
using pping::ArrayRef;
using pping::ReedSolomonDecoder;
using pping::GenericGFPoly;

// VC++
using pping::GenericGF;

namespace pping {

ReedSolomonDecoder::ReedSolomonDecoder(Ref<GenericGF> fld) :
    field(fld) {
}

ReedSolomonDecoder::~ReedSolomonDecoder() {
}

Fallible<void> ReedSolomonDecoder::decode(ArrayRef<int> received, int twoS) MB_NOEXCEPT_EXCEPT_BADALLOC {
  if(twoS < 0) return failure<ReedSolomonException>("twoS should be >= 0");

  auto const tryCreatePoly(GenericGFPoly::createGenericGFPoly(*field, received));
  if(!tryCreatePoly)
      return tryCreatePoly.error();

  Ref<GenericGFPoly> poly(*tryCreatePoly);
  ArrayRef<int> syndromeCoefficients(new Array<int> (twoS));

  LOGV("syndromeCoefficients array = %p", syndromeCoefficients.array_);

  bool dataMatrix = (field.object_ == GenericGF::DATA_MATRIX_FIELD_256.object_);
  bool noError = true;
  for (int i = 0; i < twoS; i++) {
    auto const tryGetExp(field->exp(dataMatrix ? i + 1 : i));
    if(!tryGetExp)
        return tryGetExp.error();

    auto const tryEval(poly->evaluateAt(*tryGetExp));
    if(!tryEval)
        return tryEval.error();

    int eval = *tryEval;
    syndromeCoefficients[syndromeCoefficients->size() - 1 - i] = eval;
    if (eval != 0) {
      noError = false;
    }
  }
  if (noError) {
    return success();
  }
  auto const tryCreatePolySyndrome(GenericGFPoly::createGenericGFPoly(*field, syndromeCoefficients));
  if(!tryCreatePolySyndrome)
      return tryCreatePolySyndrome.error();

  Ref<GenericGFPoly> syndrome(*tryCreatePolySyndrome);

  auto const tryBuildMonomial(field->buildMonomial(twoS, 1));
  if(!tryBuildMonomial)
      return tryBuildMonomial.error();

  auto const getSigmaOmega(runEuclideanAlgorithm((*tryBuildMonomial), syndrome, twoS));
  if(!getSigmaOmega)
      return getSigmaOmega.error();

  std::vector<Ref<GenericGFPoly> > sigmaOmega = *getSigmaOmega;
  Ref<GenericGFPoly> sigma = sigmaOmega[0];
  Ref<GenericGFPoly> omega = sigmaOmega[1];

  auto const errorLocations(findErrorLocations(sigma));
  if(!errorLocations)
      return errorLocations.error();

  auto const tryGetErrorMagnitudes(findErrorMagnitudes(omega, *errorLocations));
  if(!tryGetErrorMagnitudes)
      return tryGetErrorMagnitudes.error();

  ArrayRef<int> errorMagitudes = *tryGetErrorMagnitudes;
  for (int i = 0; i < (int) (*errorLocations).size(); i++) {

    auto const getLog(field->log((*errorLocations)[i]));
    if(!getLog)
        return getLog.error();

    int position = (int)received->size() - 1 - (*getLog);

    if(position < 0)
        return failure<ReedSolomonException>("Bad error location");

    received[position] = GenericGF::addOrSubtract(received[position], errorMagitudes[i]);
  }
  return success();
}

Fallible<std::vector<Ref<GenericGFPoly>>> ReedSolomonDecoder::runEuclideanAlgorithm(Ref<GenericGFPoly> a,
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

  Ref<GenericGFPoly> tLast;
  {
      auto const tryGetZero(field->getZero());
      if(!tryGetZero)
          return tryGetZero.error();

      tLast = *tryGetZero;
  }

  auto const tryGetOne(field->getOne());
  if(!tryGetOne)
      return tryGetOne.error();

  Ref<GenericGFPoly> t(*tryGetOne);

  // Run Euclidean algorithm until r's degree is less than R/2
  while (r->getDegree() >= R / 2) {
    Ref<GenericGFPoly> rLastLast(rLast);
    Ref<GenericGFPoly> tLastLast(tLast);
    rLast = r;
    tLast = t;


    // Divide rLastLast by rLast, with quotient q and remainder r
    if (rLast->isZero()) {
      // Oops, Euclidean algorithm already terminated?
      return failure<ReedSolomonException>("r_{i-1} was zero");
    }
    r = rLastLast;

    auto const tryGetZero(field->getZero());
    if(!tryGetZero)
        return tryGetZero.error();

    Ref<GenericGFPoly> q(*tryGetZero);
    int denominatorLeadingTerm = rLast->getCoefficient(rLast->getDegree());

    auto const invDenominatorLeadTerm(field->inverse(denominatorLeadingTerm));
    if(!invDenominatorLeadTerm)
        return invDenominatorLeadTerm.error();

    int dltInverse = *invDenominatorLeadTerm;

    while (r->getDegree() >= rLast->getDegree() && !r->isZero()) {
      int degreeDiff = r->getDegree() - rLast->getDegree();

      auto const tryMultForScale(field->multiply(r->getCoefficient(r->getDegree()), dltInverse));
      if(!tryMultForScale)
          return tryMultForScale.error();

      int scale = *tryMultForScale;

      auto const tryBuildMonomial(field->buildMonomial(degreeDiff, scale));
      if(!tryBuildMonomial)
          return tryBuildMonomial.error();

      auto const tryOp1(q->addOrSubtract(*tryBuildMonomial));
      if(!tryOp1)
          return tryOp1.error();

      q = *tryOp1;

      auto const tryMult(rLast->multiplyByMonomial(degreeDiff, scale));
      if(!tryMult)
          return tryMult.error();

      auto const tryOp2(r->addOrSubtract(*tryMult));
      if(!tryOp2)
          return tryOp2.error();

      r = *tryOp2;
    }
    auto const tryMultByLast(q->multiply(tLast));
    if(!tryMultByLast)
        return tryMultByLast.error();

    auto const tryOp((*tryMultByLast)->addOrSubtract(tLastLast));
    if(!tryOp)
        return tryOp.error();

    t = *tryOp;

    if (r->getDegree() >= rLast->getDegree()) {
        // After updating ZXing 05.05.2015, not to have new Exception Class
        // throw IllegalStateException("Division algorithm failed to reduce polynomial?");
        return failure<ReedSolomonException>("Division algorithm failed to reduce polynomial?");
    }
  }

  int sigmaTildeAtZero = t->getCoefficient(0);

  auto const getInverse(field->inverse(sigmaTildeAtZero));
  if(!getInverse)
      return getInverse.error();

  int inverse = *getInverse;

  auto const tryMultForSigma(t->multiply(inverse));
  if(!tryMultForSigma)
      return tryMultForSigma.error();

  Ref<GenericGFPoly> sigma(*tryMultForSigma);

  auto const tryMultForOmega(r->multiply(inverse));
  if(!tryMultForOmega)
      return tryMultForOmega.error();

  Ref<GenericGFPoly> omega(*tryMultForOmega);

  std::vector<Ref<GenericGFPoly> > result(2);
  result[0] = sigma;
  result[1] = omega;
  return result;
}

Fallible<ArrayRef<int>> ReedSolomonDecoder::findErrorLocations(Ref<GenericGFPoly> errorLocator) {
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
    // cout << "errorLocator(" << i << ") == " << errorLocator->evaluateAt(i) << endl;
    auto const tryEval(errorLocator->evaluateAt(i));
    if(!tryEval)
        return tryEval.error();

    if ((*tryEval) == 0) {

        auto const getInverse(field->inverse(i));
        if(!getInverse)
            return getInverse.error();

        result[e] = *getInverse;
        e++;
    }
  }
  if (e != numErrors) {
      return failure<ReedSolomonException>("Error locator degree does not match number of roots");
  }
  return result;
}

Fallible<ArrayRef<int>> ReedSolomonDecoder::findErrorMagnitudes(Ref<GenericGFPoly> errorEvaluator, ArrayRef<int> errorLocations) {
  // This is directly applying Forney's Formula
  int s = (int)errorLocations.size();
  ArrayRef<int> result(new Array<int>(s));
  for (int i = 0; i < s; i++) {

      auto const getInverse(field->inverse(errorLocations[i]));
      if(!getInverse)
          return getInverse.error();

      int xiInverse = *getInverse;

    int denominator = 1;
    for (int j = 0; j < s; j++) {
      if (i != j) {

        auto const tryMult(field->multiply(errorLocations[j], xiInverse));
        if(!tryMult)
            return tryMult.error();

        int term = *tryMult;
        int termPlus1 = (term & 0x1) == 0 ? term | 1 : term & ~1;

        auto const tryMultDenom(field->multiply(denominator, termPlus1));
        if(!tryMultDenom)
            return tryMultDenom.error();

        denominator = *tryMultDenom;
      }
    }
    auto const getInverseDenominator(field->inverse(denominator));
    if(!getInverseDenominator)
        return getInverseDenominator.error();

    auto const tryEval(errorEvaluator->evaluateAt(xiInverse));
    if(!tryEval)
        return tryEval.error();

    auto const tryMultEval(field->multiply(*tryEval, *getInverseDenominator));
    if(!tryMultEval)
        return tryMultEval.error();

    result[i] = *tryMultEval;

    if (field->getGeneratorBase() != 0) {

        auto const tryMultForResult(field->multiply(result[i], xiInverse));
        if(!tryMultForResult)
            return tryMultForResult.error();

        result[i] = *tryMultForResult;
    }
  }
  return result;
}
}
