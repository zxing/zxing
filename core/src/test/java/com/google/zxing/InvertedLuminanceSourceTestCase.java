/*
 * Copyright 2020 ZXing authors
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

import java.awt.image.BufferedImage;

/**
 * Tests {@link InvertedLuminanceSource}.
 */
public final class InvertedLuminanceSourceTestCase extends Assert {

  @Test
  public void testInverted() {
    BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, 0xFFFFFF);
    LuminanceSource source = new BufferedImageLuminanceSource(image);
    assertArrayEquals(new byte[] { (byte) 0xFF, 0 }, source.getRow(0, null));
    LuminanceSource inverted = new InvertedLuminanceSource(source);
    assertArrayEquals(new byte[] { 0, (byte) 0xFF }, inverted.getRow(0, null));
  }

}
