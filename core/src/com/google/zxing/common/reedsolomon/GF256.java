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
 * <p>This class contains utility methods for performing mathematical operations over
 * the Galois Field GF(256). Operations use the primitive polynomial
 * x^8 + x^4 + x^3 + x^2 + 1 in calculations.</p>
 *
 * <p>Throughout this package, elements of GF(256) are represented as an <code>int</code>
 * for convenience and speed (but at the cost of memory).
 * Only the bottom 8 bits are really used.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
final class GF256 {

  private static final int PRIMITIVE = 0x011D;
  private static final int[] exp = new int[256];
  private static final int[] log = new int[256];
  static {
    int x = 1;
    for (int i = 0; i < 256; i++) {
      exp[i] = x;
      x <<= 1; // x = x * 2; we're assuming the generator alpha is 2
      if (x >= 0x100) {
        x ^= PRIMITIVE;
      }
    }
    for (int i = 0; i < 255; i++) {
      log[exp[i]] = i;
    }
    // log[0] == 0 but this should never be used
  }

  private GF256() {
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
  static int exp(int a) {
    return exp[a];
  }

  /**
   * @return base 2 log of a in GF(256)
   */
  static int log(int a) {
    if (a == 0) {
      throw new IllegalArgumentException();
    }
    return log[a];
  }

  /**
   * @return multiplicative inverse of a
   */
  static int inverse(int a) {
    if (a == 0) {
      throw new ArithmeticException();
    }
    return exp[255 - log[a]];
  }

  /**
   *
   * @param a
   * @param b
   * @return product of a and b in GF(256)
   */
  static int multiply(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }
    if (a == 1) {
      return b;
    }
    if (b == 1) {
      return a;
    }
    return exp[(log[a] + log[b]) % 255];
  }

}