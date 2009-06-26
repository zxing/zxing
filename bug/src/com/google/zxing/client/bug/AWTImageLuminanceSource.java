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

package com.google.zxing.client.bug;

import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;

import java.awt.Image;
import java.awt.image.PixelGrabber;

/**
 * An implementation based on AWT's Image representation. This can be used on CDC devices
 * or other devices that do not have access to the Mobile Information Device Profile
 * and thus do not have access to javax.microedition.lcdui.Image.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author David Albert
 * @author Sean Owen
 */
public final class AWTImageLuminanceSource extends LuminanceSource {

  private final int[] pixels;

  public AWTImageLuminanceSource(Image image) throws ReaderException {
    super(image.getWidth(null), image.getHeight(null));

    int width = getWidth();
    int height = getHeight();
    pixels = new int[width * height];

    // Seems best in this situation to grab all pixels upfront. Grabbing any individual pixel
    // entails creating a relatively expensive object and calling through several methods.
    PixelGrabber grabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);
    try {
      grabber.grabPixels();
    } catch (InterruptedException ie) {
      throw ReaderException.getInstance();
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

    int offset = y * width;
    for (int x = 0; x < width; x++) {
      int pixel = pixels[offset + x];
      int luminance = (((pixel & 0x00FF0000) >> 16) +
                       ((pixel & 0x0000FF00) >>  7) +
                        (pixel & 0x000000FF       )) >> 2;
      row[x] = (byte) luminance;
    }
    return row;
  }

  public byte[] getMatrix() {
    int width = getWidth();
    int height = getHeight();
    int area = width * height;
    byte[] matrix = new byte[area];

    for (int y = 0; y < height; y++) {
      int offset = y * width;
      for (int x = 0; x < width; x++) {
        int pixel = pixels[offset + x];
        int luminance = (((pixel & 0x00FF0000) >> 16) +
                         ((pixel & 0x0000FF00) >>  7) +
                          (pixel & 0x000000FF       )) >> 2;
        matrix[x] = (byte) luminance;
      }
    }
    return matrix;
  }

}
