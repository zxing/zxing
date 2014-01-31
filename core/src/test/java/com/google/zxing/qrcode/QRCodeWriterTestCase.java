/*
 * Copyright 2008 ZXing authors
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
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported and expanded from C++
 */
public final class QRCodeWriterTestCase extends Assert {

  private static final Path BASE_IMAGE_PATH = Paths.get("src/test/resources/golden/qrcode/");

  private static BufferedImage loadImage(String fileName) throws IOException {
    Path file = BASE_IMAGE_PATH.resolve(fileName);
    if (!Files.exists(file)) {
      // try starting with 'core' since the test base is often given as the project root
      file = Paths.get("core/").resolve(BASE_IMAGE_PATH).resolve(fileName);
    }
    assertTrue("Please download and install test images, and run from the 'core' directory", Files.exists(file));
    return ImageIO.read(file.toFile());
  }

  // In case the golden images are not monochromatic, convert the RGB values to greyscale.
  private static BitMatrix createMatrixFromImage(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    int[] pixels = new int[width * height];
    image.getRGB(0, 0, width, height, pixels, 0, width);

    BitMatrix matrix = new BitMatrix(width, height);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pixel = pixels[y * width + x];
        int luminance = (306 * ((pixel >> 16) & 0xFF) +
            601 * ((pixel >> 8) & 0xFF) +
            117 * (pixel & 0xFF)) >> 10;
        if (luminance <= 0x7F) {
          matrix.set(x, y);
        }
      }
    }
    return matrix;
  }

  @Test
  public void testQRCodeWriter() throws WriterException {
    // The QR should be multiplied up to fit, with extra padding if necessary
    int bigEnough = 256;
    Writer writer = new QRCodeWriter();
    BitMatrix matrix = writer.encode("http://www.google.com/", BarcodeFormat.QR_CODE, bigEnough,
        bigEnough, null);
    assertNotNull(matrix);
    assertEquals(bigEnough, matrix.getWidth());
    assertEquals(bigEnough, matrix.getHeight());

    // The QR will not fit in this size, so the matrix should come back bigger
    int tooSmall = 20;
    matrix = writer.encode("http://www.google.com/", BarcodeFormat.QR_CODE, tooSmall,
        tooSmall, null);
    assertNotNull(matrix);
    assertTrue(tooSmall < matrix.getWidth());
    assertTrue(tooSmall < matrix.getHeight());

    // We should also be able to handle non-square requests by padding them
    int strangeWidth = 500;
    int strangeHeight = 100;
    matrix = writer.encode("http://www.google.com/", BarcodeFormat.QR_CODE, strangeWidth,
        strangeHeight, null);
    assertNotNull(matrix);
    assertEquals(strangeWidth, matrix.getWidth());
    assertEquals(strangeHeight, matrix.getHeight());
  }

  private static void compareToGoldenFile(String contents,
                                          ErrorCorrectionLevel ecLevel,
                                          int resolution,
                                          String fileName) throws WriterException, IOException {

    BufferedImage image = loadImage(fileName);
    assertNotNull(image);
    BitMatrix goldenResult = createMatrixFromImage(image);
    assertNotNull(goldenResult);

    Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.ERROR_CORRECTION, ecLevel);
    Writer writer = new QRCodeWriter();
    BitMatrix generatedResult = writer.encode(contents, BarcodeFormat.QR_CODE, resolution,
        resolution, hints);

    assertEquals(resolution, generatedResult.getWidth());
    assertEquals(resolution, generatedResult.getHeight());
    assertEquals(goldenResult, generatedResult);
  }

  // Golden images are generated with "qrcode_sample.cc". The images are checked with both eye balls
  // and cell phones. We expect pixel-perfect results, because the error correction level is known,
  // and the pixel dimensions matches exactly. 
  @Test
  public void testRegressionTest() throws Exception {
    compareToGoldenFile("http://www.google.com/", ErrorCorrectionLevel.M, 99,
        "renderer-test-01.png");
  }

}
