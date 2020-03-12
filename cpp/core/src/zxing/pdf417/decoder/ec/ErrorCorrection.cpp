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

#include <stddef.h>                                         // for size_t
#include <zxing/pdf417/decoder/ec/ErrorCorrection.h>

#include "zxing/common/Array.h"                             // for ArrayRef, Array
#include "zxing/common/Counted.h"                           // for Ref
#include "zxing/common/reedsolomon/ReedSolomonException.h"  // for ReedSolomonException
#include "zxing/pdf417/decoder/ec/ModulusGF.h"              // for ModulusGF, ModulusGF::PDF417_GF
#include "zxing/pdf417/decoder/ec/ModulusPoly.h"            // for ModulusPoly

#if (defined (DEBUG) && defined _WIN32)
#include "Log.h"
#include "Utils/disable_warnings.hpp"
#include <windows.h>
MB_DISABLE_WARNING_MSVC( 4995 )
#endif

namespace pping {
namespace pdf417 {

/**
 * <p>PDF417 error correction implementation.</p>
 *
 * <p>This <a href="http://en.wikipedia.org/wiki/Reed%E2%80%93Solomon_error_correction#Example">example</a>
 * is quite useful in understanding the algorithm.</p>
 *
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.ReedSolomonDecoder
 */
 

ErrorCorrection::ErrorCorrection()
    : field_(ModulusGF::PDF417_GF)
{
}

Fallible<void> ErrorCorrection::decode(ArrayRef<int> received,
                     int numECCodewords,
                     ArrayRef<int> erasures) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    if(numECCodewords < 0)
        return failure<ReedSolomonException>("numECCodewords < 0");

    auto const tryCreatePoly(ModulusPoly::createModulusPoly(field_, received));
    if(!tryCreatePoly)
        return tryCreatePoly.error();

   Ref<ModulusPoly> poly (*tryCreatePoly);
    ArrayRef<int> S( new Array<int>(numECCodewords));
    bool error = false;
    for (int i = numECCodewords; i > 0; i--) {
      int eval = poly->evaluateAt(field_.exp(i));
      S[numECCodewords - i] = eval;
      if (eval != 0) {
        error = true;
      }
    }

    if (error) {

      Ref<ModulusPoly> knownErrors = field_.getOne();
      for (size_t i=0;i<erasures.size();i++) {
        int b = field_.exp((int)received.size() - 1 - erasures[i]);
        // Add (1 - bx) term:
        ArrayRef<int> one_minus_b_x(new Array<int>(2));
        one_minus_b_x[1]=field_.subtract(0,b);
        one_minus_b_x[0]=1;

        auto const tryCreateModPoly(ModulusPoly::createModulusPoly(field_,one_minus_b_x));
        if(!tryCreateModPoly)
            return tryCreateModPoly.error();

        Ref<ModulusPoly> term (*tryCreateModPoly);

        auto const tryMult(knownErrors->multiply(term));
        if(!tryMult)
            return tryMult.error();
        knownErrors = *tryMult;
      }
      auto const tryCreatePolySyndrome(ModulusPoly::createModulusPoly(field_, S));
      if(!tryCreatePolySyndrome)
          return tryCreatePolySyndrome.error();

      Ref<ModulusPoly> syndrome (*tryCreatePolySyndrome);
      //syndrome = syndrome.multiply(knownErrors);

      auto const tryBuildMonomial(field_.buildMonomial(numECCodewords, 1));
      if(!tryBuildMonomial)
          return tryBuildMonomial.error();

      auto const tryGetSigmaOmega(runEuclideanAlgorithm(*tryBuildMonomial, syndrome, numECCodewords));
      if(!tryGetSigmaOmega)
          return tryGetSigmaOmega.error();

      auto const sigmaOmega = *tryGetSigmaOmega;

      Ref<ModulusPoly> sigma = sigmaOmega[0];
      Ref<ModulusPoly> omega = sigmaOmega[1];

      //sigma = sigma.multiply(knownErrors);

      auto const tryGetLocations(findErrorLocations(sigma));
      if(!tryGetLocations)
          return tryGetLocations.error();

      ArrayRef<int> errorLocations = *tryGetLocations;

      auto const tryGetMagnitudes(findErrorMagnitudes(omega, sigma, errorLocations));
      if(!tryGetMagnitudes)
          return tryGetMagnitudes.error();

      ArrayRef<int> errorMagnitudes = *tryGetMagnitudes;

      for (size_t i = 0; i < errorLocations.size(); i++) {
          auto const tryLog(field_.log(errorLocations[i]));
          if(!tryLog)
              return tryLog.error();

        int position = (int)received.size() - 1 - (*tryLog);
        if (position < 0) {
          return failure<ReedSolomonException>("Bad error location!");
        }
        received[position] = field_.subtract(received[position], errorMagnitudes[i]);
#if (defined (DEBUG)  && defined _WIN32)
        {
            LOGD("ErrorCorrection::decode: fix @ %d, new value = %d\n", position, received[position]);
        }
#endif
      }
    }
    return success();
}

Fallible<std::vector<Ref<ModulusPoly>>> ErrorCorrection::runEuclideanAlgorithm(Ref<ModulusPoly> a, Ref<ModulusPoly> b, int R) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    // Assume a's degree is >= b's
    if (a->getDegree() < b->getDegree()) {
      Ref<ModulusPoly> temp = a;
      a = b;
      b = temp;
    }

    Ref<ModulusPoly> rLast ( a);
    Ref<ModulusPoly> r ( b);
    Ref<ModulusPoly> tLast ( field_.getZero());
    Ref<ModulusPoly> t ( field_.getOne());

    // Run Euclidean algorithm until r's degree is less than R/2
    while (r->getDegree() >= R / 2) {
      Ref<ModulusPoly> rLastLast (rLast);
      Ref<ModulusPoly> tLastLast (tLast);
      rLast = r;
      tLast = t;

      // Divide rLastLast by rLast, with quotient in q and remainder in r
      if (rLast->isZero()) {
        // Oops, Euclidean algorithm already terminated?
        return failure<ReedSolomonException>("Euclidean algorithm already terminated?");
      }
      r = rLastLast;
      Ref<ModulusPoly> q (field_.getZero());
      int denominatorLeadingTerm = rLast->getCoefficient(rLast->getDegree());

      auto const tryInverse(field_.inverse(denominatorLeadingTerm));
      if(!tryInverse)
          return tryInverse.error();

      int dltInverse = *tryInverse;
      while (r->getDegree() >= rLast->getDegree() && !r->isZero()) {
        int degreeDiff = r->getDegree() - rLast->getDegree();
        int scale = field_.multiply(r->getCoefficient(r->getDegree()), dltInverse);

        auto const tryBuildMonomial(field_.buildMonomial(degreeDiff, scale));
        if(!tryBuildMonomial)
            return tryBuildMonomial.error();

        auto const tryAdd(q->add(*tryBuildMonomial));
        if(!tryAdd)
            return tryAdd.error();
        q = *tryAdd;

        auto const tryMult(rLast->multiplyByMonomial(degreeDiff, scale));
        if(!tryMult)
            return tryMult.error();

        auto const trySub(r->subtract(*tryMult));
        if(!trySub)
            return trySub.error();

        r = *trySub;
      }
      auto const mult = q->multiply(tLast);
      if(mult) {
          auto const sub((*mult)->subtract(tLastLast));
          if(sub) {
              auto const neg((*sub)->negative());
              if(neg)
                  t = *neg;
              else
                  return neg.error();
          } else {
              return sub.error();
          }
      } else {
          mult.error();
      }


    }

    int sigmaTildeAtZero = t->getCoefficient(0);
    if (sigmaTildeAtZero == 0) {
      return failure<ReedSolomonException>("sigmaTilde = 0!");
    }
    auto const tryInverse(field_.inverse(sigmaTildeAtZero));
    if(!tryInverse)
        return tryInverse.error();

    int inverse = *tryInverse;

    auto const tryMult1(t->multiply(inverse));
    if(!tryMult1)
        return tryMult1.error();
    Ref<ModulusPoly> sigma (*tryMult1);

    auto const tryMult2(r->multiply(inverse));
    if(!tryMult2)
        return tryMult2.error();

    Ref<ModulusPoly> omega (*tryMult2);
    std::vector<Ref<ModulusPoly> > v(2);
    v[0] = sigma;
    v[1] = omega;
    return v;
}

Fallible<ArrayRef<int>> ErrorCorrection::findErrorLocations(Ref<ModulusPoly> errorLocator) MB_NOEXCEPT_EXCEPT_BADALLOC {
    // This is a direct application of Chien's search
    int numErrors = errorLocator->getDegree();
    ArrayRef<int> result( new Array<int>(numErrors));
    int e = 0;
    for (int i = 1; i < field_.getSize() && e < numErrors; i++) {
      if (errorLocator->evaluateAt(i) == 0) {
          auto const tryInverse(field_.inverse(i));
          if(!tryInverse)
              return tryInverse.error();

        result[e] = *tryInverse;
        e++;
      }
    }
    if (e != numErrors) {
#if (defined (DEBUG) && defined _WIN32)
      char sz[128];
      sprintf(sz,"Error number inconsistency, %d/%d!",e,numErrors);
      return failure<ReedSolomonException>(sz);
#else
      return failure<ReedSolomonException>("Error number inconsistency!");
#endif
    }
#if (defined (DEBUG) && defined _WIN32)
        {
            LOGD("ErrorCorrection::findErrorLocations: found %d errors.\n", e);
        }
#endif
    return result;
}

Fallible<ArrayRef<int>> ErrorCorrection::findErrorMagnitudes(Ref<ModulusPoly> errorEvaluator,
                                    Ref<ModulusPoly> errorLocator,
                                    ArrayRef<int> errorLocations) MB_NOEXCEPT_EXCEPT_BADALLOC {
    int i;
    int errorLocatorDegree = errorLocator->getDegree();
    ArrayRef<int> formalDerivativeCoefficients (new Array<int>(errorLocatorDegree));
    for (i = 1; i <= errorLocatorDegree; i++) {
      formalDerivativeCoefficients[errorLocatorDegree - i] =
          field_.multiply(i, errorLocator->getCoefficient(i));
    }
    auto const tryCreatePoly(ModulusPoly::createModulusPoly(field_, formalDerivativeCoefficients));
    if(!tryCreatePoly)
        return tryCreatePoly.error();
    Ref<ModulusPoly> formalDerivative (*tryCreatePoly);

    // This is directly applying Forney's Formula
    int s = (int)errorLocations->size();
    ArrayRef<int> result ( new Array<int>(s));
    for (i = 0; i < s; i++) {
      auto const tryInverse(field_.inverse(errorLocations[i]));
      if(!tryInverse)
          return tryInverse.error();

      int xiInverse = *tryInverse;
      int numerator = field_.subtract(0, errorEvaluator->evaluateAt(xiInverse));

      auto const tryInverseInverse(field_.inverse(formalDerivative->evaluateAt(xiInverse)));
      if(!tryInverseInverse)
          return tryInverseInverse.error();

      int denominator = *tryInverseInverse;
      result[i] = field_.multiply(numerator, denominator);
    }
    return result;
  }

} /* namespace pdf417 */
} /* namespace zxing */
