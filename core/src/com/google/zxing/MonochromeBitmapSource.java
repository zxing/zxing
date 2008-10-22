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

package com.google.zxing;

import com.google.zxing.common.BitArray;

/**
 * <p>Encapsulates a generic black-and-white bitmap -- a collection of pixels in two dimensions.
 * This unifies many possible representations, like AWT's <code>BufferedImage</code>.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public interface MonochromeBitmapSource {

  /**
   * @param x horizontal offset, from left, of the pixel
   * @param y vertical offset, from top, of the pixel
   * @return true iff the pixel at (x,y) is black
   */
  boolean isBlack(int x, int y);

  /**
   * <p>Returns an entire row of black/white pixels as an array of bits, where "true" means "black".
   * This is a sort of "bulk get" operation intended to enable efficient access in
   * certain situations.</p>
   *
   * @param y vertical offset, from top, of the row of pixels
   * @param row if not null, {@link BitArray} to write pixels into. If null, a new {@link BitArray}
   * is allocated and returned.
   * @param startX horizontal offset, from left, from which to start getting pixels
   * @param getWidth number of pixels to get from the row
   * @return {@link BitArray} representing the (subset of the) row of pixels. If row parameter
   *         was not null, it is returned.
   */
  BitArray getBlackRow(int y, BitArray row, int startX, int getWidth);

  /**
   * Entirely analogous to {@link #getBlackRow(int, BitArray, int, int)} but gets a column.
   */
  BitArray getBlackColumn(int x, BitArray column, int startY, int getHeight);

  /**
   * @return height of underlying image
   */
  int getHeight();

  /**
   * @return width of underlying image
   */
  int getWidth();

  /**
   * Retrieves the luminance at the pixel x,y in the bitmap. This method is only used for estimating
   * the black point and implementing getBlackRow() - it is not meant for decoding.
   *
   * @param x The x coordinate in the image.
   * @param y The y coordinate in the image.
   * @return The luminance value between 0 and 255.
   */
  int getLuminance(int x, int y);

  /**
   * Some implementations can be much more efficient by fetching an entire row of luminance data at
   * a time. This method should be called once per row before calling getLuminance().
   *
   * @param y The row to cache.
   */
  void cacheRowForLuminance(int y);

  /**
   * Entirely analogous to {@link #cacheRowForLuminance(int)} but caches a column.
   */
  void cacheColumnForLuminance(int x);

  /**
   * <p>Estimates black point according to the given method, which is optionally parameterized by
   * a single int argument. For {@link BlackPointEstimationMethod#ROW_SAMPLING}, this
   * specifies the row to sample.</p>
   *
   * <p>The estimated value will be used in subsequent computations that rely on an estimated black
   * point.</p>
   *
   * @param method black point estimation method
   * @param argument method-specific argument
   */
  void estimateBlackPoint(BlackPointEstimationMethod method, int argument) throws ReaderException;

  /**
   * @return {@link BlackPointEstimationMethod} representing last sampling method used
   */
  BlackPointEstimationMethod getLastEstimationMethod();

  /**
   * <p>Optional operation which returns an implementation based on the same underlying
   * image, but which behaves as if the underlying image had been rotated 90 degrees
   * counterclockwise. This is useful in the context of 1D barcodes and the
   * {@link DecodeHintType#TRY_HARDER} decode hint, and is only intended to be
   * used in non-resource-constrained environments. Hence, implementations
   * of this class which are only used in resource-constrained mobile environments
   * don't have a need to implement this.</p>
   *
   * @throws IllegalArgumentException if not supported
   */
  MonochromeBitmapSource rotateCounterClockwise();

  /**
   * @return true iff rotation is supported
   * @see #rotateCounterClockwise()
   */
  boolean isRotateSupported();

}
