/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.client.j2me;

import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BlackPointEstimator;

import javax.microedition.lcdui.Image;

/**
 * <p>An implementation based on Java ME's {@link Image} representation.</p>
 *
 * @author Sean Owen (srowen@google.com), Daniel Switkin (dswitkin@google.com)
 */
public final class LCDUIImageMonochromeBitmapSource implements MonochromeBitmapSource {

  private final int[] rgbPixels;
  private final int width;
  private final int height;
  private int blackPoint;
  private BlackPointEstimationMethod lastMethod;
  private int lastArgument;

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

  public LCDUIImageMonochromeBitmapSource(Image image) {
    width = image.getWidth();
    height = image.getHeight();
    rgbPixels = new int[width * height];
    image.getRGB(rgbPixels, 0, width, 0, 0, width, height);
    blackPoint = 0x7F;
    lastMethod = null;
    lastArgument = 0;
  }

  public boolean isBlack(int x, int y) {
    return computeRGBLuminance(rgbPixels[x + y * width]) < blackPoint;
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
    if (lastMethod.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
      int offset = y * width + startX;
      int left = computeRGBLuminance(rgbPixels[offset]);
      offset++;
      int center = computeRGBLuminance(rgbPixels[offset]);
      for (int i = 1; i < getWidth - 1; i++, offset++) {
        int right = computeRGBLuminance(rgbPixels[offset + 1]);
        // Simple -1 4 -1 box filter with a weight of 2
        int luminance = ((center << 2) - left - right) >> 1;
        if (luminance < blackPoint) {
          row.set(i);
        }
        left = center;
        center = right;
      }
    } else {
      for (int i = 0, offset = y * width + startX; i < getWidth; i++, offset++) {
        if (computeRGBLuminance(rgbPixels[offset]) < blackPoint) {
          row.set(i);
        }
      }
    }
    return row;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public void estimateBlackPoint(BlackPointEstimationMethod method, int argument) throws ReaderException {
    if (!method.equals(lastMethod) || argument != lastArgument) {
      int[] histogram = new int[LUMINANCE_BUCKETS];
      if (method.equals(BlackPointEstimationMethod.TWO_D_SAMPLING)) {
        int minDimension = width < height ? width : height;
        for (int n = 0, offset = 0; n < minDimension; n++, offset += width + 1) {
          histogram[computeRGBLuminance(rgbPixels[offset]) >> LUMINANCE_SHIFT]++;
        }
      } else if (method.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
        if (argument < 0 || argument >= height) {
          throw new IllegalArgumentException("Row is not within the image: " + argument);
        }
        int offset = argument * width;
        for (int x = 0; x < width; x++) {
          histogram[computeRGBLuminance(rgbPixels[offset + x]) >> LUMINANCE_SHIFT]++;
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
    return (((pixel & 0x00FF0000) >> 16)  +
            ((pixel & 0x0000FF00) >>  7) +
            ( pixel & 0x000000FF       )) >> 2;
  }

}