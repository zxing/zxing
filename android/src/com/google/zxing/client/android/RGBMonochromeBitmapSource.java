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

package com.google.zxing.client.android;

import android.graphics.Bitmap;
import com.google.zxing.common.BaseMonochromeBitmapSource;

/**
 * This object implements MonochromeBitmapSource around an Android Bitmap.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author srowen@google.com (Sean Owen)
 */
final class RGBMonochromeBitmapSource extends BaseMonochromeBitmapSource {

  private final Bitmap image;
  private final int[] rgbRow;
  private int cachedRow;

  RGBMonochromeBitmapSource(Bitmap image) {
    this.image = image;
    rgbRow = new int[image.getWidth()];
    cachedRow = -1;
  }

  public int getHeight() {
    return image.height();
  }

  public int getWidth() {
    return image.width();
  }

  /**
   * An optimized approximation of a more proper conversion from RGB to luminance which
   * only uses shifts.
   */
  public int getLuminance(int x, int y) {
    int pixel;
    if (cachedRow == y) {
      pixel = rgbRow[x];
    } else {
      pixel = image.getPixel(x, y);
    }

    // Instead of multiplying by 306, 601, 117, we multiply by 256, 512, 256, so that
    // the multiplies can be implemented as shifts.
    //
    // Really, it's:
    //
    // return ((((pixel >> 16) & 0xFF) << 8) +
    //         (((pixel >>  8) & 0xFF) << 9) +
    //         (( pixel        & 0xFF) << 8)) >> 10;
    //
    // That is, we're replacing the coefficients in the original with powers of two,
    // which can be implemented as shifts, even though changing the coefficients slightly
    // corrupts the conversion. Not significant for our purposes.
    return (((pixel & 0x00FF0000) >> 16) +
            ((pixel & 0x0000FF00) >>  7) +
             (pixel & 0x000000FF       )) >> 2;
  }

  public void cacheRowForLuminance(int y) {
    if (y != cachedRow) {
      int width = image.width();
      image.getPixels(rgbRow, 0, width, 0, y, width, 1);
      cachedRow = y;
    }
  }

}
