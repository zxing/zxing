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
  private final int[] pixelHolder;

  public LCDUIImageMonochromeBitmapSource(Image image) {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    pixelHolder = new int[1];
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  // This is expensive and should be used very sparingly.
  protected int getLuminance(int x, int y) {
    image.getRGB(pixelHolder, 0, width, x, y, 1, 1);
    int pixel = pixelHolder[0];

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

  // For efficiency, the RGB data and the luminance data share the same array.
  protected int[] getLuminanceRow(int y, int[] row) {
    if (row == null || row.length < width) {
      row = new int[width];
    }
    image.getRGB(row, 0, width, 0, y, width, 1);
    for (int x = 0; x < width; x++) {
      int pixel = row[x];
      row[x] = (((pixel & 0x00FF0000) >> 16) +
                ((pixel & 0x0000FF00) >>  7) +
                 (pixel & 0x000000FF       )) >> 2;
    }
    return row;
  }

  protected int[] getLuminanceColumn(int x, int[] column) {
    if (column == null || column.length < height) {
      column = new int[height];
    }
    image.getRGB(column, 0, 1, x, 0, 1, height);
    for (int y = 0; y < height; y++) {
      int pixel = column[y];
      column[y] = (((pixel & 0x00FF0000) >> 16) +
                   ((pixel & 0x0000FF00) >>  7) +
                    (pixel & 0x000000FF       )) >> 2;
    }
    return column;
  }

}
