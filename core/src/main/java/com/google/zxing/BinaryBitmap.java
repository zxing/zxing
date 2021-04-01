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
  }

  /**
   * @return The width of the bitmap.
   */
  public int getWidth() {
    return binarizer.getWidth();
  }

  /**
   * @return The height of the bitmap.
   */
  public int getHeight() {
    return binarizer.getHeight();
  }

  /**
   * Converts one row of luminance data to 1 bit data. May actually do the conversion, or return
   * cached data. Callers should assume this method is expensive and call it as seldom as possible.
   * This method is intended for decoding 1D barcodes and may choose to apply sharpening.
   *
   * @param y The row to fetch, which must be in [0, bitmap height)
   * @param row An optional preallocated array. If null or too small, it will be ignored.
   *            If used, the Binarizer will call BitArray.clear(). Always use the returned object.
   * @return The array of bits for this row (true means black).
   * @throws NotFoundException if row can't be binarized
   */
  public BitArray getBlackRow(int y, BitArray row) throws NotFoundException {
    return binarizer.getBlackRow(y, row);
  }

  /**
   * Converts a 2D array of luminance data to 1 bit. As above, assume this method is expensive
   * and do not call it repeatedly. This method is intended for decoding 2D barcodes and may or
   * may not apply sharpening. Therefore, a row from this matrix may not be identical to one
   * fetched using getBlackRow(), so don't mix and match between them.
   *
   * @return The 2D array of bits for the image (true means black).
   * @throws NotFoundException if image can't be binarized to make a matrix
   */
  public BitMatrix getBlackMatrix() throws NotFoundException {
    // The matrix is created on demand the first time it is requested, then cached. There are two
    // reasons for this:
    // 1. This work will never be done if the caller only installs 1D Reader objects, or if a
    //    1D Reader finds a barcode before the 2D Readers run.
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
   * @param left The left coordinate, which must be in [0,getWidth())
   * @param top The top coordinate, which must be in [0,getHeight())
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
   * Returns a new object with rotated image data by 90 degrees counterclockwise.
   * Only callable if {@link #isRotateSupported()} is true.
   *
   * @return A rotated version of this object.
   */
  public BinaryBitmap rotateCounterClockwise() {
    LuminanceSource newSource = binarizer.getLuminanceSource().rotateCounterClockwise();
    return new BinaryBitmap(binarizer.createBinarizer(newSource));
  }

  /**
   * Returns a new object with rotated image data by 45 degrees counterclockwise.
   * Only callable if {@link #isRotateSupported()} is true.
   *
   * @return A rotated version of this object.
   */
  public BinaryBitmap rotateCounterClockwise45() {
    LuminanceSource newSource = binarizer.getLuminanceSource().rotateCounterClockwise45();
    return new BinaryBitmap(binarizer.createBinarizer(newSource));
  }

  @Override
  public String toString() {
    try {
      return getBlackMatrix().toString();
    } catch (NotFoundException e) {
      return "";
    }
  }

}
