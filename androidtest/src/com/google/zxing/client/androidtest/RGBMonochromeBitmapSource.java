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

package com.google.zxing.client.androidtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.zxing.common.BaseMonochromeBitmapSource;

import java.io.FileNotFoundException;

public final class RGBMonochromeBitmapSource extends BaseMonochromeBitmapSource {

  private final byte[] mLuminances;

  public RGBMonochromeBitmapSource(String path) throws FileNotFoundException {
    this(loadBitmap(path));
  }

  private static Bitmap loadBitmap(String path) throws FileNotFoundException {
    Bitmap bitmap = BitmapFactory.decodeFile(path);
    if (bitmap == null) {
      throw new FileNotFoundException("Couldn't open " + path);
    }
    return bitmap;
  }

  public RGBMonochromeBitmapSource(Bitmap bitmap) {
    super(bitmap.getHeight(), bitmap.getWidth());
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int[] pixels = new int[width * height];
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

    // In order to measure pure decoding speed, we convert the entire image to a greyscale array up
    // front, which is the same as the Y channel of the YUVMonochromeBitmapSource in the real app.
    mLuminances = new byte[width * height];
    for (int y = 0; y < height; y++) {
      int offset = y * height;
      for (int x = 0; x < width; x++) {
        int pixel = pixels[offset + x];
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;
        if (r == g && g == b) {
          // Image is already greyscale, so pick any channel
          mLuminances[offset + x] = (byte) r;
        } else {
          // Calculate luminance cheaply, favoring green
          mLuminances[offset + x] = (byte) ((r + g + g + b) >> 2);
        }
      }
    }
  }

  @Override
  protected int getLuminance(int x, int y) {
    return mLuminances[y * getWidth() + x] & 0xff;
  }

  @Override
  protected int[] getLuminanceRow(int y, int[] row) {
    int width = getWidth();
    if (row == null || row.length < width) {
      row = new int[width];
    }
    int offset = y * width;
    for (int x = 0; x < width; x++) {
      row[x] = mLuminances[offset + x] & 0xff;
    }
    return row;
  }

  @Override
  protected int[] getLuminanceColumn(int x, int[] column) {
    int width = getWidth();
    int height = getHeight();
    if (column == null || column.length < height) {
      column = new int[height];
    }
    int offset = x;
    for (int y = 0; y < height; y++) {
      column[y] = mLuminances[offset] & 0xff;
      offset += width;
    }
    return column;
  }

}
