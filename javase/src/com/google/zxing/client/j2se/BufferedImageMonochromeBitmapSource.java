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

import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BlackPointEstimator;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

/**
 * <p>An implementation based upon {@link BufferedImage}. This provides access to the
 * underlying image as if it were a monochrome image. Behind the scenes, it is evaluating
 * the luminance of the underlying image by retrieving its pixels' RGB values.</p>
 *
 * @author srowen@google.com (Sean Owen), Daniel Switkin (dswitkin@google.com)
 */
public final class BufferedImageMonochromeBitmapSource implements MonochromeBitmapSource {

  private final BufferedImage image;
  private int blackPoint;
  private BlackPointEstimationMethod lastMethod;
  private int lastArgument;

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

  public BufferedImageMonochromeBitmapSource(BufferedImage image) {
    this.image = image;
    blackPoint = 0x7F;
    lastMethod = null;
    lastArgument = 0;
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

  public void estimateBlackPoint(BlackPointEstimationMethod method, int argument) {
    if (!method.equals(lastMethod) || argument != lastArgument) {
      int width = image.getWidth();
      int height = image.getHeight();
      int[] histogram = new int[LUMINANCE_BUCKETS];
      float biasTowardsWhite = 1.0f;
      if (method.equals(BlackPointEstimationMethod.TWO_D_SAMPLING)) {
        int minDimension = width < height ? width : height;
        int startI = height == minDimension ? 0 : (height - width) >> 1;
        int startJ = width == minDimension ? 0 : (width - height) >> 1;
        for (int n = 0; n < minDimension; n++) {
          int pixel = image.getRGB(startJ + n, startI + n);
          histogram[computeRGBLuminance(pixel) >> LUMINANCE_SHIFT]++;
        }
      } else if (method.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
        if (argument < 0 || argument >= height) {
          throw new IllegalArgumentException("Row is not within the image: " + argument);
        }
        biasTowardsWhite = 2.0f;
        int[] rgbArray = new int[width];
        image.getRGB(0, argument, width, 1, rgbArray, 0, width);
        for (int x = 0; x < width; x++) {
          histogram[computeRGBLuminance(rgbArray[x]) >> LUMINANCE_SHIFT]++;
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
    if (!isRotateSupported()) {
      throw new IllegalStateException("Rotate not supported");
    }
    // 90 degrees counterclockwise:
    AffineTransform transform = new AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, image.getHeight());
    BufferedImageOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    BufferedImage rotatedImage = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
    op.filter(image, rotatedImage);
    return new BufferedImageMonochromeBitmapSource(rotatedImage);
  }

  public boolean isRotateSupported() {
    // Can't run AffineTransforms on images of unknown format
    return image.getType() != BufferedImage.TYPE_CUSTOM;
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
