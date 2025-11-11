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
 * This class is used to help decode images from files which arrive as RGB data from
 * an ARGB pixel array.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Betaminos
 */
public final class RGBLuminanceSource extends GrayscaleLuminanceSource {

  public RGBLuminanceSource(int width, int height, int[] pixels) {
    super(width, height, toGrayscale(width, height, pixels));
  }

  private static byte[] toGrayscale(int width, int height, int[] pixels) {
    int size = width * height;
    if (pixels == null || pixels.length < size) {
      throw new IllegalArgumentException("Pixel array length is less than width * height");
    }
    byte[] luminances = new byte[size];
    for (int offset = 0; offset < size; offset++) {
      int pixel = pixels[offset];
      int r = (pixel >> 16) & 0xff; // red
      int g2 = (pixel >> 7) & 0x1fe; // 2 * green
      int b = pixel & 0xff; // blue
      // Calculate green-favouring average cheaply
      luminances[offset] = (byte) ((r + g2 + b) / 4);
    }
    return luminances;
  }
}
