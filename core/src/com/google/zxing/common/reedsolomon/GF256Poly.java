/*
 * Copyright 2007 Google Inc.
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
 * <p>Represents a polynomial whose coefficients are elements of GF(256).
 * Instances of this class are immutable.</p>
 *
 * <p>Much credit is due to William Rucklidge since portions of this code are an indirect
 * port of his C++ Reed-Solomon implementation.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
final class GF256Poly {

  /** Polynimal representing the monomial 0. */
  static final GF256Poly ZERO = new GF256Poly(new int[] { 0 });
  /** Polynimal representing the monomial 1. */
  static final GF256Poly ONE = new GF256Poly(new int[] { 1 });

  private final int[] coefficients;

  /**
   * @param coefficients coefficients as ints representing elements of GF(256), arranged
   *  from most significant (highest-power term) coefficient to least significant
   * @throws IllegalArgumentException if argument is null or empty,
   *  or if leading coefficient is 0 and this is not a
   *  constant polynomial (that is, it is not the monomial "0")
   */
  GF256Poly(int[] coefficients) {
    if (coefficients == null || coefficients.length == 0) {
      throw new IllegalArgumentException();
    }
    if (coefficients.length > 1 && coefficients[0] == 0) {
      // Leading term must be non-zero for anything except the constant polynomial "0"
      int firstNonZero = 1;
      while (firstNonZero < coefficients.length && coefficients[firstNonZero] == 0) {
        firstNonZero++;
      }
      if (firstNonZero == coefficients.length) {
        this.coefficients = ZERO.coefficients;
      } else {
        this.coefficients = new int[coefficients.length - firstNonZero];
        System.arraycopy(coefficients,
                         firstNonZero,
                         this.coefficients,
                         0,
                         this.coefficients.length);
      }
    } else {
      this.coefficients = coefficients;
    }
  }

  /**
   * @return degree of this polynomial
   */
  int getDegree() {
    return coefficients.length - 1;
  }

  /**
   * @return true iff this polynomial is the monomial "0"
   */
  boolean isZero() {
    return coefficients[0] == 0;
  }

  /**
   * @return the monomial representing coefficient * x^degree
   */
  static GF256Poly buildMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return ZERO;
    }
    int[] coefficients = new int[degree + 1];
    coefficients[0] = coefficient;
    return new GF256Poly(coefficients);
  }

  /**
   * @return coefficient of x^degree term in this polynomial
   */
  int getCoefficient(int degree) {
    return coefficients[coefficients.length - 1 - degree];
  }

  /**
   * @return evaluation of this polynomial at a given point
   */
  int evaluateAt(int a) {
    if (a == 0) {
      // Just return the x^0 coefficient
      return getCoefficient(0);
    }
    final int size = coefficients.length;
    if (a == 1) {
      // Just the sum of the coefficients
      int result = 0;
      for (int i = 0; i < size; i++) {
        result = GF256.addOrSubtract(result, coefficients[i]);
      }
      return result;
    }
    int result = coefficients[0];
    for (int i = 1; i < size; i++) {
      result = GF256.addOrSubtract(GF256.multiply(a, result), coefficients[i]);
    }
    return result;
  }

  int evaluateFormatDerivativeAt(int a) {
    int degree = getDegree();
    if (degree == 0) {
      // Derivative of a constant is zero.
      return 0;
    }

    int aToTheI = 1;
    int sum = getCoefficient(1);
    int aSquared = GF256.multiply(a, a);
    for (int i = 2; i < degree; i += 2) {
      aToTheI = GF256.multiply(aSquared, aToTheI);
      sum = GF256.addOrSubtract(sum, GF256.multiply(aToTheI, getCoefficient(i + 1)));
    }
    return sum;
  }

  GF256Poly addOrSubtract(GF256Poly other) {
    if (isZero()) {
      return other;
    }
    if (other.isZero()) {
      return this;
    }

    int[] smallerCoefficients = this.coefficients;
    int[] largerCoefficients = other.coefficients;
    if (smallerCoefficients.length > largerCoefficients.length) {
      int[] temp = smallerCoefficients;
      smallerCoefficients = largerCoefficients;
      largerCoefficients = temp;
    }
    int[] sumDiff = new int[largerCoefficients.length];
    int lengthDiff = largerCoefficients.length - smallerCoefficients.length;
    // Copy high-order terms only found in higher-degree polynomial's coefficients
    System.arraycopy(largerCoefficients, 0, sumDiff, 0, lengthDiff);

    for (int i = lengthDiff; i < largerCoefficients.length; i++) {
      sumDiff[i] = GF256.addOrSubtract(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
    }

    return new GF256Poly(sumDiff);
  }

  GF256Poly multiply(GF256Poly other) {
    if (isZero() || other.isZero()) {
      return ZERO;
    }
    int[] aCoefficients = this.coefficients;
    int aLength = aCoefficients.length;
    int[] bCoefficients = other.coefficients;
    int bLength = bCoefficients.length;
    int[] product = new int[aLength + bLength - 1];
    for (int i = 0; i < aLength; i++) {
      int aCoeff = aCoefficients[i];
      for (int j = 0; j < bLength; j++) {
        product[i + j] = GF256.addOrSubtract(product[i + j],
                                             GF256.multiply(aCoeff, bCoefficients[j]));
      }
    }
    return new GF256Poly(product);
  }

  GF256Poly multiply(int scalar) {
    if (scalar == 0) {
      return ZERO;
    }
    if (scalar == 1) {
      return this;
    }
    int size = coefficients.length;
    int[] product = new int[size];
    System.arraycopy(coefficients, 0, product, 0, size);
    for (int i = 0; i < size; i++) {
      product[i] = GF256.multiply(product[i], scalar);
    }
    return new GF256Poly(product);
  }

  GF256Poly multiplyByMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return ZERO;
    }
    int size = coefficients.length;    
    int[] product = new int[size + degree];
    System.arraycopy(coefficients, 0, product, 0, size);
    for (int i = 0; i < size; i++) {
      product[i] = GF256.multiply(product[i], coefficient);
    }
    return new GF256Poly(product);
  }

}
