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

import com.google.zxing.LuminanceSource;

public class AdjustableBitMatrix extends BitMatrix {

  private final LuminanceSource source;
  private int blackpoint = 127;

  public AdjustableBitMatrix(LuminanceSource source) {
    this.source = source;
  }

  private boolean isBlack(byte value) {
    return (value & 0xff) < blackpoint;
  }

  @Override
  public boolean get(int x, int y) {
<<<<<<< Upstream, based on origin/master
    byte[] currentRow = source.getRow(y, null);
    return isBlack(currentRow[x]);
=======
    try {

      byte[] currentRow = source.getRow(y, null);
      return isBlack(currentRow[x]);

      //      if (x == 0 || x == source.getWidth() - 1) {
      //        return isBlack(currentRow[x]);
      //      }
      //      if (isBlack(currentRow[x - 1]) == isBlack(currentRow[x + 1]) && isBlack(currentRow[x]) != isBlack(currentRow[x - 1])) {
      //        return isBlack(currentRow[x - 1]);
      //      } else {
      //        return isBlack(currentRow[x]);
      //      }
      //
      //      
      //      byte[] previousRow = source.getRow(y - 1, null);
      //      byte[] nextRow = source.getRow(y + 1, null);
      //      
      //      return (source.getRow(y, null)[x] ;
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
    return false;
>>>>>>> 84c0c47 work in progress for a new PDF417 decoder
  }

  /**
   * <p>Sets the given bit to true.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  @Override
  public void set(int x, int y) {
    throw new RuntimeException();
  }

  /**
   * <p>Flips the given bit.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  @Override
  public void flip(int x, int y) {
    throw new RuntimeException();
  }

  /**
   * Clears all bits (sets to false).
   */
  @Override
  public void clear() {
    throw new RuntimeException();
  }

  /**
   * <p>Sets a square region of the bit matrix to true.</p>
   *
   * @param left The horizontal position to begin at (inclusive)
   * @param top The vertical position to begin at (inclusive)
   * @param width The width of the region
   * @param height The height of the region
   */
  @Override
  public void setRegion(int left, int top, int width, int height) {
    throw new RuntimeException();
  }

  /**
   * A fast method to retrieve one row of data from the matrix as a BitArray.
   *
   * @param y The row to retrieve
   * @param row An optional caller-allocated BitArray, will be allocated if null or too small
   * @return The resulting BitArray - this reference should always be used even when passing
   *         your own row
   */
  @Override
  public BitArray getRow(int y, BitArray row) {
    throw new RuntimeException();
  }

  /**
   * @param y row to set
   * @param row {@link BitArray} to copy from
   */
  @Override
  public void setRow(int y, BitArray row) {
    throw new RuntimeException();
  }

  /**
   * This is useful in detecting the enclosing rectangle of a 'pure' barcode.
   *
   * @return {left,top,width,height} enclosing rectangle of all 1 bits, or null if it is all white
   */
  @Override
  public int[] getEnclosingRectangle() {
    throw new RuntimeException();
  }

  /**
   * This is useful in detecting a corner of a 'pure' barcode.
   *
   * @return {x,y} coordinate of top-left-most 1 bit, or null if it is all white
   */
  @Override
  public int[] getTopLeftOnBit() {
    throw new RuntimeException();
  }

  @Override
  public int[] getBottomRightOnBit() {
    throw new RuntimeException();
  }

  /**
   * @return The width of the matrix
   */
  @Override
  public int getWidth() {
    return source.getWidth();
  }

  /**
   * @return The height of the matrix
   */
  @Override
  public int getHeight() {
    return source.getHeight();
  }

  public int getBlackpoint() {
    return blackpoint;
  }

  public void setBlackpoint(int blackpoint) {
    this.blackpoint = blackpoint;
  }

}
