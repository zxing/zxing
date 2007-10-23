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

package com.google.zxing.client.j2se;

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BlackPointEstimator;

import java.awt.image.BufferedImage;

/**
 * <p>An implementation based upon {@link BufferedImage}. This provides access to the
 * underlying image as if it were a monochrome image. Behind the scenes, it is evaluating
 * the luminance of the underlying image by retrieving its pixels' RGB values.</p>
 * 
 * @author srowen@google.com (Sean Owen)
 */
public final class BufferedImageMonochromeBitmapSource implements MonochromeBitmapSource {

  private final BufferedImage image;
  private final int blackPoint;

  public BufferedImageMonochromeBitmapSource(BufferedImage image) {
    this.image = image;
    int width = image.getWidth();
    int height = image.getHeight();
    int[] luminanceBuckets = new int[32];
    int minDimension = width < height ? width : height;
    int startI = height == minDimension ? 0 : (height - width) >> 1;
    int startJ = width == minDimension ? 0 : (width - height) >> 1;
    for (int n = 0; n < minDimension; n++) {
      int pixel = image.getRGB(startJ + n, startI + n);
      luminanceBuckets[computeRGBLuminance(pixel) >> 3]++;
    }
    blackPoint = BlackPointEstimator.estimate(luminanceBuckets) << 3;
  }

  public boolean isBlack(int x, int y) {
    return computeRGBLuminance(image.getRGB(x, y)) < blackPoint;
  }

  public BitArray getBlackRow(int y, BitArray row, int startX, int getWidth) {
    if (row == null) {
      row = new BitArray(getWidth);
    } else {
      row.clear();
    }
    int[] pixelRow = image.getRGB(startX, y, getWidth, 1, null, 0, getWidth);
    for (int i = 0; i < getWidth; i++) {
      if (computeRGBLuminance(pixelRow[i]) < blackPoint) {
        row.set(i);
      }
    }
    return row;
  }

  public int getHeight() {
    return image.getHeight();
  }

  public int getWidth() {
    return image.getWidth();
  }

  /**
   * Extracts luminance from a pixel from this source. By default, the source is assumed to use RGB,
   * so this implementation computes luminance is a function of a red, green and blue components as
   * follows:
   *
   * <code>Y = 0.299R + 0.587G + 0.114B</code>
   *
   * where R, G, and B are values in [0,1].
   */
  private static int computeRGBLuminance(int pixel) {
    // Coefficients add up to 1024 to make the divide into a fast shift
    return (306 * ((pixel >> 16) & 0xFF) +
        601 * ((pixel >> 8) & 0xFF) +
        117 * (pixel & 0xFF)) >> 10;
  }

}
