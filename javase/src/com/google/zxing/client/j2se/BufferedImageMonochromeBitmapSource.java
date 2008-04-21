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
import com.google.zxing.ReaderException;
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
 * <p>This may also be used to construct a {@link MonochromeBitmapSource}
 * based on a region of a {@link BufferedImage}; see
 * {@link #BufferedImageMonochromeBitmapSource(BufferedImage, int, int, int, int)}.</p>
 *
 * @author srowen@google.com (Sean Owen), Daniel Switkin (dswitkin@google.com)
 */
public final class BufferedImageMonochromeBitmapSource implements MonochromeBitmapSource {

  private final BufferedImage image;
  private final int left;
  private final int top;
  private final int width;
  private final int height;
  private int blackPoint;
  private BlackPointEstimationMethod lastMethod;
  private int lastArgument;

  private static final int LUMINANCE_BITS = 5;
  private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
  private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;

  /**
   * Creates an instance that uses the entire given image as a source of pixels to decode.
   *
   * @param image image to decode
   */
  public BufferedImageMonochromeBitmapSource(BufferedImage image) {
    this(image, 0, 0, image.getWidth(), image.getHeight());
  }

  /**
   * Creates an instance that uses only a region of the given image as a source of pixels to decode.
   *
   * @param image image to decode a region of
   * @param left x coordinate of leftmost pixels to decode
   * @param top y coordinate of topmost pixels to decode
   * @param right one more than the x coordinate of rightmost pixels to decode. That is, we will decode
   *  pixels whose x coordinate is in [left,right)
   * @param bottom likewise, one more than the y coordinate of the bottommost pixels to decode
   */
  public BufferedImageMonochromeBitmapSource(BufferedImage image, int left, int top, int right, int bottom) {
    this.image = image;
    blackPoint = 0x7F;
    lastMethod = null;
    lastArgument = 0;
    int sourceHeight = image.getHeight();
    int sourceWidth = image.getWidth();
    if (left < 0 || top < 0 || right > sourceWidth || bottom > sourceHeight || right <= left || bottom <= top) {
      throw new IllegalArgumentException("Invalid bounds: (" + top + ',' + left + ") (" + right + ',' + bottom + ')');
    }
    this.left = left;
    this.top = top;
    this.width = right - left;
    this.height = bottom - top;
  }

  /**
   * @return underlying {@link BufferedImage} behind this instance. Note that even if this instance
   *  only uses a subset of the full image, the returned value here represents the entire backing image.
   */
  public BufferedImage getImage() {
    return image;
  }

  private int getRGB(int x, int y) {
    return image.getRGB(left + x, top + y);
  }

  private void getRGBRow(int startX, int startY, int[] result) {
    image.getRGB(left + startX, top + startY, result.length, 1, result, 0, result.length);
  }

  public boolean isBlack(int x, int y) {
    return computeRGBLuminance(getRGB(x, y)) < blackPoint;
  }

  public BitArray getBlackRow(int y, BitArray row, int startX, int getWidth) {
    if (row == null || row.getSize() < getWidth) {
      row = new BitArray(getWidth);
    } else {
      row.clear();
    }
    int[] pixelRow = new int[getWidth];
    getRGBRow(startX, y, pixelRow);

    // If the current decoder calculated the blackPoint based on one row, assume we're trying to
    // decode a 1D barcode, and apply some sharpening.
    // TODO: We may want to add a fifth parameter to request the amount of shapening to be done.
    if (lastMethod == BlackPointEstimationMethod.ROW_SAMPLING) {
      int left = computeRGBLuminance(pixelRow[0]);
      int center = computeRGBLuminance(pixelRow[1]);
      for (int i = 1; i < getWidth - 1; i++) {
        int right = computeRGBLuminance(pixelRow[i + 1]);
        // Simple -1 4 -1 box filter with a weight of 2
        int luminance = ((center << 2) - left - right) >> 1;
        if (luminance < blackPoint) {
          row.set(i);
        }
        left = center;
        center = right;
      }
    } else {
      for (int i = 0; i < getWidth; i++) {
        if (computeRGBLuminance(pixelRow[i]) < blackPoint) {
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
        int startI = height == minDimension ? 0 : (height - width) >> 1;
        int startJ = width == minDimension ? 0 : (width - height) >> 1;
        for (int n = 0; n < minDimension; n++) {
          int pixel = getRGB(startJ + n, startI + n);
          histogram[computeRGBLuminance(pixel) >> LUMINANCE_SHIFT]++;
        }
      } else if (method.equals(BlackPointEstimationMethod.ROW_SAMPLING)) {
        if (argument < 0 || argument >= height) {
          throw new IllegalArgumentException("Row is not within the image: " + argument);
        }
        int[] rgbArray = new int[width];
        getRGBRow(0, argument, rgbArray);
        for (int x = 0; x < width; x++) {
          histogram[computeRGBLuminance(rgbArray[x]) >> LUMINANCE_SHIFT]++;
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
    if (!isRotateSupported()) {
      throw new IllegalStateException("Rotate not supported");
    }
    int sourceWidth = image.getWidth();
    int sourceHeight = image.getHeight();
    // 90 degrees counterclockwise:
    AffineTransform transform = new AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, sourceWidth);
    BufferedImageOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    // Note width/height are flipped since we are rotating 90 degrees:
    BufferedImage rotatedImage = new BufferedImage(sourceHeight, sourceWidth, image.getType());
    op.filter(image, rotatedImage);
    return new BufferedImageMonochromeBitmapSource(rotatedImage,
                                                   top,
                                                   sourceWidth - (left + width),
                                                   top + height,
                                                   sourceWidth - left);
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
