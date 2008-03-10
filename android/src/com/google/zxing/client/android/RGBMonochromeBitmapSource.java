/*
 * Copyright (C) 2008 Google Inc.
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

package com.google.zxing.client.android;

import android.graphics.Bitmap;
import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BlackPointEstimator;

/**
 * This object implements MonochromeBitmapSource around an Android Bitmap. Rather than capturing an
 * RGB image and calculating the grey value at each pixel, we ask the camera driver for YUV data and
 * strip out the luminance channel directly. This should be faster but provides fewer bits, i.e.
 * fewer grey levels.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author srowen@google.com (Sean Owen)
 */
final class RGBMonochromeBitmapSource implements MonochromeBitmapSource {

  private final Bitmap image;
  private int blackPoint;
  private BlackPointEstimationMethod lastMethod;
  private int lastArgument;

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

  RGBMonochromeBitmapSource(Bitmap image) {
    this.image = image;
    blackPoint = 0x7F;
    lastMethod = null;
    lastArgument = 0;
  }

  public boolean isBlack(int x, int y) {
    return computeRGBLuminance(image.getPixel(x, y)) < blackPoint;
  }

  public BitArray getBlackRow(int y, BitArray row, int startX, int getWidth) {
    if (row == null) {
      row = new BitArray(getWidth);
    } else {
      row.clear();
    }
    int[] pixelRow = new int[getWidth];
     image.getPixels(pixelRow, 0, getWidth, startX, y, getWidth, 1);
    for (int i = 0; i < getWidth; i++) {
      if (computeRGBLuminance(pixelRow[i]) < blackPoint) {
        row.set(i);
      }
    }
    return row;
  }

  public int getHeight() {
    return image.height();
  }

  public int getWidth() {
    return image.width();
  }

  public void estimateBlackPoint(BlackPointEstimationMethod method, int argument) {
    if (!method.equals(lastMethod) || argument != lastArgument) {
      int width = image.width();
      int height = image.height();
      int[] histogram = new int[LUMINANCE_BUCKETS];
      float biasTowardsWhite = 1.0f;
      if (method.equals(BlackPointEstimationMethod.TWO_D_SAMPLING)) {
        int minDimension = width < height ? width : height;
        int startI = height == minDimension ? 0 : (height - width) >> 1;
        int startJ = width == minDimension ? 0 : (width - height) >> 1;
        for (int n = 0; n < minDimension; n++) {
          int pixel = image.getPixel(startJ + n, startI + n);
          histogram[computeRGBLuminance(pixel) >> LUMINANCE_SHIFT]++;
        }
      } else if (method.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
        if (argument < 0 || argument >= height) {
          throw new IllegalArgumentException("Row is not within the image: " + argument);
        }
        biasTowardsWhite = 2.0f;
        int[] pixelRow = new int[width];
        image.getPixels(pixelRow, 0, width, 0, argument, width, 1);
        for (int x = 0; x < width; x++) {
          histogram[computeRGBLuminance(pixelRow[x]) >> LUMINANCE_SHIFT]++;
        }
      } else {
        throw new IllegalArgumentException("Unknown method: " + method);
      }
      blackPoint = BlackPointEstimator.estimate(histogram, biasTowardsWhite) << LUMINANCE_SHIFT;
      lastMethod = method;
      lastArgument = argument;
    }
  }

  public BlackPointEstimationMethod getLastEstimationMethod() {
    return lastMethod;
  }

  public MonochromeBitmapSource rotateCounterClockwise() {
    throw new IllegalStateException("Rotate not supported");
  }

  public boolean isRotateSupported() {
    return false;
  }

  /**
   * An optimized approximation of a more proper conversion from RGB to luminance which
   * only uses shifts. See BufferedImageMonochromeBitmapSource for an original version.
   */
  private static int computeRGBLuminance(int pixel) {
    // Instead of multiplying by 306, 601, 117, we multiply by 256, 512, 256, so that
    // the multiplies can be implemented as shifts.
    //
    // Really, it's:
    //
    // return ((((pixel >> 16) & 0xFF) << 8) +
    //         (((pixel >>  8) & 0xFF) << 9) +
    //         (( pixel        & 0xFF) << 8)) >> 10;
    //
    // That is, we're replacing the coefficients in the original with powers of two,
    // which can be implemented as shifts, even though changing the coefficients slightly
    // corrupts the conversion. Not significant for our purposes.
    //
    // But we can get even cleverer and eliminate a few shifts:
    return (((pixel & 0x00FF0000) >> 8)  +
            ((pixel & 0x0000FF00) << 1) +
            ((pixel & 0x000000FF) << 8)) >> 10;
  }

}