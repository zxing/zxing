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
 * Tests {@link RGBLuminanceSource}.
 */
public final class RGBLuminanceSourceTestCase extends Assert {

  private static final RGBLuminanceSource SOURCE = new RGBLuminanceSource(3, 3, new int[] {
      0x000000, 0x7F7F7F, 0xFFFFFF,
      0xFF0000, 0x00FF00, 0x0000FF,
      0x0000FF, 0x00FF00, 0xFF0000});

  @Test
  public void testCrop() {
    assertTrue(SOURCE.isCropSupported());
    LuminanceSource cropped = SOURCE.crop(1, 1, 1, 1);
    assertEquals(1, cropped.getHeight());
    assertEquals(1, cropped.getWidth());
    assertArrayEquals(new byte[] { 0x7F }, cropped.getRow(0, null));
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

}
