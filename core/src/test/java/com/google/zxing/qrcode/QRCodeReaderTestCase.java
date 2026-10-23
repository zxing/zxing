/*
 * Copyright 2026 ZXing authors
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

package com.google.zxing.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Decodes the output of {@link QRCodeWriter} after drawing it as a clean bitmap.
 */
public final class QRCodeReaderTestCase extends Assert {

  /**
   * When drawn with 4 pixels per module, the data of "6ywm" holds a pattern that looks like a finder
   * pattern, directly under the top left one. It must not be taken for the bottom left corner.
   */
  @Test
  public void testPatternInDataIsNotACorner() throws Exception {
    assertEquals("6ywm", encodeAndDecode("6ywm", 4));
  }

  @Test
  public void testPatternInDataIsNotACornerBesidePattern() throws Exception {
    assertEquals("GG]DW*27?zS8", encodeAndDecode("GG]DW*27?zS8", 4));
  }

  private static String encodeAndDecode(String contents, int pixelsPerModule) throws Exception {
    BitMatrix matrix = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, 0, 0);
    int quietZone = 4;
    int size = (matrix.getWidth() + 2 * quietZone) * pixelsPerModule;
    byte[] pixels = new byte[size * size];
    Arrays.fill(pixels, (byte) 0xFF);
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        int moduleX = x / pixelsPerModule - quietZone;
        int moduleY = y / pixelsPerModule - quietZone;
        if (moduleX >= 0 && moduleX < matrix.getWidth() &&
            moduleY >= 0 && moduleY < matrix.getHeight() && matrix.get(moduleX, moduleY)) {
          pixels[y * size + x] = 0;
        }
      }
    }
    LuminanceSource source = new PlanarYUVLuminanceSource(pixels, size, size, 0, 0, size, size, false);
    return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source))).getText();
  }

}
