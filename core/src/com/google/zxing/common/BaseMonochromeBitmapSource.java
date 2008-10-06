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

  private int blackPoint;
  private BlackPointEstimationMethod lastMethod;
  private int lastArgument;

  protected BaseMonochromeBitmapSource() {
    blackPoint = 0x7F;
    lastMethod = null;
    lastArgument = 0;
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

    // If the current decoder calculated the blackPoint based on one row, assume we're trying to
    // decode a 1D barcode, and apply some sharpening.
    // TODO: We may want to add a fifth parameter to request the amount of shapening to be done.
    cacheRowForLuminance(y);
    if (lastMethod.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
      int left = getLuminance(startX, y);
      int center = getLuminance(startX + 1, y);
      for (int x = 1; x < getWidth - 1; x++) {
        int right = getLuminance(startX + x + 1, y);
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
        if (getLuminance(startX + x, y) < blackPoint) {
          row.set(x);
        }
      }
    }
    return row;
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
        cacheRowForLuminance(argument);
        for (int x = 0; x < width; x++) {
          int luminance = getLuminance(x, argument);
          histogram[luminance >> LUMINANCE_SHIFT]++;
        }
      } else {
        throw new IllegalArgumentException("Unknown method: " + method);
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

  // These two methods should not need to exist because they are defined in the interface that
  // this abstract class implements. However this seems to cause problems on some Nokias. 
  // So we write these redundant declarations.

  public abstract int getHeight();

  public abstract int getWidth();

  public abstract int getLuminance(int x, int y);

  public abstract void cacheRowForLuminance(int y);

}
