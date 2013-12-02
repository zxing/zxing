/*
 * Copyright 2009 ZXing authors
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

import com.google.zxing.LuminanceSource;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * This LuminanceSource implementation is meant for J2SE clients and our blackbox unit tests.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 * @author code@elektrowolle.de (Wolfgang Jung)
 */
public final class BufferedImageLuminanceSource extends LuminanceSource {

  private static final double MINUS_45_IN_RADIANS = -0.7853981633974483; // Math.toRadians(-45.0)

  private final BufferedImage image;
  private final int left;
  private final int top;

  public BufferedImageLuminanceSource(BufferedImage image) {
    this(image, 0, 0, image.getWidth(), image.getHeight());
  }

  public BufferedImageLuminanceSource(BufferedImage image, int left, int top, int width, int height) {
    super(width, height);

    if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
      this.image = image;
    } else {
      int sourceWidth = image.getWidth();
      int sourceHeight = image.getHeight();
      if (left + width > sourceWidth || top + height > sourceHeight) {
        throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
      }

      this.image = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_BYTE_GRAY);

      WritableRaster raster = this.image.getRaster();
      int[] buffer = new int[width];
      for (int y = top; y < top + height; y++) {
        image.getRGB(left, y, width, 1, buffer, 0, sourceWidth);
        for (int x = 0; x < width; x++) {
          int pixel = buffer[x];

          // The color of fully-transparent pixels is irrelevant. They are often, technically, fully-transparent
          // black (0 alpha, and then 0 RGB). They are often used, of course as the "white" area in a
          // barcode image. Force any such pixel to be white:
          if ((pixel & 0xFF000000) == 0) {
            pixel = 0xFFFFFFFF; // = white
          }

          // .229R + 0.587G + 0.114B (YUV/YIQ for PAL and NTSC)
          buffer[x] =
              (306 * ((pixel >> 16) & 0xFF) +
               601 * ((pixel >> 8) & 0xFF) +
               117 * (pixel & 0xFF) +
               0x200) >> 10;
        }
        raster.setPixels(left, y, width, 1, buffer);
      }

    }
    this.left = left;
    this.top = top;
  }

  @Override
  public byte[] getRow(int y, byte[] row) {
    if (y < 0 || y >= getHeight()) {
      throw new IllegalArgumentException("Requested row is outside the image: " + y);
    }
    int width = getWidth();
    if (row == null || row.length < width) {
      row = new byte[width];
    }
    // The underlying raster of image consists of bytes with the luminance values
    image.getRaster().getDataElements(left, top + y, width, 1, row);
    return row;
  }

  @Override
  public byte[] getMatrix() {
    int width = getWidth();
    int height = getHeight();
    int area = width * height;
    byte[] matrix = new byte[area];
    // The underlying raster of image consists of area bytes with the luminance values
    image.getRaster().getDataElements(left, top, width, height, matrix);
    return matrix;
  }

  @Override
  public boolean isCropSupported() {
    return true;
  }

  @Override
  public LuminanceSource crop(int left, int top, int width, int height) {
    return new BufferedImageLuminanceSource(image, this.left + left, this.top + top, width, height);
  }

  /**
   * This is always true, since the image is a gray-scale image.
   *
   * @return true
   */
  @Override
  public boolean isRotateSupported() {
    return true;
  }

  @Override
  public LuminanceSource rotateCounterClockwise() {
    int sourceWidth = image.getWidth();
    int sourceHeight = image.getHeight();

    // Rotate 90 degrees counterclockwise.
    AffineTransform transform = new AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, sourceWidth);

    // Note width/height are flipped since we are rotating 90 degrees.
    BufferedImage rotatedImage = new BufferedImage(sourceHeight, sourceWidth, BufferedImage.TYPE_BYTE_GRAY);

    // Draw the original image into rotated, via transformation
    Graphics2D g = rotatedImage.createGraphics();
    g.drawImage(image, transform, null);
    g.dispose();

    // Maintain the cropped region, but rotate it too.
    int width = getWidth();
    return new BufferedImageLuminanceSource(rotatedImage, top, sourceWidth - (left + width), getHeight(), width);
  }

  @Override
  public LuminanceSource rotateCounterClockwise45() {
    int width = getWidth();
    int height = getHeight();

    int oldCenterX = left + width / 2;
    int oldCenterY = top + height / 2;

    // Rotate 45 degrees counterclockwise.
    AffineTransform transform = AffineTransform.getRotateInstance(MINUS_45_IN_RADIANS, oldCenterX, oldCenterY);

    int sourceDimension = Math.max(image.getWidth(), image.getHeight());
    BufferedImage rotatedImage = new BufferedImage(sourceDimension, sourceDimension, BufferedImage.TYPE_BYTE_GRAY);

    // Draw the original image into rotated, via transformation
    Graphics2D g = rotatedImage.createGraphics();
    g.drawImage(image, transform, null);
    g.dispose();

    int halfDimension = Math.max(width, height) / 2;
    int newLeft = Math.max(0, oldCenterX - halfDimension);
    int newTop = Math.max(0, oldCenterY - halfDimension);
    int newRight = Math.min(sourceDimension - 1, oldCenterX + halfDimension);
    int newBottom = Math.min(sourceDimension - 1, oldCenterY + halfDimension);

    return new BufferedImageLuminanceSource(rotatedImage, newLeft, newTop, newRight - newLeft, newBottom - newTop);
  }

}
