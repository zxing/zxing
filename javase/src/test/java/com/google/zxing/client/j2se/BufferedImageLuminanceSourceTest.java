/*
 * Copyright 2024 ZXing authors
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

package com.google.zxing.client.j2se;

import org.junit.Assert;
import org.junit.Test;

import java.awt.image.BufferedImage;

/**
 * Tests {@link BufferedImageLuminanceSource}.
 */
public final class BufferedImageLuminanceSourceTest extends Assert {

  @Test
  public void testInvalidCropRectangleOnGrayImage() {
    // Create a small TYPE_BYTE_GRAY image (10x10)
    BufferedImage grayImage = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
    
    // Request a crop rectangle that exceeds the image dimensions
    // left + width = 5 + 10 = 15 > 10 (image width)
    // top + height = 5 + 10 = 15 > 10 (image height)
    Assert.assertThrows(IllegalArgumentException.class, () ->
        new BufferedImageLuminanceSource(grayImage, 5, 5, 10, 10)
    );
  }

  @Test
  public void testInvalidCropRectangleOnColorImage() {
    // Create a small TYPE_INT_ARGB image (10x10)
    BufferedImage colorImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
    
    // Same invalid crop rectangle
    Assert.assertThrows(IllegalArgumentException.class, () ->
        new BufferedImageLuminanceSource(colorImage, 5, 5, 10, 10)
    );
  }

  @Test
  public void testValidCropRectangleOnGrayImage() {
    BufferedImage grayImage = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
    
    // Valid crop rectangle that fits within the image
    BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(grayImage, 2, 2, 5, 5);
    assertEquals(5, source.getWidth());
    assertEquals(5, source.getHeight());
    
    // Should be able to read data without error
    byte[] row = source.getRow(0, null);
    assertEquals(5, row.length);
    
    byte[] matrix = source.getMatrix();
    assertEquals(25, matrix.length);
  }
}
