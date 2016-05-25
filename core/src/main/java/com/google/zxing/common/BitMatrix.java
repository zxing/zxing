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

import java.util.Arrays;

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
public final class BitMatrix implements Cloneable {

  private static final int MAXBITS = 32;
  private int width = 0;
  private int height;
  private int rowSize;
  private int[] bits;

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
    this.rowSize = (width + 31) / MAXBITS;
    bits = new int[rowSize * height];
  }

  private BitMatrix(int width, int height, int rowSize, int[] bits) {
    this.width = width;
    this.height = height;
    this.rowSize = rowSize;
    this.bits = bits;
  }

  public static BitMatrix parse(String stringRepresentation, String setString, String unsetString) {
    if (stringRepresentation == null) {
      throw new IllegalArgumentException();
    }

    boolean[] bits = new boolean[stringRepresentation.length()];
    int bitsPosition = 0;
    int rowStartPosition = 0;
    int rowLength = -1;
    int numberOfRows = 0;
    int position = 0;
    while (position < stringRepresentation.length()) {
      if (stringRepresentation.charAt(position) == '\n' ||
          stringRepresentation.charAt(position) == '\r') {
        if (bitsPosition > rowStartPosition) {
          rowLength = getRowLength(bitsPosition, rowStartPosition, rowLength);
          rowStartPosition = bitsPosition;
          numberOfRows++;
        }
        position++;
      } else{
        position = handleStringManipulation(stringRepresentation, setString, unsetString, bits, bitsPosition, position);
        bitsPosition++;
      }
    }
    
    // no EOL at end?
    if (bitsPosition > rowStartPosition) {
        rowLength = getRowLength(bitsPosition, rowStartPosition, rowLength);
        numberOfRows++;
    }
    return createMatrix(bits, bitsPosition, rowLength, numberOfRows);
  }

private static int handleStringManipulation(String stringRepresentation, String setString, String unsetString,
        boolean[] bits, int bitsPosition, int position) {
    if (stringRepresentation.substring(position, position + setString.length()).equals(setString)) {
          setBitAtArrayPosition(bits, bitsPosition, true);
          return position + setString.length();
      } else if (stringRepresentation.substring(position, position + unsetString.length()).equals(unsetString)) {
          setBitAtArrayPosition(bits, bitsPosition, false);
          return position + unsetString.length();
      } else {
        throw new IllegalArgumentException(
            "illegal character encountered: " + stringRepresentation.substring(position));
      }
}

private static BitMatrix createMatrix(boolean[] bits, int bitsPosition, int rowLength, int numberOfRows) {
    BitMatrix matrix = new BitMatrix(rowLength, numberOfRows);
    for (int i = 0; i < bitsPosition; i++) {
      if (bits[i]) {
        matrix.set(i % rowLength, i / rowLength);
      }
    }
    return matrix;
}

    private static int getRowLength(int bitsPosition, int rowStartPosition, int rowLength) {
        
        if(rowLength == -1) {
          return bitsPosition - rowStartPosition;
        } else if (bitsPosition - rowStartPosition != rowLength) {
          throw new IllegalArgumentException("row lengths do not match");
        }
        return rowLength;
    }

    private static void setBitAtArrayPosition(boolean[] bits, int bitsPos, boolean value) {
        bits[bitsPos] = value;
    }

    /**
   * <p>Gets the requested bit, where true means black.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   * @return value of given bit in matrix
   */
  public boolean get(int x, int y) {
    int offset = y * rowSize + (x / MAXBITS);
    return ((bits[offset] >>> (x & 0x1f)) & 1) != 0;
  }

  /**
   * <p>Sets the given bit to true.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void set(int x, int y) {
    int offset = y * rowSize + (x / MAXBITS);
    bits[offset] |= 1 << (x & 0x1f);
  }

  public void unset(int x, int y) {
    int offset = y * rowSize + (x / MAXBITS);
    bits[offset] &= ~(1 << (x & 0x1f));
  }

  /**
   * <p>Flips the given bit.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void flip(int x, int y) {
    int offset = y * rowSize + (x / MAXBITS);
    bits[offset] ^= 1 << (x & 0x1f);
  }

  /**
   * Exclusive-or (XOR): Flip the bit in this {@code BitMatrix} if the corresponding
   * mask bit is set.
   *
   * @param mask XOR mask
   */
  public void xor(BitMatrix mask) {
    if (width != mask.getWidth() || height != mask.getHeight()
        || rowSize != mask.getRowSize()) {
      throw new IllegalArgumentException("input matrix dimensions do not match");
    }
    BitArray rowArray = new BitArray(width / MAXBITS + 1);
    for (int y = 0; y < height; y++) {
      int offset = y * rowSize;
      int[] row = mask.getRow(y, rowArray).getBitArray();
      for (int x = 0; x < rowSize; x++) {
        bits[offset + x] ^= row[x];
      }
    }
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
   * <p>Sets a square bitRegion of the bit matrix to true.</p>
   *
   * @param bitRegion
   */
  public void setRegion(BitRegion bitRegion) {

    bitRegion.checkValidityBitRegion();

    int right = bitRegion.calculateRight();
    int bottom = bitRegion.calculateBottom();

    if (bottom > this.height || right > this.width) {
      throw new IllegalArgumentException("The bitRegion must fit inside the matrix");
    }

    for (int y = bitRegion.getTop(); y < bottom; y++) {
      int offset = y * rowSize;
      for (int x = bitRegion.getLeft(); x < right; x++) {
        bits[offset + (x / MAXBITS)] |= 1 << (x & 0x1f);
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
      BitArray newRow = row;
    if (newRow == null || newRow.getSize() < width) {
      newRow = new BitArray(width);
    } else {
      newRow.clear();
    }
    int offset = y * rowSize;
    for (int x = 0; x < rowSize; x++) {
      newRow.setBulk(x * MAXBITS, bits[offset + x]);
    }
    return newRow;
  }

  /**
   * @param y row to set
   * @param row {@link BitArray} to copy from
   */
  public void setRow(int y, BitArray row) {
    System.arraycopy(row.getBitArray(), 0, bits, y * rowSize, rowSize);
  }

  /**
   * Modifies this {@code BitMatrix} to represent the same but rotated 180 degrees
   */
  public void rotate180() {
    int matrixWidth = getWidth();
    int matrixHeight = getHeight();
    BitArray topRow = new BitArray(matrixWidth);
    BitArray bottomRow = new BitArray(matrixWidth);
    for (int i = 0; i < (matrixHeight+1) / 2; i++) {
      topRow = getRow(i, topRow);
      bottomRow = getRow(matrixHeight - 1 - i, bottomRow);
      topRow.reverse();
      bottomRow.reverse();
      setRow(i, bottomRow);
      setRow(matrixHeight - 1 - i, topRow);
    }
  }

  /**
   * This is useful in detecting the enclosing rectangle of a 'pure' barcode.
   *
   * @return {@code left,top,width,height} enclosing rectangle of all 1 bits, or null if it is all white
   */
  public int[] getEnclosingRectangle() {
    int left = width;
    int top = height;
    int right = -1;
    int bottom = -1;

    for (int y = 0; y < height; y++) {
      for (int x32 = 0; x32 < rowSize; x32++) {
        int theBits = bits[y * rowSize + x32];
        if (theBits != 0) {
          top = y < top ? y : top;
          bottom = y > bottom ? y : bottom;
          
          left = modifyRectangleLeft(left, x32, theBits);
          right = modifyRectangleRight(right, x32, theBits);
        }
      }
    }

    int rectWidth = right - left;
    int rectHeight = bottom - top;

    if (rectWidth < 0 || rectHeight < 0) {
      return new int[]{};
    }

    return new int[] {left, top, rectWidth, rectHeight};
  }

private int modifyRectangleLeft(int left, int x32, int theBits) {
    int tempLeft = left;
    if (x32 * MAXBITS < tempLeft) {
        int bit = 0;
        while ((theBits << (31 - bit)) == 0) {
          bit++;
        }
        if ((x32 * MAXBITS + bit) < tempLeft) {
            tempLeft = x32 * MAXBITS + bit;
        }
      }
    return tempLeft;
}

private int modifyRectangleRight(int right, int x32, int theBits) {
    int tempRight = right;
    if (x32 * MAXBITS + 31 > tempRight) {
        int bit = 31;
        while ((theBits >>> bit) == 0) {
          bit--;
        }
        if ((x32 * MAXBITS + bit) > tempRight) {
            tempRight = x32 * MAXBITS + bit;
        }
      }
    return tempRight;
}

  /**
   * This is useful in detecting a corner of a 'pure' barcode.
   *
   * @return {@code x,y} coordinate of top-left-most 1 bit, or null if it is all white
   */
  public int[] getTopLeftOnBit() {
    int bitsOffset = 0;
    while (bitsOffset < bits.length && bits[bitsOffset] == 0) {
      bitsOffset++;
    }
    if (bitsOffset == bits.length) {
      return new int[]{};
    }
    int y = bitsOffset / rowSize;
    int x = (bitsOffset % rowSize) * MAXBITS;

    int theBits = bits[bitsOffset];
    int bit = 0;
    while ((theBits << (31-bit)) == 0) {
      bit++;
    }
    x += bit;
    return new int[] {x, y};
  }

  public int[] getBottomRightOnBit() {
    int bitsOffset = bits.length - 1;
    while (bitsOffset >= 0 && bits[bitsOffset] == 0) {
      bitsOffset--;
    }
    if (bitsOffset < 0) {
      return new int[]{};
    }

    int y = bitsOffset / rowSize;
    int x = (bitsOffset % rowSize) * MAXBITS;

    int theBits = bits[bitsOffset];
    int bit = 31;
    while ((theBits >>> bit) == 0) {
      bit--;
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

  /**
   * @return The row size of the matrix
   */
  public int getRowSize() {
    return rowSize;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BitMatrix)) {
      return false;
    }
    BitMatrix other = (BitMatrix) o;
    return width == other.width && height == other.height && rowSize == other.rowSize &&
    Arrays.equals(bits, other.bits);
  }

  @Override
  public int hashCode() {
    int hash = width;
    hash = 31 * hash + width;
    hash = 31 * hash + height;
    hash = 31 * hash + rowSize;
     hash = 31 * hash + Arrays.hashCode(bits);
    return hash;
  }

  /**
   * @return string representation using "X" for set and " " for unset bits
   */
  @Override
  public String toString() {
    return toString("X ", "  ");
  }

  /**
   * @param setString representation of a set bit
   * @param unsetString representation of an unset bit
   * @return string representation of entire matrix utilizing given strings
   */
  public String toString(String setString, String unsetString) {
    return toString(setString, unsetString, "\n");
  }

  /**
   * @param setString representation of a set bit
   * @param unsetString representation of an unset bit
   * @param lineSeparator newline character in string representation
   * @return string representation of entire matrix utilizing given strings and line separator
   * @deprecated call {@link #toString(String,String)} only, which uses \n line separator always
   */
  @Deprecated
  public String toString(String setString, String unsetString, String lineSeparator) {
    StringBuilder result = new StringBuilder(height * (width + 1));
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        result.append(get(x, y) ? setString : unsetString);
      }
      result.append(lineSeparator);
    }
    return result.toString();
  }

  @Override
  public BitMatrix clone() {
    return new BitMatrix(width, height, rowSize, bits.clone());
  }

}
