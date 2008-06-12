/*
 * Copyright 2007 Google Inc.
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

  private final int[] rgbPixels;
  private final int width;
  private final int height;

  public LCDUIImageMonochromeBitmapSource(Image image) {
    width = image.getWidth();
    height = image.getHeight();
    rgbPixels = new int[width * height];
    image.getRGB(rgbPixels, 0, width, 0, 0, width, height);
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public int getLuminance(int x, int y) {
    int pixel = rgbPixels[y * width + x];

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

  // Nothing to do, since we have direct access to the image data.
  public void cacheRowForLuminance(int y) {

  }

}