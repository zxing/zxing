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

package com.google.zxing.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class BitMatrixTestCase extends Assert {

  @Test
  public void testGetSet() {
    BitMatrix matrix = new BitMatrix(33);
    assertEquals(33, matrix.getHeight());
    for (int y = 0; y < 33; y++) {
      for (int x = 0; x < 33; x++) {
        if (y * x % 3 == 0) {
          matrix.set(x, y);
        }
      }
    }
    for (int y = 0; y < 33; y++) {
      for (int x = 0; x < 33; x++) {
        assertEquals(y * x % 3 == 0, matrix.get(x, y));
      }
    }
  }

  @Test
  public void testSetRegion() {
    BitMatrix matrix = new BitMatrix(5);
    matrix.setRegion(1, 1, 3, 3);
    for (int y = 0; y < 5; y++) {
      for (int x = 0; x < 5; x++) {
        assertEquals(y >= 1 && y <= 3 && x >= 1 && x <= 3, matrix.get(x, y));
      }
    }
  }

  @Test
  public void testRectangularMatrix() {
    BitMatrix matrix = new BitMatrix(75, 20);
    assertEquals(75, matrix.getWidth());
    assertEquals(20, matrix.getHeight());
    matrix.set(10, 0);
    matrix.set(11, 1);
    matrix.set(50, 2);
    matrix.set(51, 3);
    matrix.flip(74, 4);
    matrix.flip(0, 5);

    // Should all be on
    assertTrue(matrix.get(10, 0));
    assertTrue(matrix.get(11, 1));
    assertTrue(matrix.get(50, 2));
    assertTrue(matrix.get(51, 3));
    assertTrue(matrix.get(74, 4));
    assertTrue(matrix.get(0, 5));

    // Flip a couple back off
    matrix.flip(50, 2);
    matrix.flip(51, 3);
    assertFalse(matrix.get(50, 2));
    assertFalse(matrix.get(51, 3));
  }

  @Test
  public void testRectangularSetRegion() {
    BitMatrix matrix = new BitMatrix(320, 240);
    assertEquals(320, matrix.getWidth());
    assertEquals(240, matrix.getHeight());
    matrix.setRegion(105, 22, 80, 12);

    // Only bits in the region should be on
    for (int y = 0; y < 240; y++) {
      for (int x = 0; x < 320; x++) {
        assertEquals(y >= 22 && y < 34 && x >= 105 && x < 185, matrix.get(x, y));
      }
    }
  }

  @Test
  public void testGetRow() {
    BitMatrix matrix = new BitMatrix(102, 5);
    for (int x = 0; x < 102; x++) {
      if ((x & 0x03) == 0) {
        matrix.set(x, 2);
      }
    }

    // Should allocate
    BitArray array = matrix.getRow(2, null);
    assertEquals(102, array.getSize());

    // Should reallocate
    BitArray array2 = new BitArray(60);
    array2 = matrix.getRow(2, array2);
    assertEquals(102, array2.getSize());

    // Should use provided object, with original BitArray size
    BitArray array3 = new BitArray(200);
    array3 = matrix.getRow(2, array3);
    assertEquals(200, array3.getSize());

    for (int x = 0; x < 102; x++) {
      boolean on = (x & 0x03) == 0;
      assertEquals(on, array.get(x));
      assertEquals(on, array2.get(x));
      assertEquals(on, array3.get(x));
    }
  }

}
