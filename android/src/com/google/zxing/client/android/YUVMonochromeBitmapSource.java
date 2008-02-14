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
final class YUVMonochromeBitmapSource implements MonochromeBitmapSource {

  private final Bitmap image;
  private final BitArray[] blackWhitePixels;
  private final int width;
  private final int height;
  private int blackPoint;
  private BlackPointEstimationMethod lastMethod;
  private int lastArgument;

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

  YUVMonochromeBitmapSource(Bitmap image) {
    width = image.width();
    height = image.height();
    this.image = image;
    blackWhitePixels = new BitArray[height];
    blackPoint = 0x7F;
    lastMethod = null;
    lastArgument = 0;
  }

  public boolean isBlack(int x, int y) {
    BitArray blackWhite = blackWhitePixels[y];
    if (blackWhite == null) {
      blackWhite = parseBlackWhite(y);
    }
    return blackWhite.get(x);
  }

  public BitArray getBlackRow(int y, BitArray row, int startX, int getWidth) {
    BitArray blackWhite = blackWhitePixels[y];
    if (blackWhite == null) {
      blackWhite = parseBlackWhite(y);
    }
    if (row == null) {
      if (startX == 0 && getWidth == width) {
        return blackWhite;
      }
      row = new BitArray(getWidth);
    } else {
      row.clear();
    }
    for (int i = 0; i < getWidth; i++) {
      if (blackWhite.get(startX + i)) {
        row.set(i);
      }
    }
    return row;
  }

  private BitArray parseBlackWhite(int y) {
    int width = this.width;
    int[] pixelRow = new int[width];
    image.getPixels(pixelRow, 0, width, 0, y, width, 1);
    BitArray luminanceRow = new BitArray(width);
    int blackPoint = this.blackPoint;
    // Calculate 32 bits at a time to more efficiently set the bit array
    int bits = 0;
    int bitCount = 0;
    for (int j = 0; j < width; j++) {
      bits >>>= 1;
      // Computation of luminance is inlined here for speed:      
      if (((pixelRow[j] >> 16) & 0xFF) <= blackPoint) {
        bits |= 0x80000000;
      }
      if (++bitCount == 32) {
        luminanceRow.setBulk(j, bits);
        bits = 0;
        bitCount = 0;
      }
    }
    if (bitCount > 0) {
      luminanceRow.setBulk(width, bits >>> (32 - bitCount));
    }
    blackWhitePixels[y] = luminanceRow;
    return luminanceRow;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public void estimateBlackPoint(BlackPointEstimationMethod method, int argument) {
    if (!method.equals(lastMethod) || argument != lastArgument) {
      for (int i = 0; i < blackWhitePixels.length; i++) {
        blackWhitePixels[i] = null;
      }
      int[] histogram = new int[LUMINANCE_BUCKETS];
      if (method.equals(BlackPointEstimationMethod.TWO_D_SAMPLING)) {
        int minDimension = width < height ? width : height;
        int startI = height == minDimension ? 0 : (height - width) >> 1;
        int startJ = width == minDimension ? 0 : (width - height) >> 1;
        for (int n = 0; n < minDimension; n++) {
          int pixel = image.getPixel(startJ + n, startI + n);
          // Computation of luminance is inlined here for speed:
          histogram[((pixel >> 16) & 0xFF) >> LUMINANCE_SHIFT]++;
        }
      } else if (method.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
        if (argument < 0 || argument >= height) {
          throw new IllegalArgumentException("Row is not within the image: " + argument);
        }
        int[] yuvArray = new int[width];
        image.getPixels(yuvArray, 0, width, 0, argument, width, 1);
        for (int x = 0; x < width; x++) {
          histogram[((yuvArray[x] >> 16) & 0xFF) >> LUMINANCE_SHIFT]++;
        }
      } else {
        throw new IllegalArgumentException("Unknown method: " + method);
      }
      blackPoint = BlackPointEstimator.estimate(histogram, 1.0f) << LUMINANCE_SHIFT;
      lastMethod = method;
      lastArgument = argument;
    }
  }

  public BlackPointEstimationMethod getLastEstimationMethod() {
    return lastMethod;
  }

}