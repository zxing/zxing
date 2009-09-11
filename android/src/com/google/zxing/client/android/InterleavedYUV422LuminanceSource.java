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

package com.google.zxing.client.android;

import com.google.zxing.LuminanceSource;

import android.graphics.Bitmap;

/**
 * This object extends LuminanceSource around an array of YUV data returned from the camera driver,
 * with the option to crop to a rectangle within the full data. This can be used to exclude
 * superfluous pixels around the perimeter and speed up decoding.
 *
 * It handles YUV 422 interleaved data, where each pixel consists of first a Y value, then
 * a color value, with U and V alternating at each pixel.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class InterleavedYUV422LuminanceSource extends BaseLuminanceSource {
  private final byte[] yuvData;
  private final int dataWidth;
  private final int dataHeight;
  private final int left;
  private final int top;

  public InterleavedYUV422LuminanceSource(byte[] yuvData, int dataWidth, int dataHeight,
      int left, int top, int width, int height) {
    super(width, height);

    if (left + width > dataWidth || top + height > dataHeight) {
      throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
    }

    this.yuvData = yuvData;
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
    int offset = (y + top) * dataWidth * 2 + (left * 2);
    byte[] yuv = yuvData;
    for (int x = 0; x < width; x++) {
      row[x] = yuv[offset + (x << 1)];
    }
    return row;
  }

  @Override
  public byte[] getMatrix() {
    int width = getWidth();
    int height = getHeight();
    int area = width * height;
    byte[] matrix = new byte[area];
    int inputOffset = top * dataWidth * 2 + (left * 2);
    byte[] yuv = yuvData;

    for (int y = 0; y < height; y++) {
      int outputOffset = y * width;
      for (int x = 0; x < width; x++) {
        matrix[outputOffset + x] = yuv[inputOffset + (x << 1)];
      }
      inputOffset += (dataWidth * 2);
    }
    return matrix;
  }

  @Override
  public boolean isCropSupported() {
    return true;
  }

  @Override
  public LuminanceSource crop(int left, int top, int width, int height) {
    return new InterleavedYUV422LuminanceSource(yuvData, dataWidth, dataHeight, left, top,
        width, height);
  }

  @Override
  public int getDataWidth() {
    return dataWidth;
  }

  @Override
  public int getDataHeight() {
    return dataHeight;
  }

  @Override
  public Bitmap renderCroppedGreyscaleBitmap() {
    int width = getWidth();
    int height = getHeight();
    int[] pixels = new int[width * height];
    byte[] yuv = yuvData;
    int inputOffset = top * dataWidth * 2 + (left * 2);

    for (int y = 0; y < height; y++) {
      int outputOffset = y * width;
      for (int x = 0; x < width; x++) {
        int grey = yuv[inputOffset + (x << 1)] & 0xff;
        pixels[outputOffset + x] = (0xff000000) | (grey * 0x00010101);
      }
      inputOffset += (dataWidth * 2);
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }

  // Not currently needed.
  @Override
  public Bitmap renderFullColorBitmap(boolean halfSize) {
    return null;
  }
}
