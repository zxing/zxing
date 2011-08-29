/*
 * Copyright 2011 ZXing authors
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

package com.google.zxing.pdf417.encoder;

/**
 * Holds all of the information for a barcode in a format where it can be easily accessable
 *
 * @author Jacob Haynes
 */
final class BarcodeMatrix {

  private final BarcodeRow[] matrix;
  private int currentRow;
  private final int height;
  private final int width;

  /**
   * @param height the height of the matrix (Rows)
   * @param width  the width of the matrix (Cols)
   */
  BarcodeMatrix(int height, int width) {
    matrix = new BarcodeRow[height + 2];
    //Initializes the array to the correct width
    for (int i = 0, matrixLength = matrix.length; i < matrixLength; i++) {
      matrix[i] = new BarcodeRow((width + 4) * 17 + 1);
    }
    this.width = width * 17;
    this.height = height + 2;
    this.currentRow = 0;
  }

  void set(int x, int y, byte value) {
    matrix[y].set(x, value);
  }

  void setMatrix(int x, int y, boolean black) {
    set(x, y, (byte) (black ? 1 : 0));
  }

  void startRow() {
    ++currentRow;
  }

  BarcodeRow getCurrentRow() {
    return matrix[currentRow];
  }

  byte[][] getMatrix() {
    return getScaledMatrix(1, 1);
  }

  byte[][] getScaledMatrix(int Scale) {
    return getScaledMatrix(Scale, Scale);
  }

  byte[][] getScaledMatrix(int xScale, int yScale) {
    byte[][] matrixOut = new byte[height * yScale][width * xScale];
    int yMax = height * yScale;
    for (int ii = 0; ii < yMax; ii++) {
      matrixOut[yMax - ii - 1] = matrix[ii / yScale].getScaledRow(xScale);
    }
    return matrixOut;
  }
}
