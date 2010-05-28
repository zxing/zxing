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
 * <p>This class contains utility methods for performing mathematical operations over
 * the Galois Field GF(256). Operations use a given primitive polynomial in calculations.</p>
 *
 * <p>Throughout this package, elements of GF(256) are represented as an <code>int</code>
 * for convenience and speed (but at the cost of memory).
 * Only the bottom 8 bits are really used.</p>
 *
 * @author Sean Owen
 */
public final class GF256 {

  public static final GF256 QR_CODE_FIELD = new GF256(0x011D); // x^8 + x^4 + x^3 + x^2 + 1
  public static final GF256 DATA_MATRIX_FIELD = new GF256(0x012D); // x^8 + x^5 + x^3 + x^2 + 1

  private final int[] expTable;
  private final int[] logTable;
  private final GF256Poly zero;
  private final GF256Poly one;

  /**
   * Create a representation of GF(256) using the given primitive polynomial.
   *
   * @param primitive irreducible polynomial whose coefficients are represented by
   *  the bits of an int, where the least-significant bit represents the constant
   *  coefficient
   */
  private GF256(int primitive) {
    expTable = new int[256];
    logTable = new int[256];
    int x = 1;
    for (int i = 0; i < 256; i++) {
      expTable[i] = x;
      x <<= 1; // x = x * 2; we're assuming the generator alpha is 2
      if (x >= 0x100) {
        x ^= primitive;
      }
    }
    for (int i = 0; i < 255; i++) {
      logTable[expTable[i]] = i;
    }
    // logTable[0] == 0 but this should never be used
    zero = new GF256Poly(this, new int[]{0});
    one = new GF256Poly(this, new int[]{1});
  }

  GF256Poly getZero() {
    return zero;
  }

  GF256Poly getOne() {
    return one;
  }

  /**
   * @return the monomial representing coefficient * x^degree
   */
  GF256Poly buildMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return zero;
    }
    int[] coefficients = new int[degree + 1];
    coefficients[0] = coefficient;
    return new GF256Poly(this, coefficients);
  }

  /**
   * Implements both addition and subtraction -- they are the same in GF(256).
   *
   * @return sum/difference of a and b
   */
  static int addOrSubtract(int a, int b) {
    return a ^ b;
  }

  /**
   * @return 2 to the power of a in GF(256)
   */
  int exp(int a) {
    return expTable[a];
  }

  /**
   * @return base 2 log of a in GF(256)
   */
  int log(int a) {
    if (a == 0) {
      throw new IllegalArgumentException();
    }
    return logTable[a];
  }

  /**
   * @return multiplicative inverse of a
   */
  int inverse(int a) {
    if (a == 0) {
      throw new ArithmeticException();
    }
    return expTable[255 - logTable[a]];
  }

  /**
   * @param a
   * @param b
   * @return product of a and b in GF(256)
   */
  int multiply(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }
    int logSum = logTable[a] + logTable[b];
    // index is a sped-up alternative to logSum % 255 since sum
    // is in [0,510]. Thanks to jmsachs for the idea
    return expTable[(logSum & 0xFF) + (logSum >>> 8)];
  }

}
