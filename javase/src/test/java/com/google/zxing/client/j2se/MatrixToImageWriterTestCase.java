/*
 * Copyright 2017 ZXing authors
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

import com.google.zxing.common.BitMatrix;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests {@link MatrixToImageWriter}.
 */
public final class MatrixToImageWriterTestCase extends Assert {
  
  @Test
  public void testBlackAndWhite() throws Exception {
    doTest(new MatrixToImageConfig());
  }

  @Test
  public void testColor() throws Exception {
    doTest(new MatrixToImageConfig(0xFF102030, 0xFF405060));
  }

  @Test
  public void testAlpha() throws Exception {
    doTest(new MatrixToImageConfig(0x7F102030, 0x7F405060));
  }
  
  private static void doTest(MatrixToImageConfig config) throws IOException {
    doTestFormat("tiff", config);
    doTestFormat("png", config);
  }

  private static void doTestFormat(String format, MatrixToImageConfig config) throws IOException {
    int width = 2;
    int height = 3;
    BitMatrix matrix = new BitMatrix(width, height);
    matrix.set(0, 0);
    matrix.set(0, 1);
    matrix.set(1, 2);
    
    BufferedImage newImage;
    Path tempFile = Files.createTempFile(null, "." + format);
    try {
      MatrixToImageWriter.writeToPath(matrix, format, tempFile, config);
      assertTrue(Files.size(tempFile) > 0);
      newImage = ImageIO.read(tempFile.toFile());
    } finally {
      Files.delete(tempFile);
    }

    assertEquals(width, newImage.getWidth());
    assertEquals(height, newImage.getHeight());
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int expected = matrix.get(x, y) ? config.getPixelOnColor() : config.getPixelOffColor();
        int actual = newImage.getRGB(x, y);
        assertEquals(
            "At " + x + "," + y + " expected " + Integer.toHexString(expected) + 
            " but got " + Integer.toHexString(actual),
            expected, actual);
      }
    }
  }
  
}
