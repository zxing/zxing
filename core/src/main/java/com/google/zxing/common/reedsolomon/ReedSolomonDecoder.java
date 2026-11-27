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

package com.google.zxing.common.reedsolomon;

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
public final class ReedSolomonDecoder {

  private final GenericGF field;

  public ReedSolomonDecoder(GenericGF field) {
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
  public void decode(int[] received, int twoS) throws ReedSolomonException {
    decodeWithECCount(received, twoS);
  }

  /**
   * <p>Decodes given set of received codewords, which include both data and error-correction
   * codewords. Really, this means it uses Reed-Solomon to detect and correct errors, in-place,
   * in the input.</p>
   *
   * @param received data and error-correction codewords
   * @param twoS number of error-correction codewords available
   * @return the number of errors corrected
   * @throws ReedSolomonException if decoding fails for any reason
   */
  public int decodeWithECCount(int[] received, int twoS) throws ReedSolomonException {
    if (received.length >= field.getSize()) {
      throw new ReedSolomonException("Received data length exceeds field maximum");
    }
    if (twoS < 0 || twoS > received.length) {
      throw new ReedSolomonException("Invalid number of error correction codewords");
    }
    for (int codeword : received) {
      if (codeword < 0 || codeword >= field.getSize()) {
        throw new ReedSolomonException("Invalid codeword value");
      }
    }
    GenericGFPoly poly = new GenericGFPoly(field, received);
    int[] syndromeCoefficients = new int[twoS];
    boolean noError = true;
    int generatorBase = field.getGeneratorBase() % (field.getSize() - 1);
    for (int i = 0; i < twoS; i++) {
      int eval = poly.evaluateAt(field.exp(i + generatorBase));
      syndromeCoefficients[syndromeCoefficients.length - 1 - i] = eval;
      if (eval != 0) {
        noError = false;
      }
    }
    if (noError) {
      return 0;
    }
    GenericGFPoly syndrome = new GenericGFPoly(field, syndromeCoefficients);
    GenericGFPoly[] sigmaOmega =
        runEuclideanAlgorithm(field.buildMonomial(twoS, 1), syndrome, twoS);
    GenericGFPoly sigma = sigmaOmega[0];
    GenericGFPoly omega = sigmaOmega[1];
    int[] errorLocations = findErrorLocations(sigma);
    int[] errorPositions = new int[errorLocations.length];
    for (int i = 0; i < errorLocations.length; i++) {
      int position = received.length - 1 - field.log(errorLocations[i]);
      if (position < 0) {
        throw new ReedSolomonException("Bad error location");
      }
      errorPositions[i] = position;
    }
    int[] errorMagnitudes = findErrorMagnitudes(omega, errorLocations);
    for (int i = 0; i < errorPositions.length; i++) {
      received[errorPositions[i]] =
          GenericGF.addOrSubtract(received[errorPositions[i]], errorMagnitudes[i]);
    }
    return errorLocations.length;
  }

  private GenericGFPoly[] runEuclideanAlgorithm(GenericGFPoly a, GenericGFPoly b, int R)
      throws ReedSolomonException {
    // a is x^R (degree R) and b is the syndrome polynomial (degree < R)
    if (a.getDegree() < b.getDegree()) {
      throw new IllegalArgumentException();
    }

    GenericGFPoly rLast = a;
    GenericGFPoly r = b;
    GenericGFPoly tLast = field.getZero();
    GenericGFPoly t = field.getOne();

    // Run Euclidean algorithm until r's degree is less than R/2
    while (!r.isZero() && 2 * r.getDegree() >= R) {
      GenericGFPoly rLastLast = rLast;
      GenericGFPoly tLastLast = tLast;
      rLast = r;
      tLast = t;

      // Divide rLastLast by rLast, with quotient in q and remainder in r
      if (rLast.isZero()) {
        // Oops, Euclidean algorithm already terminated?
        throw new ReedSolomonException("r_{i-1} was zero");
      }
      r = rLastLast;
      GenericGFPoly q = field.getZero();
      int denominatorLeadingTerm = rLast.getCoefficient(rLast.getDegree());
      int dltInverse = field.inverse(denominatorLeadingTerm);
      while (r.getDegree() >= rLast.getDegree() && !r.isZero()) {
        int degreeDiff = r.getDegree() - rLast.getDegree();
        int scale = field.multiply(r.getCoefficient(r.getDegree()), dltInverse);
        q = q.addOrSubtract(field.buildMonomial(degreeDiff, scale));
        r = r.addOrSubtract(rLast.multiplyByMonomial(degreeDiff, scale));
      }

      t = q.multiply(tLast).addOrSubtract(tLastLast);

      if (!r.isZero() && r.getDegree() >= rLast.getDegree()) {
        throw new IllegalStateException("Division algorithm failed to reduce polynomial? " +
          "r: " + r + ", rLast: " + rLast);
      }
    }

    int sigmaTildeAtZero = t.getCoefficient(0);
    if (sigmaTildeAtZero == 0) {
      throw new ReedSolomonException("sigmaTilde(0) was zero");
    }

    int inverse = field.inverse(sigmaTildeAtZero);
    GenericGFPoly sigma = t.multiply(inverse);
    GenericGFPoly omega = r.multiply(inverse);
    if (omega.isZero() || omega.getDegree() >= sigma.getDegree()) {
      throw new ReedSolomonException("Error evaluator degree is not less than error locator degree");
    }
    return new GenericGFPoly[]{sigma, omega};
  }

  private int[] findErrorLocations(GenericGFPoly errorLocator) throws ReedSolomonException {
    // This is a direct application of Chien's search
    int numErrors = errorLocator.getDegree();
    if (numErrors == 1) { // shortcut
      return new int[] { errorLocator.getCoefficient(1) };
    }
    int[] result = new int[numErrors];
    int e = 0;
    for (int i = 1; i < field.getSize() && e < numErrors; i++) {
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

  private int[] findErrorMagnitudes(GenericGFPoly errorEvaluator, int[] errorLocations) {
    // This is directly applying Forney's Formula
    int s = errorLocations.length;
    int[] result = new int[s];
    int order = field.getSize() - 1;
    int generatorBase = field.getGeneratorBase() % order;
    if (generatorBase < 0) {
      generatorBase += order;
    }
    for (int i = 0; i < s; i++) {
      int xiInverse = field.inverse(errorLocations[i]);
      int denominator = 1;
      for (int j = 0; j < s; j++) {
        if (i != j) {
          //denominator = field.multiply(denominator,
          //    GenericGF.addOrSubtract(1, field.multiply(errorLocations[j], xiInverse)));
          // Above should work but fails on some Apple and Linux JDKs due to a Hotspot bug.
          // Below is a funny-looking workaround from Steven Parkes
          int term = field.multiply(errorLocations[j], xiInverse);
          int termPlus1 = (term & 0x1) == 0 ? term | 1 : term & ~1;
          denominator = field.multiply(denominator, termPlus1);
        }
      }
      result[i] = field.multiply(errorEvaluator.evaluateAt(xiInverse),
          field.inverse(denominator));
      if (generatorBase != 0) {
        int factor = generatorBase == 1
            ? xiInverse
            : field.exp((int) (((long) field.log(xiInverse) * generatorBase) % order));
        result[i] = field.multiply(result[i], factor);
      }
    }
    return result;
  }

}
