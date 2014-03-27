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
 */

package com.google.zxing.pdf417.decoder.ec;

import com.google.zxing.ChecksumException;

/**
 * <p>PDF417 error correction implementation.</p>
 *
 * <p>This <a href="http://en.wikipedia.org/wiki/Reed%E2%80%93Solomon_error_correction#Example">example</a>
 * is quite useful in understanding the algorithm.</p>
 *
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.ReedSolomonDecoder
 */
public final class ErrorCorrection {

  private final ModulusGF field;

  public ErrorCorrection() {
    this.field = ModulusGF.PDF417_GF;
  }

  /**
   * @return number of errors
   */
  public int decode(int[] received,
                    int numECCodewords,
                    int[] erasures) throws ChecksumException {

    //codewords.Length - codewordIndex - 1
    for (int i = 0; i < erasures.length; i++) {
      erasures[i] = received.length - erasures[i] - 1;
    }

    ModulusPoly poly = new ModulusPoly(field, received);
    int[] S = new int[numECCodewords];
    boolean error = false;
    for (int i = numECCodewords; i > 0; i--) {
      int eval = poly.evaluateAt(field.exp(i));
      S[numECCodewords - i] = eval;
      if (eval != 0) {
        error = true;
      }
    }

    if (!error) {
      return 0;
    }

    ModulusPoly syndrome = new ModulusPoly(field, S);

    // lambda, omega
    ModulusPoly[] pols = BerlekampMassey(syndrome, erasures, numECCodewords);
    ModulusPoly sigma = pols[0];
    ModulusPoly omega = pols[1];

    int[] errorLocations = findErrorLocations(sigma);
    int[] errorMagnitudes = findErrorMagnitudes(omega, sigma, errorLocations);

    for (int i = 0; i < errorLocations.length; i++) {
      int position = received.length - 1 - field.log(errorLocations[i]);
      if (position < 0) {
        throw ChecksumException.getChecksumInstance();
      }
      received[position] = field.subtract(received[position], errorMagnitudes[i]);
    }
    return errorLocations.length;
  }

  ModulusPoly[] BerlekampMassey(ModulusPoly syn, int[] erasures, int numECCodewords) throws ChecksumException {
    /* initialize Gamma, the erasure locator polynomial */
    ModulusPoly gamma = initGamma(erasures);

    /* initialize to z */
    ModulusPoly D = ModulusPoly.copy(gamma);
    D = multiplyByZ(D);

    ModulusPoly psi = ModulusPoly.copy(gamma);
    int k = -1;
    int L = erasures.length;

    for (int n = erasures.length; n < numECCodewords; n++) {
      int d = computeDiscrepancy(psi, syn, L, n);

      if (d != 0) {
        /* psi2 = psi - d*D */
        ModulusPoly psi2 = psi.subtract(D.multiply(d));

        if (L < (n - k)) {
          int L2 = n - k;
          k = n - L;
          /* D = scale_poly(ginv(d), psi); */
          D = psi.multiply(field.inverse(d));
          L = L2;
        }

        /* psi = psi2 */
        psi = ModulusPoly.copy(psi2);
      }

      D = multiplyByZ(D);
    }

    ModulusPoly lambda = ModulusPoly.copy(psi);
    ModulusPoly omega = computeModifiedOmega(lambda, syn, numECCodewords);
    return new ModulusPoly[] { lambda, omega };
  }

  private ModulusPoly initGamma(int[] erasures) {
    ModulusPoly gamma = field.getOne();
    for(int erasure : erasures) {
      int b = field.exp(erasure);
      // Add (1 - bx) term:
      ModulusPoly term = new ModulusPoly(field, new int[] { field.subtract(0, b), 1 });
      gamma = gamma.multiply(term);
    }

    return gamma;
  }

  /**
   * given Psi (called Lambda in Modified_Berlekamp_Massey) and synBytes,
   * compute the combined erasure/error evaluator polynomial as
   * Psi*S mod z^4
   * @param lambda error locator polynomial
   * @param syndromes syndromes polynomial
   * @param ECCNum mod value
   * @return combined erasure/error evaluator polynomial
   */
  ModulusPoly computeModifiedOmega(ModulusPoly lambda, ModulusPoly syndromes, int ECCNum) {
    int[] mod4 = new int[ECCNum];
    ModulusPoly product = lambda.multiply(syndromes);
    System.arraycopy(product.getCoefficients(), product.getCoefficients().length - ECCNum, mod4, 0, ECCNum);
    return new ModulusPoly(field, mod4);
  }

  private int computeDiscrepancy(ModulusPoly lambda, ModulusPoly S, int L, int n) throws ChecksumException {
    if(S.getDegree() < n || lambda.getDegree() < L) {
      throw ChecksumException.getChecksumInstance();
    }
    int sum = 0;

    for (int i = 0; i <= L; i++) {
        sum = field.add(sum, field.multiply(lambda.getCoefficient(i), S.getCoefficient(n - i)));
    }
    return sum;
  }

  /**
   * multiply by z, i.e., shift right by 1
   * @param src polynomial to be shifted
   * @return shifted polynomial
   */
  ModulusPoly multiplyByZ(ModulusPoly src) {
    return src.multiply(new ModulusPoly(field, new int[] { 1, 0 }));
  }


  private int[] findErrorLocations(ModulusPoly errorLocator) throws ChecksumException {
    // This is a direct application of Chien's search
    int numErrors = errorLocator.getDegree();
    int[] result = new int[numErrors];
    int e = 0;
    for (int i = 1; i < field.getSize() && e < numErrors; i++) {
      if (errorLocator.evaluateAt(i) == 0) {
        result[e] = field.inverse(i);
        e++;
      }
    }
    if (e != numErrors) {
      throw ChecksumException.getChecksumInstance();
    }
    return result;
  }

  private int[] findErrorMagnitudes(ModulusPoly errorEvaluator,
                                    ModulusPoly errorLocator,
                                    int[] errorLocations) {
    int errorLocatorDegree = errorLocator.getDegree();
    int[] formalDerivativeCoefficients = new int[errorLocatorDegree];
    for (int i = 1; i <= errorLocatorDegree; i++) {
      formalDerivativeCoefficients[errorLocatorDegree - i] =
          field.multiply(i, errorLocator.getCoefficient(i));
    }
    ModulusPoly formalDerivative = new ModulusPoly(field, formalDerivativeCoefficients);

    // This is directly applying Forney's Formula
    int s = errorLocations.length;
    int[] result = new int[s];
    for (int i = 0; i < s; i++) {
      int xiInverse = field.inverse(errorLocations[i]);
      int numerator = field.subtract(0, errorEvaluator.evaluateAt(xiInverse));
      int denominator = field.inverse(formalDerivative.evaluateAt(xiInverse));
      result[i] = field.multiply(numerator, denominator);
    }
    return result;
  }
}
