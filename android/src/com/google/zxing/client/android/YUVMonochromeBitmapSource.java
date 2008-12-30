/*
 * Copyright (C) 2008 ZXing authors
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
import android.graphics.Rect;
import com.google.zxing.common.BaseMonochromeBitmapSource;

/**
 * This object implements MonochromeBitmapSource around an array of YUV data, giving you the option
 * to crop to a rectangle within the full data. This can be used to exclude superfluous pixels
 * around the perimeter and speed up decoding.
 */
final class YUVMonochromeBitmapSource extends BaseMonochromeBitmapSource {

  private final byte[] mYUVData;
  private final int mDataWidth;
  private final Rect mCrop;

  /**
   * Builds an object around a YUV buffer from the camera.
   *
   * @param yuvData    A byte array of planar Y data, followed by interleaved U and V
   * @param dataWidth  The width of the Y data
   * @param dataHeight The height of the Y data
   * @param crop       The rectangle within the yuvData to expose to MonochromeBitmapSource users
   */
  YUVMonochromeBitmapSource(byte[] yuvData, int dataWidth, int dataHeight, Rect crop) {
    mYUVData = yuvData;
    mDataWidth = dataWidth;
    mCrop = crop;
    assert (crop.width() <= dataWidth);
    assert (crop.height() <= dataHeight);
  }

  @Override
  public int getHeight() {
    return mCrop.height();
  }

  @Override
  public int getWidth() {
    return mCrop.width();
  }

  /**
   * The Y channel is stored as planar data at the head of the array, so we just ignore the
   * interleavd U and V which follow it.
   *
   * @param x The x coordinate to fetch within crop
   * @param y The y coordinate to fetch within crop
   * @return The luminance as an int, from 0-255
   */
  @Override
  protected int getLuminance(int x, int y) {
    return mYUVData[(y + mCrop.top) * mDataWidth + x + mCrop.left] & 0xff;
  }

  @Override
  protected int[] getLuminanceRow(int y, int[] row) {
    int width = getWidth();
    if (row == null || row.length < width) {
      row = new int[width];
    }
    int offset = (y + mCrop.top) * mDataWidth + mCrop.left;
    for (int x = 0; x < width; x++) {
      row[x] = mYUVData[offset + x] & 0xff;
    }
    return row;
  }

  @Override
  protected int[] getLuminanceColumn(int x, int[] column) {
    int height = getHeight();
    if (column == null || column.length < height) {
      column = new int[height];
    }
    int offset = mCrop.top * mDataWidth + mCrop.left + x;
    for (int y = 0; y < height; y++) {
      column[y] = mYUVData[offset] & 0xff;
      offset += mDataWidth;
    }
    return column;
  }

  /**
   * Create a greyscale Android Bitmap from the YUV data based on the crop rectangle.
   *
   * @return An 8888 bitmap.
   */
  public Bitmap renderToBitmap() {
    int width = mCrop.width();
    int height = mCrop.height();
    int[] pixels = new int[width * height];
    for (int y = 0; y < height; y++) {
      int base = (y + mCrop.top) * mDataWidth + mCrop.left;
      for (int x = 0; x < width; x++) {
        int grey = mYUVData[base + x] & 0xff;
        pixels[y * width + x] = (0xff << 24) | (grey << 16) | (grey << 8) | grey;
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }

}
