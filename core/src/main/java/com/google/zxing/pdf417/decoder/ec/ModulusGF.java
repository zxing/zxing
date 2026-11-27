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

import com.google.zxing.pdf417.PDF417Common;

/**
 * <p>A field based on powers of a generator integer, modulo some modulus.</p>
 *
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.GenericGF
 */
public final class ModulusGF {

  public static final ModulusGF PDF417_GF = new ModulusGF(PDF417Common.NUMBER_OF_CODEWORDS, 3);

  private final int[] expTable;
  private final int[] logTable;
  private final ModulusPoly zero;
  private final ModulusPoly one;
  private final int modulus;

  private ModulusGF(int modulus, int generator) {
    this.modulus = modulus;
    int order = modulus - 1;
    expTable = new int[2 * order];
    logTable = new int[modulus];
    int x = 1;
    for (int i = 0; i < order; i++) {
      expTable[i] = x;
      expTable[i + order] = x;
      logTable[x] = i;
      x = (x * generator) % modulus;
    }
    // logTable[0] == 0 but this should never be used
    zero = new ModulusPoly(this, new int[]{0});
    one = new ModulusPoly(this, new int[]{1});
  }


  ModulusPoly getZero() {
    return zero;
  }

  ModulusPoly getOne() {
    return one;
  }

  ModulusPoly buildMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return zero;
    }
    int[] coefficients = new int[degree + 1];
    coefficients[0] = coefficient;
    return new ModulusPoly(this, coefficients);
  }

  int add(int a, int b) {
    int sum = a + b;
    return sum >= modulus ? sum - modulus : sum;
  }

  int subtract(int a, int b) {
    int diff = a - b;
    return diff < 0 ? diff + modulus : diff;
  }

  int exp(int a) {
    return expTable[a];
  }

  int log(int a) {
    if (a == 0) {
      throw new IllegalArgumentException();
    }
    return logTable[a];
  }

  int inverse(int a) {
    if (a == 0) {
      throw new ArithmeticException();
    }
    return expTable[modulus - logTable[a] - 1];
  }

  int multiply(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }
    return expTable[logTable[a] + logTable[b]];
  }

  int multiplyByLog(int a, int logB) {
    if (a == 0) {
      return 0;
    }
    return expTable[logTable[a] + logB];
  }

  int evaluateByLog(int[] coefficients, int logA) {
    int mod = this.modulus;
    if (logA == 0) {
      int result = 0;
      for (int c : coefficients) {
        result += c;
        if (result >= mod) {
          result -= mod;
        }
      }
      return result;
    }
    int[] exp = this.expTable;
    int[] log = this.logTable;
    int result = coefficients[0];
    for (int i = 1; i < coefficients.length; i++) {
      if (result != 0) {
        result = exp[log[result] + logA];
      }
      result += coefficients[i];
      if (result >= mod) {
        result -= mod;
      }
    }
    return result;
  }

  int getSize() {
    return modulus;
  }

}
