/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.client.j2me;

import com.google.zxing.common.BaseMonochromeBitmapSource;

import javax.microedition.lcdui.Image;

/**
 * <p>An implementation based on Java ME's {@link Image} representation.</p>
 *
 * @author Sean Owen (srowen@google.com), Daniel Switkin (dswitkin@google.com)
 */
public final class LCDUIImageMonochromeBitmapSource extends BaseMonochromeBitmapSource {

  private final Image image;
  private final int height;
  private final int width;
  // For why this isn't final, see below
  private int[] rgbRow;
  private final int[] pixelHolder;
  private int cachedRow;

  public LCDUIImageMonochromeBitmapSource(Image image) {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    rgbRow = new int[width];
    pixelHolder = new int[1];
    cachedRow = -1;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public int getLuminance(int x, int y) {

    // Below, why the check for rgbRow being the right size? it should never change size
    // or need to be reallocated. But bizarrely we have seen a but on Sun's WTK, and on
    // some phones, where the array becomes zero-sized somehow. So we keep making sure the
    // array is OK.
    int pixel;
    if (cachedRow == y && rgbRow.length == width) {
      pixel = rgbRow[x];
    } else {
      image.getRGB(pixelHolder, 0, width, x, y, 1, 1);
      pixel = pixelHolder[0];
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
      // See explanation above
      if (rgbRow.length != width) {
        rgbRow = new int[width];
      }
      image.getRGB(rgbRow, 0, width, 0, y, width, 1);
      cachedRow = y;
    }
  }

}