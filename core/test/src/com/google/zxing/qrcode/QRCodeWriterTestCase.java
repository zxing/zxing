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
import com.google.zxing.WriterException;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import junit.framework.TestCase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported and expanded from C++
 */
public final class QRCodeWriterTestCase extends TestCase {

  private static final String BASE_IMAGE_PATH = "test/data/golden/qrcode/";

  private static BufferedImage loadImage(String fileName) {
    try {
      File file = new File(BASE_IMAGE_PATH + fileName);
      assertTrue("Please run from the 'core' directory", file.exists());
      return ImageIO.read(file);
    } catch (IOException e) {
      return null;
    }
  }

  // In case the golden images are not monochromatic, convert the RGB values to greyscale.
  private static ByteMatrix createMatrixFromImage(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    int[] pixels = new int[width * height];
    image.getRGB(0, 0, width, height, pixels, 0, width);

    ByteMatrix matrix = new ByteMatrix(height, width);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pixel = pixels[y * width + x];
        int luminance = (306 * ((pixel >> 16) & 0xFF) +
            601 * ((pixel >> 8) & 0xFF) +
            117 * (pixel & 0xFF)) >> 10;
        matrix.set(y, x, luminance);
      }
    }
    return matrix;
  }

  public void testQRCodeWriter() throws WriterException {
    // The QR should be multiplied up to fit, with extra padding if necessary
    int bigEnough = 256;
    QRCodeWriter writer = new QRCodeWriter();
    ByteMatrix matrix = writer.encode("http://www.google.com/", BarcodeFormat.QR_CODE, bigEnough,
        bigEnough, null);
    assertNotNull(matrix);
    assertEquals(bigEnough, matrix.width());
    assertEquals(bigEnough, matrix.height());

    // The QR will not fit in this size, so the matrix should come back bigger
    int tooSmall = 20;
    matrix = writer.encode("http://www.google.com/", BarcodeFormat.QR_CODE, tooSmall,
        tooSmall, null);
    assertNotNull(matrix);
    assertTrue(tooSmall < matrix.width());
    assertTrue(tooSmall < matrix.height());

    // We should also be able to handle non-square requests by padding them
    int strangeWidth = 500;
    int strangeHeight = 100;
    matrix = writer.encode("http://www.google.com/", BarcodeFormat.QR_CODE, strangeWidth,
        strangeHeight, null);
    assertNotNull(matrix);
    assertEquals(strangeWidth, matrix.width());
    assertEquals(strangeHeight, matrix.height());
  }

  private static void compareToGoldenFile(String contents, ErrorCorrectionLevel ecLevel,
      int resolution, String fileName) throws WriterException {

    BufferedImage image = loadImage(fileName);
    assertNotNull(image);
    ByteMatrix goldenResult = createMatrixFromImage(image);
    assertNotNull(goldenResult);

    QRCodeWriter writer = new QRCodeWriter();
    Hashtable<EncodeHintType,Object> hints = new Hashtable<EncodeHintType,Object>();
    hints.put(EncodeHintType.ERROR_CORRECTION, ecLevel);
    ByteMatrix generatedResult = writer.encode(contents, BarcodeFormat.QR_CODE, resolution,
        resolution, hints);

    assertEquals("Width should be " + resolution + ", but was " + generatedResult.width(),
        resolution, generatedResult.width());
    assertEquals("Height should be " + resolution + ", but was " + generatedResult.height(),
        resolution, generatedResult.height());
    assertTrue("Expected " + goldenResult.toString() + " but got " + generatedResult.toString(),
        Arrays.deepEquals(goldenResult.getArray(), generatedResult.getArray()));
  }

  // Golden images are generated with "qrcode_sample.cc". The images are checked with both eye balls
  // and cell phones. We expect pixel-perfect results, because the error correction level is known,
  // and the pixel dimensions matches exactly.
  public void testRegressionTest() throws WriterException {
    compareToGoldenFile("http://www.google.com/", ErrorCorrectionLevel.M, 99,
        "renderer-test-01.png");

    compareToGoldenFile("12345", ErrorCorrectionLevel.L, 58, "renderer-test-02.png");

    // Test in Katakana in Shift_JIS.
    // TODO: this test is bogus now that byte mode has been basically fixed to assuming ISO-8859-1 encoding
    //  The real solution is to implement Kanji mode, in which case the golden file will be wrong again
    /*
    compareToGoldenFile(
        new String(new byte[] {(byte)0x83, 0x65, (byte)0x83, 0x58, (byte)0x83, 0x67}, "Shift_JIS"),
        ErrorCorrectionLevel.H, 145,
        "renderer-test-03.png");
     */
  }

}
