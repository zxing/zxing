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

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.BlackPointEstimationMethod;
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
    if (row == null) {
      row = new BitArray(getWidth);
    } else {
      row.clear();
    }
    for (int i = 0, offset = y * width + startX; i < getWidth; i++, offset++) {
      if (computeRGBLuminance(rgbPixels[offset]) < blackPoint) {
        row.set(i);
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

  public void estimateBlackPoint(BlackPointEstimationMethod method, int argument) {
    if (!method.equals(lastMethod) || argument != lastArgument) {
      int[] histogram = new int[LUMINANCE_BUCKETS];
	    float biasTowardsWhite = 1.0f;
      if (method.equals(BlackPointEstimationMethod.TWO_D_SAMPLING)) {
        int minDimension = width < height ? width : height;
        for (int n = 0, offset = 0; n < minDimension; n++, offset += width + 1) {
          histogram[computeRGBLuminance(rgbPixels[offset]) >> LUMINANCE_SHIFT]++;
        }
      } else if (method.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
        if (argument < 0 || argument >= height) {
          throw new IllegalArgumentException("Row is not within the image: " + argument);
        }
	      biasTowardsWhite = 2.0f;
        int offset = argument * width;
        for (int x = 0; x < width; x++) {
          histogram[computeRGBLuminance(rgbPixels[offset + x]) >> LUMINANCE_SHIFT]++;
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