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
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

/**
 * This LuminanceSource implementation is meant for J2SE clients and our blackbox unit tests.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class BufferedImageLuminanceSource extends LuminanceSource {

  private final BufferedImage image;
  private final int left;
  private final int top;
  private int[] rgbData;

  public BufferedImageLuminanceSource(BufferedImage image) {
    this(image, 0, 0, image.getWidth(), image.getHeight());
  }

  public BufferedImageLuminanceSource(BufferedImage image, int left, int top, int width,
      int height) {
    super(width, height);

    int sourceWidth = image.getWidth();
    int sourceHeight = image.getHeight();
    if (left + width > sourceWidth || top + height > sourceHeight) {
      throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
    }

    this.image = image;
    this.left = left;
    this.top = top;
  }

  // These methods use an integer calculation for luminance derived from:
  // <code>Y = 0.299R + 0.587G + 0.114B</code>
  @Override
  public byte[] getRow(int y, byte[] row) {
    if (y < 0 || y >= getHeight()) {
      throw new IllegalArgumentException("Requested row is outside the image: " + y);
    }
    int width = getWidth();
    if (row == null || row.length < width) {
      row = new byte[width];
    }

    if (rgbData == null || rgbData.length < width) {
      rgbData = new int[width];
    }
    image.getRGB(left, top + y, width, 1, rgbData, 0, width);
    for (int x = 0; x < width; x++) {
      int pixel = rgbData[x];
      int luminance = (306 * ((pixel >> 16) & 0xFF) +
          601 * ((pixel >> 8) & 0xFF) +
          117 * (pixel & 0xFF) +
          (0x200)) >> 10; // 0x200 = 1<<9, half an lsb of the result to force rounding
      row[x] = (byte) luminance;
    }
    return row;
  }

  @Override
  public byte[] getMatrix() {
    int width = getWidth();
    int height = getHeight();
    int area = width * height;
    byte[] matrix = new byte[area];

    int[] rgb = new int[area];
    image.getRGB(left, top, width, height, rgb, 0, width);
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      for (int x = 0; x < width; x++) {
        int pixel = rgb[offset + x];
        int luminance = (306 * ((pixel >> 16) & 0xFF) +
            601 * ((pixel >> 8) & 0xFF) +
            117 * (pixel & 0xFF) +
            (0x200)) >> 10; // 0x200 = 1<<9, half an lsb of the result to force rounding
        matrix[offset + x] = (byte) luminance;
      }
    }
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

  // Can't run AffineTransforms on images of unknown format.
  @Override
  public boolean isRotateSupported() {
    return image.getType() != BufferedImage.TYPE_CUSTOM;
  }

  @Override
  public LuminanceSource rotateCounterClockwise() {
    if (!isRotateSupported()) {
      throw new IllegalStateException("Rotate not supported");
    }
    int sourceWidth = image.getWidth();
    int sourceHeight = image.getHeight();

    // Rotate 90 degrees counterclockwise.
    AffineTransform transform = new AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, sourceWidth);

    // Note width/height are flipped since we are rotating 90 degrees.
    BufferedImage rotatedImage = new BufferedImage(sourceHeight, sourceWidth, image.getType());

    // Draw the original image into rotated, via transformation
    Graphics2D g = rotatedImage.createGraphics();
    g.drawImage(image, transform, null);
    g.dispose();

    // Maintain the cropped region, but rotate it too.
    int width = getWidth();
    return new BufferedImageLuminanceSource(rotatedImage, top, sourceWidth - (left + width),
        getHeight(), width);
  }

}
