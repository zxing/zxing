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
 * <p>Represents a 2D matrix of bits. In function arguments below, and throughout the common
 * module, x is the column position, and y is the row position. The ordering is always x, y.
 * The origin is at the top-left.</p>
 *
 * <p>Internally the bits are represented in a 1-D array of 32-bit ints. However, each row begins
 * with a new int. This is done intentionally so that we can copy out a row into a BitArray very
 * efficiently.</p>
 *
 * <p>The ordering of bits is row-major. Within each int, the least significant bits are used first,
 * meaning they represent lower x values. This is compatible with BitArray's implementation.</p>
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class BitMatrix {

  // TODO: Just like BitArray, these need to be public so ProGuard can inline them.
  public final int width;
  public final int height;
  public final int rowSize;
  public final int[] bits;

  // A helper to construct a square matrix.
  public BitMatrix(int dimension) {
    this(dimension, dimension);
  }

  public BitMatrix(int width, int height) {
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Both dimensions must be greater than 0");
    }
    this.width = width;
    this.height = height;
    this.rowSize = (width + 31) >> 5;
    bits = new int[rowSize * height];
  }

  /**
   * <p>Gets the requested bit, where true means black.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   * @return value of given bit in matrix
   */
  public boolean get(int x, int y) {
    int offset = y * rowSize + (x >> 5);
    return ((bits[offset] >>> (x & 0x1f)) & 1) != 0;
  }

  /**
   * <p>Sets the given bit to true.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void set(int x, int y) {
    int offset = y * rowSize + (x >> 5);
    bits[offset] |= 1 << (x & 0x1f);
  }

  /**
   * <p>Flips the given bit.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void flip(int x, int y) {
    int offset = y * rowSize + (x >> 5);
    bits[offset] ^= 1 << (x & 0x1f);
  }

  /**
   * Clears all bits (sets to false).
   */
  public void clear() {
    int max = bits.length;
    for (int i = 0; i < max; i++) {
      bits[i] = 0;
    }
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
      throw new IllegalArgumentException("Left and top must be nonnegative");
    }
    if (height < 1 || width < 1) {
      throw new IllegalArgumentException("Height and width must be at least 1");
    }
    int right = left + width;
    int bottom = top + height;
    if (bottom > this.height || right > this.width) {
      throw new IllegalArgumentException("The region must fit inside the matrix");
    }
    for (int y = top; y < bottom; y++) {
      int offset = y * rowSize;
      for (int x = left; x < right; x++) {
        bits[offset + (x >> 5)] |= 1 << (x & 0x1f);
      }
    }
  }

  /**
   * A fast method to retrieve one row of data from the matrix as a BitArray.
   *
   * @param y The row to retrieve
   * @param row An optional caller-allocated BitArray, will be allocated if null or too small
   * @return The resulting BitArray - this reference should always be used even when passing
   *         your own row
   */
  public BitArray getRow(int y, BitArray row) {
    if (row == null || row.getSize() < width) {
      row = new BitArray(width);
    }
    int offset = y * rowSize;
    for (int x = 0; x < rowSize; x++) {
      row.setBulk(x << 5, bits[offset + x]);
    }
    return row;
  }

  /**
   * This is useful in detecting a corner of a 'pure' barcode.
   * 
   * @return {x,y} coordinate of top-left-most 1 bit, or null if it is all white
   */
  public int[] getTopLeftOnBit() {
    int bitsOffset = 0;
    while (bitsOffset < bits.length && bits[bitsOffset] == 0) {
      bitsOffset++;
    }
    if (bitsOffset == bits.length) {
      return null;
    }
    int y = bitsOffset / rowSize;
    int x = (bitsOffset % rowSize) << 5;
    
    int theBits = bits[bitsOffset];
    int bit = 0;
    while ((theBits << (31-bit)) == 0) {
      bit++;
    }
    x += bit;
    return new int[] {x, y};
  }

  /**
   * @return The width of the matrix
   */
  public int getWidth() {
    return width;
  }

  /**
   * @return The height of the matrix
   */
  public int getHeight() {
    return height;
  }

  public boolean equals(Object o) {
    if (!(o instanceof BitMatrix)) {
      return false;
    }
    BitMatrix other = (BitMatrix) o;
    if (width != other.width || height != other.height ||
        rowSize != other.rowSize || bits.length != other.bits.length) {
      return false;
    }
    for (int i = 0; i < bits.length; i++) {
      if (bits[i] != other.bits[i]) {
        return false;
      }
    }
    return true;
  }

  public int hashCode() {
    int hash = width;
    hash = 31 * hash + width;
    hash = 31 * hash + height;
    hash = 31 * hash + rowSize;
    for (int i = 0; i < bits.length; i++) {
      hash = 31 * hash + bits[i];
    }
    return hash;
  }

  public String toString() {
    StringBuffer result = new StringBuffer(height * (width + 1));
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        result.append(get(x, y) ? "X " : "  ");
      }
      result.append('\n');
    }
    return result.toString();
  }

}
