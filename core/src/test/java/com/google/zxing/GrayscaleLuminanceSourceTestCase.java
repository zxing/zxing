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
 * Tests {@link GrayscaleLuminanceSource}.
 */
public final class GrayscaleLuminanceSourceTestCase extends Assert {

  private static final GrayscaleLuminanceSource SOURCE =
      new GrayscaleLuminanceSource(3, 3, new byte[] {
        0x00, 0x7F, (byte) 0xFF,
        0x3F, 0x7F, 0x3F,
        0x3F, 0x7F, 0x3F});

  @Test
  public void testCrop() {
    assertTrue(SOURCE.isCropSupported());
    LuminanceSource cropped = SOURCE.crop(1, 1, 1, 1);
    assertEquals(1, cropped.getHeight());
    assertEquals(1, cropped.getWidth());
    assertArrayEquals(new byte[] { 0x7F }, cropped.getRow(0, null));
  }

  @Test
  public void testRotate() {
    assertTrue(SOURCE.isRotateSupported());
    assertArrayEquals(new byte[] { 0x00, 0x7F, (byte) 0xFF}, SOURCE.getRow(0, null));
    assertArrayEquals(new byte[] { 0x3F, 0x7F, 0x3F}, SOURCE.getRow(1, null));
    assertArrayEquals(new byte[] { 0x3F, 0x7F, 0x3F}, SOURCE.getRow(2, null));
    LuminanceSource rot90 = SOURCE.rotateCounterClockwise();
    assertArrayEquals(new byte[] { (byte) 0xFF, 0x3F, 0x3F}, rot90.getRow(0, null));
    assertArrayEquals(new byte[] { 0x7F, 0x7F, 0x7F}, rot90.getRow(1, null));
    assertArrayEquals(new byte[] { 0x00, 0x3F, 0x3F}, rot90.getRow(2, null));
    LuminanceSource rot180 = rot90.rotateCounterClockwise();
    assertArrayEquals(new byte[] { 0x3F, 0x7F, 0x3F}, rot180.getRow(0, null));
    assertArrayEquals(new byte[] { 0x3F, 0x7F, 0x3F}, rot180.getRow(1, null));
    assertArrayEquals(new byte[] { (byte) 0xFF, 0x7F, 0x00}, rot180.getRow(2, null));
    LuminanceSource rot270 = rot180.rotateCounterClockwise();
    assertArrayEquals(new byte[] { 0x3F, 0x3F, 0x00}, rot270.getRow(0, null));
    assertArrayEquals(new byte[] { 0x7F, 0x7F, 0x7F}, rot270.getRow(1, null));
    assertArrayEquals(new byte[] { 0x3F, 0x3F, (byte) 0xFF}, rot270.getRow(2, null));
    LuminanceSource rot360 = rot270.rotateCounterClockwise();
    assertArrayEquals(new byte[] { 0x00, 0x7F, (byte) 0xFF}, rot360.getRow(0, null));
    assertArrayEquals(new byte[] { 0x3F, 0x7F, 0x3F}, rot360.getRow(1, null));
    assertArrayEquals(new byte[] { 0x3F, 0x7F, 0x3F}, rot360.getRow(2, null));
    assertArrayEquals(SOURCE.getMatrix(), rot360.getMatrix());
  }

  @Test
  public void testRotateCropped() {
    assertTrue(SOURCE.isCropSupported());
    assertTrue(SOURCE.isRotateSupported());
    LuminanceSource cropped = SOURCE.crop(1, 1, 2, 2);
    assertArrayEquals(new byte[] { 0x7F, 0x3F}, cropped.getRow(0, null));
    assertArrayEquals(new byte[] { 0x7F, 0x3F}, cropped.getRow(1, null));
    LuminanceSource rot90 = cropped.rotateCounterClockwise();
    assertArrayEquals(new byte[] { 0x3F, 0x3F}, rot90.getRow(0, null));
    assertArrayEquals(new byte[] { 0x7F, 0x7F}, rot90.getRow(1, null));
    LuminanceSource rot180 = rot90.rotateCounterClockwise();
    assertArrayEquals(new byte[] { 0x3F, 0x7F}, rot180.getRow(0, null));
    assertArrayEquals(new byte[] { 0x3F, 0x7F}, rot180.getRow(1, null));
    LuminanceSource rot270 = rot180.rotateCounterClockwise();
    assertArrayEquals(new byte[] { 0x7F, 0x7F}, rot270.getRow(0, null));
    assertArrayEquals(new byte[] { 0x3F, 0x3F}, rot270.getRow(1, null));
    LuminanceSource rot360 = rot270.rotateCounterClockwise();
    assertArrayEquals(new byte[] { 0x7F, 0x3F}, rot360.getRow(0, null));
    assertArrayEquals(new byte[] { 0x7F, 0x3F}, rot360.getRow(1, null));
    assertArrayEquals(cropped.getMatrix(), rot360.getMatrix());
  }

  @Test
  public void testMatrix() {
    assertArrayEquals(new byte[] { 0x00, 0x7F, (byte) 0xFF, 0x3F, 0x7F, 0x3F, 0x3F, 0x7F, 0x3F },
                      SOURCE.getMatrix());
    LuminanceSource croppedFullWidth = SOURCE.crop(0, 1, 3, 2);
    assertArrayEquals(new byte[] { 0x3F, 0x7F, 0x3F, 0x3F, 0x7F, 0x3F },
                      croppedFullWidth.getMatrix());
    LuminanceSource croppedCorner = SOURCE.crop(1, 1, 2, 2);
    assertArrayEquals(new byte[] { 0x7F, 0x3F, 0x7F, 0x3F },
                      croppedCorner.getMatrix());
  }

  @Test
  public void testGetRow() {
    assertArrayEquals(new byte[] { 0x3F, 0x7F, 0x3F }, SOURCE.getRow(2, new byte[3]));
  }

  @Test
  public void testToString() {
    assertEquals("#+ \n#+#\n#+#\n", SOURCE.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullPixelArray() {
    // Test regression: null pixel array should throw IllegalArgumentException
    new RGBLuminanceSource(3, 3, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPixelArrayTooSmall() {
    // Test regression: pixel array smaller than width * height should throw IllegalArgumentException
    int width = 3;
    int height = 3;
    int[] pixels = new int[width * height - 1]; // One pixel short
    new RGBLuminanceSource(width, height, pixels);
  }

}
