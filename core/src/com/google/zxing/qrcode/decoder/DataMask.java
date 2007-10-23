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

package com.google.zxing.qrcode.decoder;

/**
 * Encapsulates data masks for the data bits in a QR code, per ISO 18004:2006 6.8. Implementations
 * of this class can un-mask a raw BitMatrix. For simplicity, they will unmask the entire BitMatrix,
 * including areas used for finder patterns, timing patterns, etc. These areas should be unused
 * after the point they are unmasked anyway.
 *
 * Note that the diagram in section 6.8.1 is misleading since it indicates that i is column position
 * and j is row position. In fact, as the text says, i is row position and j is column position.
 *
 * @author srowen@google.com (Sean Owen)
 */
abstract class DataMask {

  /**
   * See ISO 18004:2006 6.8.1
   */
  private static final DataMask[] DATA_MASKS = new DataMask[]{
      new DataMask000(),
      new DataMask001(),
      new DataMask010(),
      new DataMask011(),
      new DataMask100(),
      new DataMask101(),
      new DataMask110(),
      new DataMask111(),
  };

  private DataMask() {
  }

  abstract void unmaskBitMatrix(int[] bits, int dimension);

  static DataMask forReference(int reference) {
    if (reference < 0 || reference > 7) {
      throw new IllegalArgumentException();
    }
    return DATA_MASKS[reference];
  }

  /**
   * 000: mask bits for which (i + j) mod 2 == 0
   */
  private static class DataMask000 extends DataMask {
    private static final int BITMASK = 0x55555555; // = 010101...

    void unmaskBitMatrix(int[] bits, int dimension) {
      // This one's easy. Because the dimension of BitMatrix is always odd,
      // we can merely flip every other bit
      int max = bits.length;
      for (int i = 0; i < max; i++) {
        bits[i] ^= BITMASK;
      }
    }
  }

  /**
   * 001: mask bits for which j mod 2 == 0
   */
  private static class DataMask001 extends DataMask {
    void unmaskBitMatrix(int[] bits, int dimension) {
      int bitMask = 0;
      int count = 0;
      int offset = 0;
      for (int j = 0; j < dimension; j++) {
        for (int i = 0; i < dimension; i++) {
          if ((i & 0x01) == 0) {
            bitMask |= 1 << count;
          }
          if (++count == 32) {
            bits[offset++] ^= bitMask;
            count = 0;
            bitMask = 0;
          }
        }
      }
      bits[offset] ^= bitMask;
    }
  }

  /**
   * 010: mask bits for which j mod 3 == 0
   */
  private static class DataMask010 extends DataMask {
    void unmaskBitMatrix(int[] bits, int dimension) {
      int bitMask = 0;
      int count = 0;
      int offset = 0;
      for (int j = 0; j < dimension; j++) {
        boolean columnMasked = j % 3 == 0;
        for (int i = 0; i < dimension; i++) {
          if (columnMasked) {
            bitMask |= 1 << count;
          }
          if (++count == 32) {
            bits[offset++] ^= bitMask;
            count = 0;
            bitMask = 0;
          }
        }
      }
      bits[offset] ^= bitMask;
    }
  }

  /**
   * 011: mask bits for which (i + j) mod 3 == 0
   */
  private static class DataMask011 extends DataMask {
    void unmaskBitMatrix(int[] bits, int dimension) {
      int bitMask = 0;
      int count = 0;
      int offset = 0;
      for (int j = 0; j < dimension; j++) {
        for (int i = 0; i < dimension; i++) {
          if ((i + j) % 3 == 0) {
            bitMask |= 1 << count;
          }
          if (++count == 32) {
            bits[offset++] ^= bitMask;
            count = 0;
            bitMask = 0;
          }
        }
      }
      bits[offset] ^= bitMask;
    }
  }

  /**
   * 100: mask bits for which (i/2 + j/3) mod 2 == 0
   */
  private static class DataMask100 extends DataMask {
    void unmaskBitMatrix(int[] bits, int dimension) {
      int bitMask = 0;
      int count = 0;
      int offset = 0;
      for (int j = 0; j < dimension; j++) {
        int jComponent = j / 3;
        for (int i = 0; i < dimension; i++) {
          if (((i >> 1 + jComponent) & 0x01) == 0) {
            bitMask |= 1 << count;
          }
          if (++count == 32) {
            bits[offset++] ^= bitMask;
            count = 0;
            bitMask = 0;
          }
        }
      }
      bits[offset] ^= bitMask;
    }
  }

  /**
   * 101: mask bits for which ij mod 2 + ij mod 3 == 0
   */
  private static class DataMask101 extends DataMask {
    void unmaskBitMatrix(int[] bits, int dimension) {
      int bitMask = 0;
      int count = 0;
      int offset = 0;
      for (int j = 0; j < dimension; j++) {
        for (int i = 0; i < dimension; i++) {
          int product = i * j;
          if (((product & 0x01) == 0) && product % 3 == 0) {
            bitMask |= 1 << count;
          }
          if (++count == 32) {
            bits[offset++] ^= bitMask;
            count = 0;
            bitMask = 0;
          }
        }
      }
      bits[offset] ^= bitMask;
    }
  }

  /**
   * 110: mask bits for which (ij mod 2 + ij mod 3) mod 2 == 0
   */
  private static class DataMask110 extends DataMask {
    void unmaskBitMatrix(int[] bits, int dimension) {
      int bitMask = 0;
      int count = 0;
      int offset = 0;
      for (int j = 0; j < dimension; j++) {
        for (int i = 0; i < dimension; i++) {
          int product = i * j;
          if ((((product & 0x01) + product % 3) & 0x01) == 0) {
            bitMask |= 1 << count;
          }
          if (++count == 32) {
            bits[offset++] ^= bitMask;
            count = 0;
            bitMask = 0;
          }
        }
      }
      bits[offset] ^= bitMask;
    }
  }

  /**
   * 111: mask bits for which ((i+j)mod 2 + ij mod 3) mod 2 == 0
   */
  private static class DataMask111 extends DataMask {
    void unmaskBitMatrix(int[] bits, int dimension) {
      int bitMask = 0;
      int count = 0;
      int offset = 0;
      for (int j = 0; j < dimension; j++) {
        for (int i = 0; i < dimension; i++) {
          if (((((i + j) & 0x01) + (i * j) % 3) & 0x01) == 0) {
            bitMask |= 1 << count;
          }
          if (++count == 32) {
            bits[offset++] ^= bitMask;
            count = 0;
            bitMask = 0;
          }
        }
      }
      bits[offset] ^= bitMask;
    }
  }
}
