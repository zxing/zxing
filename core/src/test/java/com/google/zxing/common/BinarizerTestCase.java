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

package com.google.zxing.common;

import com.google.zxing.GrayscaleLuminanceSource;
import com.google.zxing.NotFoundException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Tests {@link HybridBinarizer} and {@link GlobalHistogramBinarizer} edge cases and word packing.
 */
public final class BinarizerTestCase extends Assert {

  @Test
  public void testHybridBinarizerEdgeWidthsAndPadding() throws NotFoundException {
    // Include widths where maxXOffset = width - 8 has (maxXOffset & 31) in [25, 31],
    // exercising the two-word straddle path in HybridBinarizer.thresholdBlock().
    int[][] dims = {
      {40, 40}, {41, 41}, {63, 45}, {64, 48}, {65, 43}, {71, 53}, {95, 65}
    };
    Random rng = new Random(99);
    for (int[] dim : dims) {
      int w = dim[0];
      int h = dim[1];
      byte[] pixels = new byte[w * h];
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          int v = (((x / 2) ^ (y / 4)) & 1) == 0 ? 10 : 245;
          pixels[y * w + x] = (byte) Math.max(0, Math.min(255, v + rng.nextInt(15) - 7));
        }
      }
      BitMatrix matrix = new HybridBinarizer(new GrayscaleLuminanceSource(w, h, pixels)).getBlackMatrix();
      assertEquals(w, matrix.getWidth());
      assertEquals(h, matrix.getHeight());
      // Padding bits beyond width must remain 0
      int[] enclosing = matrix.getEnclosingRectangle();
      assertNotNull(enclosing);
      assertTrue(enclosing[0] + enclosing[2] <= w);
      assertTrue(enclosing[1] + enclosing[3] <= h);
    }

    // All-zero image: threshold is 0, so <= 0 must set every bit cleanly without corrupting padding
    byte[] allBlack = new byte[65 * 43];
    BitMatrix blackMatrix = new HybridBinarizer(new GrayscaleLuminanceSource(65, 43, allBlack)).getBlackMatrix();
    assertArrayEquals(new int[] {0, 0, 65, 43}, blackMatrix.getEnclosingRectangle());
  }

  @Test
  public void testGlobalHistogramBinarizerRowsAndMatrix() throws NotFoundException {
    int[] widths = {3, 4, 31, 32, 33, 34, 63, 64, 65, 71};
    Random rng = new Random(77);
    for (int w : widths) {
      int h = 15;
      byte[] pixels = new byte[w * h];
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          int v = (x & 1) == 0 ? 20 : 230;
          pixels[y * w + x] = (byte) Math.max(0, Math.min(255, v + rng.nextInt(11) - 5));
        }
      }
      GrayscaleLuminanceSource source = new GrayscaleLuminanceSource(w, h, pixels);
      GlobalHistogramBinarizer binarizer = new GlobalHistogramBinarizer(source);

      // Matrix: check padding bits and dimensions
      BitMatrix matrix = binarizer.getBlackMatrix();
      int[] enclosing = matrix.getEnclosingRectangle();
      assertNotNull(enclosing);
      assertTrue(enclosing[0] + enclosing[2] <= w);

      // Row: check both fresh BitArray and pre-allocated larger BitArray
      BitArray freshRow = binarizer.getBlackRow(h / 2, null);
      BitArray reusedRow = new BitArray(w + 40);
      reusedRow.setRange(0, w + 40);
      reusedRow = binarizer.getBlackRow(h / 2, reusedRow);
      assertFalse(freshRow.get(0));
      assertFalse(freshRow.get(w - 1));
      for (int x = 0; x < w; x++) {
        assertEquals(freshRow.get(x), reusedRow.get(x));
      }
      assertTrue(reusedRow.isRange(w, w + 40, false));
    }
  }

}
