/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.common.reedsolomon
{
/**
 * <p>Implements Reed-Solomon decoding, as the name implies.</p>
 *
 * <p>The algorithm will not be explained here, but the following references were helpful
 * in creating this implementation:</p>
 *
 * <ul>
 * <li>Bruce Maggs.
 * <a href="http://www.cs.cmu.edu/afs/cs.cmu.edu/project/pscico-guyb/realworld/www/rs_decode.ps">
 * "Decoding Reed-Solomon Codes"</a> (see discussion of Forney's Formula)</li>
 * <li>J.I. Hall. <a href="www.mth.msu.edu/~jhall/classes/codenotes/GRS.pdf">
 * "Chapter 5. Generalized Reed-Solomon Codes"</a>
 * (see discussion of Euclidean algorithm)</li>
 * </ul>
 *
 * <p>Much credit is due to William Rucklidge since portions of this code are an indirect
 * port of his C++ Reed-Solomon implementation.</p>
 *
 * @author Sean Owen
 * @author William Rucklidge
 * @author sanfordsquires
 */
    public class ReedSolomonDecoder
    {
          private var field:GF256;

          public function ReedSolomonDecoder(field:GF256) {
            this.field = field;
          }

          /**
           * <p>Decodes given set of received codewords, which include both data and error-correction
           * codewords. Really, this means it uses Reed-Solomon to detect and correct errors, in-place,
           * in the input.</p>
           *
           * @param received data and error-correction codewords
           * @param twoS number of error-correction codewords available
           * @throws ReedSolomonException if decoding fails for any reason
           */
          public function decode(received:Array, twoS:int):void {
                var poly:GF256Poly = new GF256Poly(field, received);
                var syndromeCoefficients:Array = new Array(twoS);
                var dataMatrix:Boolean = (field == GF256.DATA_MATRIX_FIELD);
                var noError:Boolean = true;
                for (var i:int = 0; i < twoS; i++) {
                  // Thanks to sanfordsquires for this fix:
                  var eval:int = poly.evaluateAt(field.exp(dataMatrix ? i + 1 : i));
                  syndromeCoefficients[syndromeCoefficients.length - 1 - i] = eval;
                  if (eval != 0) {
                    noError = false;
                  }
                }
                if (noError) {
                  return;
                }
                var syndrome:GF256Poly = new GF256Poly(field, syndromeCoefficients);
                var sigmaOmega:Array = runEuclideanAlgorithm(field.buildMonomial(twoS, 1), syndrome, twoS);
                var sigma:GF256Poly = sigmaOmega[0];
                var omega:GF256Poly = sigmaOmega[1];
                var errorLocations:Array = findErrorLocations(sigma);
                var errorMagnitudes:Array = findErrorMagnitudes(omega, errorLocations, dataMatrix);
                for (var j:int = 0; j < errorLocations.length; j++) {
                  var position:int = received.length - 1 - field.log(errorLocations[j]);
                  if (position < 0) {
                    throw new ReedSolomonException("Bad error location");
                  }
                  received[position] = GF256.addOrSubtract(received[position], errorMagnitudes[j]);
                }
          }

          private function runEuclideanAlgorithm(a:GF256Poly, b:GF256Poly, R:int ):Array
          {
            // Assume a's degree is >= b's
            if (a.getDegree() < b.getDegree()) {
              var temp:GF256Poly  = a;
              a = b;
              b = temp;
            }

            var rLast:GF256Poly = a;
            var r:GF256Poly = b;
            var sLast:GF256Poly = field.getOne();
            var s:GF256Poly = field.getZero();
            var tLast:GF256Poly = field.getZero();
            var t:GF256Poly = field.getOne();

            // Run Euclidean algorithm until r's degree is less than R/2
            while (r.getDegree() >= R / 2) {
              var rLastLast:GF256Poly  = rLast;
              var sLastLast:GF256Poly  = sLast;
              var tLastLast:GF256Poly  = tLast;
              rLast = r;
              sLast = s;
              tLast = t;

              // Divide rLastLast by rLast, with quotient in q and remainder in r
              if (rLast.isZero()) {
                // Oops, Euclidean algorithm already terminated?
                throw new ReedSolomonException("r_{i-1} was zero");
              }
              r = rLastLast;
              var q:GF256Poly = field.getZero();
              var denominatorLeadingTerm:int = rLast.getCoefficient(rLast.getDegree());
              var dltInverse:int = field.inverse(denominatorLeadingTerm);
              while (r.getDegree() >= rLast.getDegree() && !r.isZero()) {
                var degreeDiff:int = r.getDegree() - rLast.getDegree();
                var scale:int = field.multiply(r.getCoefficient(r.getDegree()), dltInverse);
                q = q.addOrSubtract(field.buildMonomial(degreeDiff, scale));
                r = r.addOrSubtract(rLast.multiplyByMonomial(degreeDiff, scale));
              }

              s = q.multiply(sLast).addOrSubtract(sLastLast);
              t = q.multiply(tLast).addOrSubtract(tLastLast);
            }

            var sigmaTildeAtZero:int = t.getCoefficient(0);
            if (sigmaTildeAtZero == 0) {
              throw new ReedSolomonException("sigmaTilde(0) was zero");
            }

            var inverse:int = field.inverse(sigmaTildeAtZero);
            var sigma:GF256Poly = t.multiply(inverse);
            var omega:GF256Poly = r.multiply(inverse);
            return [sigma, omega];
          }

          private function findErrorLocations(errorLocator:GF256Poly):Array{
            // This is a direct application of Chien's search
            var numErrors:int = errorLocator.getDegree();
            if (numErrors == 1) { // shortcut
              return [errorLocator.getCoefficient(1)];
            }
            var result:Array = new Array(numErrors);
            var e:int = 0;
            for (var i:int = 1; i < 256 && e < numErrors; i++) {
              if (errorLocator.evaluateAt(i) == 0) {
                result[e] = field.inverse(i);
                e++;
              }
            }
            if (e != numErrors) {
              throw new ReedSolomonException("Error locator degree does not match number of roots");
            }
            return result;
          }

          private function findErrorMagnitudes(errorEvaluator:GF256Poly , errorLocations:Array, dataMatrix:Boolean):Array {
            // This is directly applying Forney's Formula
            var s:int = errorLocations.length;
            var result:Array = new Array(s);
            for (var i:int = 0; i < s; i++) {
              var xiInverse:int = field.inverse(errorLocations[i]);
              var denominator:int = 1;
              for (var j:int = 0; j < s; j++) {
                if (i != j) {
                  denominator = field.multiply(denominator,
                      GF256.addOrSubtract(1, field.multiply(errorLocations[j], xiInverse)));
                }
              }
              result[i] = field.multiply(errorEvaluator.evaluateAt(xiInverse),
                  field.inverse(denominator));
              // Thanks to sanfordsquires for this fix:
              if (dataMatrix) {
                result[i] = field.multiply(result[i], xiInverse);
              }
            }
            return result;
          }
    
    }

}