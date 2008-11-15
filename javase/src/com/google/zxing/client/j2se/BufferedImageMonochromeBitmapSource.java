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

package com.google.zxing.client.j2se;

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.common.BaseMonochromeBitmapSource;

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
 * @author Sean Owen
 * @author Daniel Switkin (dswitkin@google.com)
 */
public final class BufferedImageMonochromeBitmapSource extends BaseMonochromeBitmapSource {

  private final BufferedImage image;
  private final int left;
  private final int top;
  private final int width;
  private final int height;

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

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  @Override
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

  @Override
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
  protected int getLuminance(int x, int y) {
    int pixel = image.getRGB(left + x, top + y);
    // Coefficients add up to 1024 to make the divide into a fast shift
    return (306 * ((pixel >> 16) & 0xFF) +
        601 * ((pixel >> 8) & 0xFF) +
        117 * (pixel & 0xFF)) >> 10;
  }

  protected int[] getLuminanceRow(int y, int[] row) {
    if (row == null || row.length < width) {
      row = new int[width];
    }
    image.getRGB(left, top + y, width, 1, row, 0, width);
    for (int x = 0; x < width; x++) {
      int pixel = row[x];
      row[x] = (306 * ((pixel >> 16) & 0xFF) +
          601 * ((pixel >> 8) & 0xFF) +
          117 * (pixel & 0xFF)) >> 10;
    }
    return row;
  }

  protected int[] getLuminanceColumn(int x, int[] column) {
    if (column == null || column.length < height) {
      column = new int[height];
    }
    image.getRGB(left + x, top, 1, height, column, 0, 1);
    for (int y = 0; y < height; y++) {
      int pixel = column[y];
      column[y] = (306 * ((pixel >> 16) & 0xFF) +
          601 * ((pixel >> 8) & 0xFF) +
          117 * (pixel & 0xFF)) >> 10;
    }
    return column;
  }

}
