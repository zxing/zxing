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

package com.google.zxing.client.androidtest;

import com.google.zxing.LuminanceSource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileNotFoundException;

/**
 * This class is used to help decode images from files which arrive as RGB data from
 * Android bitmaps. It does not support cropping or rotation.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class RGBLuminanceSource extends LuminanceSource {

  private final byte[] luminances;

  public RGBLuminanceSource(String path) throws FileNotFoundException {
    this(loadBitmap(path));
  }

  public RGBLuminanceSource(Bitmap bitmap) {
    super(bitmap.getWidth(), bitmap.getHeight());

    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int[] pixels = new int[width * height];
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

    // In order to measure pure decoding speed, we convert the entire image to a greyscale array
    // up front, which is the same as the Y channel of the YUVLuminanceSource in the real app.
    luminances = new byte[width * height];
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      for (int x = 0; x < width; x++) {
        int pixel = pixels[offset + x];
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;
        if (r == g && g == b) {
          // Image is already greyscale, so pick any channel.
          luminances[offset + x] = (byte) r;
        } else {
          // Calculate luminance cheaply, favoring green.
          luminances[offset + x] = (byte) ((r + g + g + b) >> 2);
        }
      }
    }
  }

  public byte[] getRow(int y, byte[] row) {
    if (y < 0 || y >= getHeight()) {
      throw new IllegalArgumentException("Requested row is outside the image: " + y);
    }
    int width = getWidth();
    if (row == null || row.length < width) {
      row = new byte[width];
    }

    System.arraycopy(luminances, y * width, row, 0, width);
    return row;
  }

  // Since this class does not support cropping, the underlying byte array already contains
  // exactly what the caller is asking for, so give it to them without a copy.
  public byte[] getMatrix() {
    return luminances;
  }

  private static Bitmap loadBitmap(String path) throws FileNotFoundException {
    Bitmap bitmap = BitmapFactory.decodeFile(path);
    if (bitmap == null) {
      throw new FileNotFoundException("Couldn't open " + path);
    }
    return bitmap;
  }

}
