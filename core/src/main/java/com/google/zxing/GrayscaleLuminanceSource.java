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

package com.google.zxing;

/**
 * This class is used to help decode images from files which arrive as a grayscale
 * pixel array.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Betaminos
 */
public class GrayscaleLuminanceSource extends LuminanceSource {

  private final byte[] luminances;
  private final int dataWidth;
  private final int dataHeight;
  private final int left;
  private final int top;

  public GrayscaleLuminanceSource(int width, int height, byte[] pixels) {
    super(width, height);
    luminances = pixels;
    dataWidth = width;
    dataHeight = height;
    left = 0;
    top = 0;
  }

  private GrayscaleLuminanceSource(byte[] pixels,
                                   int dataWidth,
                                   int dataHeight,
                                   int left,
                                   int top,
                                   int width,
                                   int height) {
    super(width, height);
    if (left < 0 || top < 0 || width < 0 || height < 0 ||
        width > dataWidth - left || height > dataHeight - top) {
      throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
    }
    this.luminances = pixels;
    this.dataWidth = dataWidth;
    this.dataHeight = dataHeight;
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
    int offset = (y + top) * dataWidth + left;
    System.arraycopy(luminances, offset, row, 0, width);
    return row;
  }

  @Override
  public byte[] getMatrix() {
    int width = getWidth();
    int height = getHeight();

    // If the caller asks for the entire underlying image, save the copy and give them the
    // original data. The docs specifically warn that result.length must be ignored.
    if (width == dataWidth && height == dataHeight) {
      return luminances;
    }

    int area = width * height;
    byte[] matrix = new byte[area];
    int inputOffset = top * dataWidth + left;

    // If the width matches the full width of the underlying data, perform a single copy.
    if (width == dataWidth) {
      System.arraycopy(luminances, inputOffset, matrix, 0, area);
      return matrix;
    }

    // Otherwise copy one cropped row at a time.
    for (int y = 0; y < height; y++) {
      int outputOffset = y * width;
      System.arraycopy(luminances, inputOffset, matrix, outputOffset, width);
      inputOffset += dataWidth;
    }
    return matrix;
  }

  @Override
  public boolean isCropSupported() {
    return true;
  }

  @Override
  public LuminanceSource crop(int left, int top, int width, int height) {
    return new GrayscaleLuminanceSource(luminances,
                                        dataWidth,
                                        dataHeight,
                                        this.left + left,
                                        this.top + top,
                                        width,
                                        height);
  }

  @Override
  public boolean isRotateSupported() {
    return true;
  }

  @Override
  public LuminanceSource rotateCounterClockwise() {
    byte[] src = this.luminances;
    int srcWidth = this.dataWidth;
    int srcHeight = this.dataHeight;
    byte[] rotated = new byte[src.length];
    int tileSize = 32;
    for (int by = 0; by < srcHeight; by += tileSize) {
      int maxY = Math.min(by + tileSize, srcHeight);
      for (int bx = 0; bx < srcWidth; bx += tileSize) {
        int maxX = Math.min(bx + tileSize, srcWidth);
        for (int x = bx; x < maxX; x++) {
          int dstRow = (srcWidth - 1 - x) * srcHeight;
          int srcIdx = by * srcWidth + x;
          for (int y = by; y < maxY; y++, srcIdx += srcWidth) {
            rotated[dstRow + y] = src[srcIdx];
          }
        }
      }
    }
    int newWidth = getHeight();
    int newHeight = getWidth();
    int newLeft = top;
    int newTop = srcWidth - (left + getWidth());
    return new GrayscaleLuminanceSource(rotated, srcHeight, srcWidth, newLeft, newTop, newWidth, newHeight);
  }
}
