/*
 * Copyright 2014 ZXing authors
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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link PlanarYUVLuminanceSource}.
 */
public final class PlanarYUVLuminanceSourceTestCase extends Assert {

  private static final byte[] YUV = {
       0,  1,  1,  2,  3,  5,
       8, 13, 21, 34, 55, 89,
       0,  -1,  -1,  -2,  -3,  -5,
      -8, -13, -21, -34, -55, -89,
      127, 127, 127, 127, 127, 127,
      127, 127, 127, 127, 127, 127,
  };
  private static final int COLS = 6;
  private static final int ROWS = 4;
  private static final byte[] Y = new byte[COLS * ROWS];
  static {
    System.arraycopy(YUV, 0, Y, 0, Y.length);
  }

  @Test
  public void testNoCrop() {
    PlanarYUVLuminanceSource source =
        new PlanarYUVLuminanceSource(YUV, COLS, ROWS, 0, 0, COLS, ROWS, false);
    assertEquals(Y, 0, source.getMatrix(), 0, Y.length);
    for (int r = 0; r < ROWS; r++) {
      assertEquals(Y, r * COLS, source.getRow(r, null), 0, COLS);
    }
  }

  @Test
  public void testCrop() {
    PlanarYUVLuminanceSource source =
        new PlanarYUVLuminanceSource(YUV, COLS, ROWS, 1, 1, COLS - 2, ROWS - 2, false);
    assertTrue(source.isCropSupported());
    byte[] cropMatrix = source.getMatrix();
    for (int r = 0; r < ROWS - 2; r++) {
      assertEquals(Y, (r + 1) * COLS + 1, cropMatrix, r * (COLS - 2), COLS - 2);
    }
    for (int r = 0; r < ROWS - 2; r++) {
      assertEquals(Y, (r + 1) * COLS + 1, source.getRow(r, null), 0, COLS - 2);
    }
  }

  @Test
  public void testThumbnail() {
    PlanarYUVLuminanceSource source =
        new PlanarYUVLuminanceSource(YUV, COLS, ROWS, 0, 0, COLS, ROWS, false);
    assertArrayEquals(
        new int[] { 0xFF000000, 0xFF010101, 0xFF030303, 0xFF000000, 0xFFFFFFFF, 0xFFFDFDFD },
        source.renderThumbnail());
  }

  private static void assertEquals(byte[] expected, int expectedFrom,
                                   byte[] actual, int actualFrom,
                                   int length) {
    for (int i = 0; i < length; i++) {
      assertEquals(expected[expectedFrom + i], actual[actualFrom + i]);
    }
  }

}
