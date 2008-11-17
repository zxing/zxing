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

package com.google.zxing.client.bug;

import com.google.zxing.ReaderException;
import com.google.zxing.common.BaseMonochromeBitmapSource;

import java.awt.Image;
import java.awt.image.PixelGrabber;

/**
 * <p>An implementation based on AWT's {@link Image} representation.
 * This can be used on CDC devices or other devices that do not have access to the
 * Mobile Information Device Profile and thus do not have access to
 * javax.microedition.lcdui.Image.</p>
 *
 * @author David Albert
 * @author Sean Owen
 */
public final class AWTImageMonochromeBitmapSource extends BaseMonochromeBitmapSource {

  private final int height;
  private final int width;
  private final int[] pixels;

  public AWTImageMonochromeBitmapSource(Image image) throws ReaderException {
    height = image.getHeight(null);
    width = image.getWidth(null);
    pixels = new int[height * width];
    // Seems best in this situation to grab all pixels upfront. Grabbing any individual pixel
    // entails creating a relatively expensive object and calling through several methods.
    PixelGrabber grabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);
    try {
      grabber.grabPixels();
    } catch (InterruptedException ie) {
      throw ReaderException.getInstance();
    }
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  /**
   * See <code>com.google.zxing.client.j2me.LCDUIImageMonochromeBitmapSource</code> for more explanation
   * of the computation used in this method.
   */
  protected int getLuminance(int x, int y) {
    int pixel = pixels[y * width + x];
    return (((pixel & 0x00FF0000) >> 16) +
            ((pixel & 0x0000FF00) >>  7) +
             (pixel & 0x000000FF       )) >> 2;
  }

  protected int[] getLuminanceRow(int y, int[] row) {
    if (row == null || row.length < width) {
      row = new int[width];
    }
    int offset = y * width;
    for (int x = 0; x < width; x++) {
      int pixel = pixels[offset + x];
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
    int offset = x;
    for (int y = 0; y < height; y++) {
      int pixel = pixels[offset];
      column[y] = (((pixel & 0x00FF0000) >> 16) +
                   ((pixel & 0x0000FF00) >>  7) +
                    (pixel & 0x000000FF       )) >> 2;
      offset += width;
    }
    return column;
  }

}
