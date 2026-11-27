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

import java.util.Arrays;

/**
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class BitMatrixTestCase extends Assert {

  private static final int[] BIT_MATRIX_POINTS = { 1, 2, 2, 0, 3, 1 };

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
  public void testEnclosing() {
    BitMatrix matrix = new BitMatrix(5);
    assertNull(matrix.getEnclosingRectangle());
    matrix.setRegion(1, 1, 1, 1);
    assertArrayEquals(new int[] { 1, 1, 1, 1 }, matrix.getEnclosingRectangle());
    matrix.setRegion(1, 1, 3, 2);
    assertArrayEquals(new int[] { 1, 1, 3, 2 }, matrix.getEnclosingRectangle());
    matrix.setRegion(0, 0, 5, 5);
    assertArrayEquals(new int[] { 0, 0, 5, 5 }, matrix.getEnclosingRectangle());
  }

  @Test
  public void testOnBit() {
    BitMatrix matrix = new BitMatrix(5);
    assertNull(matrix.getTopLeftOnBit());
    assertNull(matrix.getBottomRightOnBit());
    matrix.setRegion(1, 1, 1, 1);
    assertArrayEquals(new int[] { 1, 1 }, matrix.getTopLeftOnBit());
    assertArrayEquals(new int[] { 1, 1 }, matrix.getBottomRightOnBit());
    matrix.setRegion(1, 1, 3, 2);
    assertArrayEquals(new int[] { 1, 1 }, matrix.getTopLeftOnBit());
    assertArrayEquals(new int[] { 3, 2 }, matrix.getBottomRightOnBit());
    matrix.setRegion(0, 0, 5, 5);
    assertArrayEquals(new int[] { 0, 0 }, matrix.getTopLeftOnBit());
    assertArrayEquals(new int[] { 4, 4 }, matrix.getBottomRightOnBit());

    BitMatrix wide = new BitMatrix(75, 41);
    wide.set(31, 5);
    wide.set(64, 30);
    assertArrayEquals(new int[] { 31, 5 }, wide.getTopLeftOnBit());
    assertArrayEquals(new int[] { 64, 30 }, wide.getBottomRightOnBit());
    assertArrayEquals(new int[] { 31, 5, 34, 26 }, wide.getEnclosingRectangle());
    wide.set(74, 30);
    assertArrayEquals(new int[] { 74, 30 }, wide.getBottomRightOnBit());
    assertArrayEquals(new int[] { 31, 5, 44, 26 }, wide.getEnclosingRectangle());
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

    // Should use provided object, with original BitArray size, clearing trailing bits
    BitArray array3 = new BitArray(200);
    array3.setRange(0, 200);
    array3 = matrix.getRow(2, array3);
    assertEquals(200, array3.getSize());

    for (int x = 0; x < 102; x++) {
      boolean on = (x & 0x03) == 0;
      assertEquals(on, array.get(x));
      assertEquals(on, array2.get(x));
      assertEquals(on, array3.get(x));
    }
    assertTrue(array3.isRange(102, 200, false));
  }

  @Test
  public void testRotate90Simple() {
    BitMatrix matrix = new BitMatrix(3, 3);
    matrix.set(0, 0);
    matrix.set(0, 1);
    matrix.set(1, 2);
    matrix.set(2, 1);

    matrix.rotate90();

    assertTrue(matrix.get(0, 2));
    assertTrue(matrix.get(1, 2));
    assertTrue(matrix.get(2, 1));
    assertTrue(matrix.get(1, 0));
  }

  @Test
  public void testRotate180Simple() {
    BitMatrix matrix = new BitMatrix(3, 3);
    matrix.set(0, 0);
    matrix.set(0, 1);
    matrix.set(1, 2);
    matrix.set(2, 1);

    matrix.rotate180();

    assertTrue(matrix.get(2, 2));
    assertTrue(matrix.get(2, 1));
    assertTrue(matrix.get(1, 0));
    assertTrue(matrix.get(0, 1));
  }

  @Test
  public void testRotate180() {
    testRotate180(7, 4);
    testRotate180(7, 5);
    testRotate180(8, 4);
    testRotate180(8, 5);
  }

  @Test
  public void testRotateMultiTileAndEdgeDimensions() {
    int[][] dims = {
      {1, 1}, {1, 33}, {33, 1}, {32, 32}, {32, 65}, {65, 32}, {75, 41}, {100, 96}
    };
    for (int[] dim : dims) {
      int w = dim[0];
      int h = dim[1];
      BitMatrix original = new BitMatrix(w, h);
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          if (((x * 37 + y * 17 + (x ^ y)) & 5) == 0) {
            original.set(x, y);
          }
        }
      }

      BitMatrix r90 = original.clone();
      r90.rotate(90);
      assertEquals(h, r90.getWidth());
      assertEquals(w, r90.getHeight());
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          assertEquals(original.get(x, y), r90.get(y, w - 1 - x));
        }
      }

      BitMatrix r180 = original.clone();
      r180.rotate(180);
      assertEquals(w, r180.getWidth());
      assertEquals(h, r180.getHeight());
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          assertEquals(original.get(x, y), r180.get(w - 1 - x, h - 1 - y));
        }
      }

      BitMatrix r270 = original.clone();
      r270.rotate(270);
      assertEquals(h, r270.getWidth());
      assertEquals(w, r270.getHeight());
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          assertEquals(original.get(x, y), r270.get(h - 1 - y, x));
        }
      }

      // Check composition invariants and clean padding bits (via equals)
      BitMatrix via90Then180 = r90.clone();
      via90Then180.rotate180();
      assertEquals(r270, via90Then180);

      BitMatrix via180Then90 = r180.clone();
      via180Then90.rotate90();
      assertEquals(r270, via180Then90);

      BitMatrix roundTrip = r270.clone();
      roundTrip.rotate90();
      assertEquals(original, roundTrip);
    }
  }

  @Test
  public void testParse() {
    BitMatrix emptyMatrix = new BitMatrix(3, 3);
    BitMatrix fullMatrix = new BitMatrix(3, 3);
    fullMatrix.setRegion(0, 0, 3, 3);
    BitMatrix centerMatrix = new BitMatrix(3, 3);
    centerMatrix.setRegion(1, 1, 1, 1);
    BitMatrix emptyMatrix24 = new BitMatrix(2, 4);

    assertEquals(emptyMatrix, BitMatrix.parse("   \n   \n   \n", "x", " "));
    assertEquals(emptyMatrix, BitMatrix.parse("   \n   \r\r\n   \n\r", "x", " "));
    assertEquals(emptyMatrix, BitMatrix.parse("   \n   \n   ", "x", " "));

    assertEquals(fullMatrix, BitMatrix.parse("xxx\nxxx\nxxx\n", "x", " "));

    assertEquals(centerMatrix, BitMatrix.parse("   \n x \n   \n", "x", " "));
    assertEquals(centerMatrix, BitMatrix.parse("      \n  x   \n      \n", "x ", "  "));
    try {
      assertEquals(centerMatrix, BitMatrix.parse("   \n xy\n   \n", "x", " "));
      fail();
    } catch (IllegalArgumentException ex) {
      // good
    }

    assertEquals(emptyMatrix24, BitMatrix.parse("  \n  \n  \n  \n", "x", " "));

    assertEquals(centerMatrix, BitMatrix.parse(centerMatrix.toString("x", "."), "x", "."));
  }

  @Test
  public void testParseBoolean() {
    BitMatrix emptyMatrix = new BitMatrix(3, 3);
    BitMatrix fullMatrix = new BitMatrix(3, 3);
    fullMatrix.setRegion(0, 0, 3, 3);
    BitMatrix centerMatrix = new BitMatrix(3, 3);
    centerMatrix.setRegion(1, 1, 1, 1);

    boolean[][] matrix = new boolean[3][3];
    assertEquals(emptyMatrix, BitMatrix.parse(matrix));
    matrix[1][1] = true;
    assertEquals(centerMatrix, BitMatrix.parse(matrix));
    for (boolean[] arr : matrix) {
      Arrays.fill(arr, true);
    }
    assertEquals(fullMatrix, BitMatrix.parse(matrix));
  }

  @Test
  public void testUnset() {
    BitMatrix emptyMatrix = new BitMatrix(3, 3);
    BitMatrix matrix = emptyMatrix.clone();
    matrix.set(1, 1);
    assertNotEquals(emptyMatrix, matrix);
    matrix.unset(1, 1);
    assertEquals(emptyMatrix, matrix);
    matrix.unset(1, 1);
    assertEquals(emptyMatrix, matrix);
  }

  @Test
  public void testXOR() {
    BitMatrix emptyMatrix = new BitMatrix(3, 3);
    BitMatrix fullMatrix = new BitMatrix(3, 3);
    fullMatrix.setRegion(0, 0, 3, 3);
    BitMatrix centerMatrix = new BitMatrix(3, 3);
    centerMatrix.setRegion(1, 1, 1, 1);
    BitMatrix invertedCenterMatrix = fullMatrix.clone();
    invertedCenterMatrix.unset(1, 1);
    BitMatrix badMatrix = new BitMatrix(4, 4);

    testXOR(emptyMatrix, emptyMatrix, emptyMatrix);
    testXOR(emptyMatrix, centerMatrix, centerMatrix);
    testXOR(emptyMatrix, fullMatrix, fullMatrix);

    testXOR(centerMatrix, emptyMatrix, centerMatrix);
    testXOR(centerMatrix, centerMatrix, emptyMatrix);
    testXOR(centerMatrix, fullMatrix, invertedCenterMatrix);

    testXOR(invertedCenterMatrix, emptyMatrix, invertedCenterMatrix);
    testXOR(invertedCenterMatrix, centerMatrix, fullMatrix);
    testXOR(invertedCenterMatrix, fullMatrix, centerMatrix);

    testXOR(fullMatrix, emptyMatrix, fullMatrix);
    testXOR(fullMatrix, centerMatrix, invertedCenterMatrix);
    testXOR(fullMatrix, fullMatrix, emptyMatrix);

    try {
      emptyMatrix.clone().xor(badMatrix);
      fail();
    } catch (IllegalArgumentException ex) {
      // good
    }

    try {
      badMatrix.clone().xor(emptyMatrix);
      fail();
    } catch (IllegalArgumentException ex) {
      // good
    }
  }

  public static String matrixToString(BitMatrix result) {
    assertEquals(1, result.getHeight());
    StringBuilder builder = new StringBuilder(result.getWidth());
    for (int i = 0; i < result.getWidth(); i++) {
      builder.append(result.get(i, 0) ? '1' : '0');
    }
    return builder.toString();
  }

  private static void testXOR(BitMatrix dataMatrix, BitMatrix flipMatrix, BitMatrix expectedMatrix) {
    BitMatrix matrix = dataMatrix.clone();
    matrix.xor(flipMatrix);
    assertEquals(expectedMatrix, matrix);
  }

  private static void testRotate180(int width, int height) {
    BitMatrix input = getInput(width, height);
    input.rotate180();
    BitMatrix expected = getExpected(width, height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        assertEquals("(" + x + ',' + y + ')', expected.get(x, y), input.get(x, y));
      }
    }
  }

  private static BitMatrix getExpected(int width, int height) {
    BitMatrix result = new BitMatrix(width, height);
    for (int i = 0; i < BIT_MATRIX_POINTS.length; i += 2) {
      result.set(width - 1 - BIT_MATRIX_POINTS[i], height - 1 - BIT_MATRIX_POINTS[i + 1]);
    }
    return result;
  }

  private static BitMatrix getInput(int width, int height) {
    BitMatrix result = new BitMatrix(width, height);
    for (int i = 0; i < BIT_MATRIX_POINTS.length; i += 2) {
      result.set(BIT_MATRIX_POINTS[i], BIT_MATRIX_POINTS[i + 1]);
    }
    return result;
  }

  @Test
  public void testFlipDoesNotCorruptPaddingBits() {
    // Width 5 means rowSize = 1, so last int has 27 padding bits
    BitMatrix matrix = new BitMatrix(5, 5);
    // Flip all bits in empty matrix
    matrix.flip();

    // All logical bits should be true
    for (int y = 0; y < 5; y++) {
      for (int x = 0; x < 5; x++) {
        assertTrue("Bit (" + x + "," + y + ") should be true after flip()", matrix.get(x, y));
      }
    }

    // getTopLeftOnBit should return (0, 0), not something beyond width due to padding bits
    int[] topLeft = matrix.getTopLeftOnBit();
    assertNotNull(topLeft);
    assertArrayEquals(new int[] { 0, 0 }, topLeft);

    // getBottomRightOnBit should return (4, 4), not (31, 4) or worse due to padding
    int[] bottomRight = matrix.getBottomRightOnBit();
    assertNotNull(bottomRight);
    assertArrayEquals(new int[] { 4, 4 }, bottomRight);

    // getEnclosingRectangle should return (0, 0, 5, 5), not (0, 0, 32, 5) due to padding
    int[] enclosing = matrix.getEnclosingRectangle();
    assertNotNull(enclosing);
    assertArrayEquals(new int[] { 0, 0, 5, 5 }, enclosing);

    // Double flip should restore the matrix to all-zeros, equal to a fresh matrix
    matrix.flip();
    assertEquals(new BitMatrix(5, 5), matrix);
  }

  @Test
  public void testGetNextSetAndUnset() {
    BitMatrix matrix = new BitMatrix(75, 4);
    // Row 0: all unset
    assertEquals(75, matrix.getNextSet(0, 0));
    assertEquals(75, matrix.getNextSet(74, 0));
    assertEquals(75, matrix.getNextSet(75, 0));
    assertEquals(0, matrix.getNextUnset(0, 0));
    assertEquals(31, matrix.getNextUnset(31, 0));
    assertEquals(32, matrix.getNextUnset(32, 0));
    assertEquals(74, matrix.getNextUnset(74, 0));
    assertEquals(75, matrix.getNextUnset(75, 0));

    // Row 1: specific runs spanning 32-bit word boundaries (bits 0..1, 31..33, 63..74)
    matrix.setRegion(0, 1, 2, 1);
    matrix.setRegion(31, 1, 3, 1);
    matrix.setRegion(63, 1, 12, 1);
    assertEquals(0, matrix.getNextSet(0, 1));
    assertEquals(1, matrix.getNextSet(1, 1));
    assertEquals(31, matrix.getNextSet(2, 1));
    assertEquals(31, matrix.getNextSet(31, 1));
    assertEquals(32, matrix.getNextSet(32, 1));
    assertEquals(33, matrix.getNextSet(33, 1));
    assertEquals(63, matrix.getNextSet(34, 1));
    assertEquals(64, matrix.getNextSet(64, 1));
    assertEquals(74, matrix.getNextSet(74, 1));
    assertEquals(75, matrix.getNextSet(75, 1));

    assertEquals(2, matrix.getNextUnset(0, 1));
    assertEquals(2, matrix.getNextUnset(1, 1));
    assertEquals(2, matrix.getNextUnset(2, 1));
    assertEquals(34, matrix.getNextUnset(31, 1));
    assertEquals(34, matrix.getNextUnset(32, 1));
    assertEquals(34, matrix.getNextUnset(33, 1));
    assertEquals(34, matrix.getNextUnset(34, 1));
    assertEquals(75, matrix.getNextUnset(63, 1));
    assertEquals(75, matrix.getNextUnset(74, 1));
    assertEquals(75, matrix.getNextUnset(75, 1));

    // Row 2: all set (test clamping at width < 32 * rowSize)
    matrix.setRegion(0, 2, 75, 1);
    assertEquals(0, matrix.getNextSet(0, 2));
    assertEquals(74, matrix.getNextSet(74, 2));
    assertEquals(75, matrix.getNextUnset(0, 2));
    assertEquals(75, matrix.getNextUnset(32, 2));
    assertEquals(75, matrix.getNextUnset(74, 2));
  }

}
