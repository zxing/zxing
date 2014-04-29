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
 * the Galois Fields. Operations use a given primitive polynomial in calculations.</p>
 *
 * <p>Throughout this package, elements of the GF are represented as an {@code int}
 * for convenience and speed (but at the cost of memory). This class is thread-safe.</p>
 *
 * @author Sean Owen
 * @author David Olivier
 */
public final class GenericGF {

  public static final GenericGF AZTEC_DATA_12 = new GenericGF(0x1069, 4096, 1); // x^12 + x^6 + x^5 + x^3 + 1
  public static final GenericGF AZTEC_DATA_10 = new GenericGF(0x409, 1024, 1); // x^10 + x^3 + 1
  public static final GenericGF AZTEC_DATA_6 = new GenericGF(0x43, 64, 1); // x^6 + x + 1
  public static final GenericGF AZTEC_PARAM = new GenericGF(0x13, 16, 1); // x^4 + x + 1
  public static final GenericGF QR_CODE_FIELD_256 = new GenericGF(0x011D, 256, 0); // x^8 + x^4 + x^3 + x^2 + 1
  public static final GenericGF DATA_MATRIX_FIELD_256 = new GenericGF(0x012D, 256, 1); // x^8 + x^5 + x^3 + x^2 + 1
  public static final GenericGF AZTEC_DATA_8 = DATA_MATRIX_FIELD_256;
  public static final GenericGF MAXICODE_FIELD_64 = AZTEC_DATA_6;

  private final int size;
  private final int primitive;
  private final int generatorBase;

  private static final Object LAZY_HOLDER_LOCK = new Object();
  private volatile LazyHolder lazyHolder;

  /**
   * Holder for shared resources which are lazily initialized.
   */
  private final class LazyHolder {
    private final int[] expTable;
    private final int[] logTable;
    private final GenericGFPoly zero;
    private final GenericGFPoly one;

    public LazyHolder() {
      // Final field freeze guarantees that the array values will be visible when the constructor
      // completes. (Java Concurrency in Practice section 16.3 and JSR-133 fig. 22-23)
      expTable = new int[size];
      logTable = new int[size];

      for (int i = 0, x = 1; i < size; i++) {
        expTable[i] = x;
        x <<= 1; // x = x * 2; we're assuming the generator alpha is 2
        if (x >= size) {
          x ^= primitive;
          x &= size - 1;
        }
      }
      for (int i = 0; i < size - 1; i++) {
        logTable[expTable[i]] = i;
      }
      // logTable[0] == 0 but this should never be used

      zero = new GenericGFPoly(GenericGF.this, new int[] { 0 });
      one = new GenericGFPoly(GenericGF.this, new int[] { 1 });
    }
  }

  /**
   * Create a representation of GF(size) using the given primitive polynomial.
   *
   * @param primitive irreducible polynomial whose coefficients are represented by
   *  the bits of an int, where the least-significant bit represents the constant
   *  coefficient
   * @param size the size of the field
   * @param b the factor b in the generator polynomial can be 0- or 1-based
   *  (g(x) = (x+a^b)(x+a^(b+1))...(x+a^(b+2t-1))).
   *  In most cases it should be 1, but for QR code it is 0.
   */
  public GenericGF(int primitive, int size, int b) {
    this.primitive = primitive;
    this.size = size;
    this.generatorBase = b;
  }

  private LazyHolder getLazyHolder() {
    // Double-checked lock idiom, not broken since JDK 1.5.
    LazyHolder instance = lazyHolder;
    if (instance == null) {
      synchronized (LAZY_HOLDER_LOCK) {
        instance = lazyHolder;
        if (instance == null) {
          lazyHolder = instance = new LazyHolder();
        }
      }
    }
    return instance;
  }
  
  GenericGFPoly getZero() {
    return getLazyHolder().zero;
  }

  GenericGFPoly getOne() {
    return getLazyHolder().one;
  }

  /**
   * @return the monomial representing coefficient * x^degree
   */
  GenericGFPoly buildMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return getLazyHolder().zero;
    }
    int[] coefficients = new int[degree + 1];
    coefficients[0] = coefficient;
    return new GenericGFPoly(this, coefficients);
  }

  /**
   * Implements both addition and subtraction -- they are the same in GF(size).
   *
   * @return sum/difference of a and b
   */
  static int addOrSubtract(int a, int b) {
    return a ^ b;
  }

  /**
   * @return 2 to the power of a in GF(size)
   */
  int exp(int a) {
    return getLazyHolder().expTable[a];
  }

  /**
   * @return base 2 log of a in GF(size)
   */
  int log(int a) {
    if (a == 0) {
      throw new IllegalArgumentException();
    }
    return getLazyHolder().logTable[a];
  }

  /**
   * @return multiplicative inverse of a
   */
  int inverse(int a) {
    if (a == 0) {
      throw new ArithmeticException();
    }
    LazyHolder lazyHolder = getLazyHolder();
    return lazyHolder.expTable[size - lazyHolder.logTable[a] - 1];
  }

  /**
   * @return product of a and b in GF(size)
   */
  int multiply(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }
    LazyHolder lazyHolder = getLazyHolder();
    return lazyHolder.expTable[(lazyHolder.logTable[a] + lazyHolder.logTable[b]) % (size - 1)];
  }

  public int getSize() {
    return size;
  }
  
  public int getGeneratorBase() {
    return generatorBase;
  }
  
  @Override
  public String toString() {
    return "GF(0x" + Integer.toHexString(primitive) + ',' + size + ')';
  }
  
}
