/*
 * Copyright 2008 ZXing authors
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

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.ReaderException;

/**
 * @author dswitkin@google.com (Daniel Switkin)
 */
public abstract class BaseMonochromeBitmapSource implements MonochromeBitmapSource {

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

  private final int height;
  private final int width;
  private int blackPoint;
  private BlackPointEstimationMethod lastMethod;
  private int lastArgument;
  private int[] luminances;

  protected BaseMonochromeBitmapSource(int width, int height) {
    this.height = height;
    this.width = width;
    blackPoint = 0x7F;
    lastMethod = null;
    lastArgument = 0;
  }

  private void initLuminances() {
    if (luminances == null) {
      int max = width > height ? width : height;
      luminances = new int[max];
    }
  }

  public boolean isBlack(int x, int y) {
    return getLuminance(x, y) < blackPoint;
  }

  public BitArray getBlackRow(int y, BitArray row, int startX, int getWidth) {
    if (row == null || row.getSize() < getWidth) {
      row = new BitArray(getWidth);
    } else {
      row.clear();
    }

    // Reuse the same int array each time
    initLuminances();
    luminances = getLuminanceRow(y, luminances);

    // If the current decoder calculated the blackPoint based on one row, assume we're trying to
    // decode a 1D barcode, and apply some sharpening.
    if (lastMethod.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
      int left = luminances[startX];
      int center = luminances[startX + 1];
      for (int x = 1; x < getWidth - 1; x++) {
        int right = luminances[startX + x + 1];
        // Simple -1 4 -1 box filter with a weight of 2
        int luminance = ((center << 2) - left - right) >> 1;
        if (luminance < blackPoint) {
          row.set(x);
        }
        left = center;
        center = right;
      }
    } else {
      for (int x = 0; x < getWidth; x++) {
        if (luminances[startX + x] < blackPoint) {
          row.set(x);
        }
      }
    }
    return row;
  }

  public BitArray getBlackColumn(int x, BitArray column, int startY, int getHeight) {
    if (column == null || column.getSize() < getHeight) {
      column = new BitArray(getHeight);
    } else {
      column.clear();
    }

    // Reuse the same int array each time
    initLuminances();
    luminances = getLuminanceColumn(x, luminances);

    // We don't handle "row sampling" specially here
    for (int y = 0; y < getHeight; y++) {
      if (luminances[startY + y] < blackPoint) {
        column.set(y);
      }
    }
    return column;
  }

  public void estimateBlackPoint(BlackPointEstimationMethod method, int argument) throws ReaderException {
    if (!method.equals(lastMethod) || argument != lastArgument) {
      int width = getWidth();
      int height = getHeight();
      int[] histogram = new int[LUMINANCE_BUCKETS];
      if (method.equals(BlackPointEstimationMethod.TWO_D_SAMPLING)) {
        int minDimension = width < height ? width : height;
        int startX = (width - minDimension) >> 1;
        int startY = (height - minDimension) >> 1;
        for (int n = 0; n < minDimension; n++) {
          int luminance = getLuminance(startX + n, startY + n);
          histogram[luminance >> LUMINANCE_SHIFT]++;
        }
      } else if (method.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
        if (argument < 0 || argument >= height) {
          throw new IllegalArgumentException("Row is not within the image: " + argument);
        }
        initLuminances();
        luminances = getLuminanceRow(argument, luminances);
        for (int x = 0; x < width; x++) {
          histogram[luminances[x] >> LUMINANCE_SHIFT]++;
        }
      } else {
        throw new IllegalArgumentException("Unknown method");
      }
      blackPoint = BlackPointEstimator.estimate(histogram) << LUMINANCE_SHIFT;
      lastMethod = method;
      lastArgument = argument;
    }
  }

  public BlackPointEstimationMethod getLastEstimationMethod() {
    return lastMethod;
  }

  public MonochromeBitmapSource rotateCounterClockwise() {
    throw new IllegalArgumentException("Rotate not supported");
  }

  public boolean isRotateSupported() {
    return false;
  }

  public final int getHeight() {
    return height;
  }

  public final int getWidth() {
    return width;
  }

  // These methods below should not need to exist because they are defined in the interface that
  // this abstract class implements. However this seems to cause problems on some Nokias.
  // So we write these redundant declarations.

  /**
   * Retrieves the luminance at the pixel x,y in the bitmap. This method is only used for estimating
   * the black point and implementing getBlackRow() - it is not meant for decoding, hence it is not
   * part of MonochromeBitmapSource itself, and is protected.
   *
   * @param x The x coordinate in the image.
   * @param y The y coordinate in the image.
   * @return The luminance value between 0 and 255.
   */
  protected abstract int getLuminance(int x, int y);

  /**
   * This is the main mechanism for retrieving luminance data. It is dramatically more efficient
   * than repeatedly calling getLuminance(). As above, this is not meant for decoders.
   *
   * @param y The row to fetch
   * @param row The array to write luminance values into. It is <b>strongly</b> suggested that you
   *            allocate this yourself, making sure row.length >= getWidth(), and reuse the same
   *            array on subsequent calls for performance. If you pass null, you will be flogged,
   *            but then I will take pity on you and allocate a sufficient array internally.
   * @return The array containing the luminance data. This is the same as row if it was usable.
   */
  protected abstract int[] getLuminanceRow(int y, int[] row);

  /**
   * The same as getLuminanceRow(), but for columns.
   *
   * @param x The column to fetch
   * @param column The array to write luminance values into. See above.
   * @return The array containing the luminance data.
   */
  protected abstract int[] getLuminanceColumn(int x, int[] column);

}
