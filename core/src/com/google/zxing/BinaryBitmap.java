/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing;

import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

/**
 * This class is the core bitmap class used by ZXing to represent 1 bit data. Reader objects
 * accept a BinaryBitmap and attempt to decode it.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class BinaryBitmap {

  private final Binarizer binarizer;
  private BitMatrix matrix;

  public BinaryBitmap(Binarizer binarizer) {
    if (binarizer == null) {
      throw new IllegalArgumentException("Binarizer must be non-null.");
    }
    this.binarizer = binarizer;
    matrix = null;
  }

  /**
   * @return The width of the bitmap.
   */
  public int getWidth() {
    return binarizer.getLuminanceSource().getWidth();
  }

  /**
   * @return The height of the bitmap.
   */
  public int getHeight() {
    return binarizer.getLuminanceSource().getHeight();
  }

  /**
   * Converts one row of luminance data to 1 bit data. May actually do the conversion, or return
   * cached data. Callers should assume this method is expensive and call it as seldom as possible.
   * This method is intended for decoding 1D barcodes and may choose to apply sharpening.
   *
   * @param y The row to fetch, 0 <= y < bitmap height.
   * @param row An optional preallocated array. If null or too small, it will be ignored.
   *            If used, the Binarizer will call BitArray.clear(). Always use the returned object.
   * @return The array of bits for this row (true means black).
   */
  public BitArray getBlackRow(int y, BitArray row) throws ReaderException {
    return binarizer.getBlackRow(y, row);
  }

  /**
   * Converts a 2D array of luminance data to 1 bit. As above, assume this method is expensive
   * and do not call it repeatedly. This method is intended for decoding 2D barcodes and may or
   * may not apply sharpening. Therefore, a row from this matrix may not be identical to one
   * fetched using getBlackRow(), so don't mix and match between them.
   *
   * @return The 2D array of bits for the image (true means black).
   */
  public BitMatrix getBlackMatrix() throws ReaderException {
    // The matrix is created on demand the first time it is requested, then cached. There are two
    // reasons for this:
    // 1. This work will never be done if the caller only installs 1D Reader objects.
    // 2. This work will only be done once even if the caller installs multiple 2D Readers.
    if (matrix == null) {
      matrix = binarizer.getBlackMatrix();
    }
    return matrix;
  }

  /**
   * @return Whether this bitmap can be cropped.
   */
  public boolean isCropSupported() {
    return binarizer.getLuminanceSource().isCropSupported();
  }

  /**
   * Returns a new object with cropped image data. Implementations may keep a reference to the
   * original data rather than a copy. Only callable if isCropSupported() is true.
   *
   * @param left The left coordinate, 0 <= left < getWidth().
   * @param top The top coordinate, 0 <= top <= getHeight().
   * @param width The width of the rectangle to crop.
   * @param height The height of the rectangle to crop.
   * @return A cropped version of this object.
   */
  public BinaryBitmap crop(int left, int top, int width, int height) {
    LuminanceSource newSource = binarizer.getLuminanceSource().crop(left, top, width, height);
    return new BinaryBitmap(binarizer.createBinarizer(newSource));
  }

  /**
   * @return Whether this bitmap supports counter-clockwise rotation.
   */
  public boolean isRotateSupported() {
    return binarizer.getLuminanceSource().isRotateSupported();
  }

  /**
   * Returns a new object with rotated image data. Only callable if isRotateSupported() is true.
   *
   * @return A rotated version of this object.
   */
  public BinaryBitmap rotateCounterClockwise() {
    LuminanceSource newSource = binarizer.getLuminanceSource().rotateCounterClockwise();
    return new BinaryBitmap(binarizer.createBinarizer(newSource));
  }

  /**
   * @deprecated
   *
   * FIXME: REMOVE!
   * These three methods are TEMPORARY and should be removed by the end of July 2009.
   * They are only here so the transition from MonochromeBitmapSource to BinaryBitmap
   * can be done in stages. We need to go through all the Reader objects and convert
   * these calls to getBlackRow() and getBlackMatrix() at the top of this file.
   *
   * TIP: Replace calls to isBlack() with a single call to getBlackMatrix(), then call
   * BitMatrix.get(x, y) per pixel.
   */
  public boolean isBlack(int x, int y) throws ReaderException {
    if (matrix == null) {
      matrix = binarizer.getBlackMatrix();
    }
    return matrix.get(x, y);
  }

  /**
   * @deprecated
   *
   * FIXME: REMOVE!
   *
   * TIP: 2D Readers should replace uses of this method with a single call to getBlackMatrix(),
   * then perform random access on that BitMatrix as needed. The version of getBlackRow() with
   * two arguments is only meant for 1D Readers, which I've already converted.
   */
  public BitArray getBlackRow(int y, BitArray row, int startX, int getWidth)
      throws ReaderException {
    if (row == null || row.getSize() < getWidth) {
      row = new BitArray(getWidth);
    } else {
      row.clear();
    }

    if (matrix == null) {
      matrix = binarizer.getBlackMatrix();
    }
    for (int x = 0; x < getWidth; x++) {
      if (matrix.get(startX + x, y)) {
        row.set(x);
      }
    }
    return row;
  }

  /**
   * @deprecated
   *
   * FIXME: REMOVE!
   *
   * TIP: Replace calls to getBlackColumn() with a single call to getBlackMatrix(), then
   * perform random access on that BitMatrix as needed.
   */
  public BitArray getBlackColumn(int x, BitArray column, int startY, int getHeight)
      throws ReaderException {
    if (column == null || column.getSize() < getHeight) {
      column = new BitArray(getHeight);
    } else {
      column.clear();
    }

    if (matrix == null) {
      matrix = binarizer.getBlackMatrix();
    }
    for (int y = 0; y < getHeight; y++) {
      if (matrix.get(x, startY + y)) {
        column.set(y);
      }
    }
    return column;
  }

}
