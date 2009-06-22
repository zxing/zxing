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

package com.google.zxing.common;

/**
 * <p>Represents a square matrix of bits. In function arguments below, and throughout the common
 * module, x is the column position, and y is the row position. The ordering is always x, y.
 * The origin is at the top-left.</p>
 *
 * <p>Internally the bits are represented in a compact 1-D array of 32-bit ints.
 * The ordering of bits is row-major. Within each int, the least significant bits are used first,
 * meaning they represent lower x values. This is compatible with BitArray's implementation.</p>
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class BitMatrix {

  private final int dimension;
  private final int[] bits;

  public BitMatrix(int dimension) {
    if (dimension < 1) {
      throw new IllegalArgumentException("dimension must be at least 1");
    }
    this.dimension = dimension;
    int numBits = dimension * dimension;
    int arraySize = numBits >> 5; // one int per 32 bits
    if ((numBits & 0x1F) != 0) { // plus one more if there are leftovers
      arraySize++;
    }
    bits = new int[arraySize];
  }

  /**
   * <p>Gets the requested bit, where true means black.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   * @return value of given bit in matrix
   */
  public boolean get(int x, int y) {
    int offset = y * dimension + x;
    return ((bits[offset >> 5] >>> (offset & 0x1F)) & 0x01) != 0;
  }

  /**
   * <p>Sets the given bit to true.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void set(int x, int y) {
    int offset = y * dimension + x;
    bits[offset >> 5] |= 1 << (offset & 0x1F);
  }

  /**
   * <p>Flips the given bit.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void flip(int x, int y) {
    int offset = y * dimension + x;
    bits[offset >> 5] ^= 1 << (offset & 0x1F);
  }

  /**
   * <p>Sets a square region of the bit matrix to true.</p>
   *
   * @param left The horizontal position to begin at (inclusive)
   * @param top The vertical position to begin at (inclusive)
   * @param width The width of the region
   * @param height The height of the region
   */
  public void setRegion(int left, int top, int width, int height) {
    if (top < 0 || left < 0) {
      throw new IllegalArgumentException("left and top must be nonnegative");
    }
    if (height < 1 || width < 1) {
      throw new IllegalArgumentException("height and width must be at least 1");
    }
    int right = left + width;
    int bottom = top + height;
    if (bottom > dimension || right > dimension) {
      throw new IllegalArgumentException(
          "top + height and left + width must be <= matrix dimension");
    }
    for (int y = top; y < bottom; y++) {
      int yoffset = dimension * y;
      for (int x = left; x < right; x++) {
        int xoffset = yoffset + x;
        bits[xoffset >> 5] |= 1 << (xoffset & 0x1F);
      }
    }
  }

  /**
   * @return row/column dimension of this matrix
   */
  public int getDimension() {
    return dimension;
  }

  public String toString() {
    StringBuffer result = new StringBuffer(dimension * (dimension + 1));
    for (int y = 0; y < dimension; y++) {
      for (int x = 0; x < dimension; x++) {
        result.append(get(x, y) ? "X " : "  ");
      }
      result.append('\n');
    }
    return result.toString();
  }

}
