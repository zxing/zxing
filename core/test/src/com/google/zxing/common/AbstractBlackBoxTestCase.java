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

package com.google.zxing.common;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageMonochromeBitmapSource;
import junit.framework.TestCase;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author srowen@google.com (Sean Owen)
 * @author dswitkin@google.com (Daniel Switkin)
 */
public abstract class AbstractBlackBoxTestCase extends TestCase {

  protected static final Hashtable<DecodeHintType, Object> TRY_HARDER_HINT;
  static {
    TRY_HARDER_HINT = new Hashtable<DecodeHintType, Object>();
    TRY_HARDER_HINT.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
  }

  private static final FilenameFilter IMAGE_NAME_FILTER = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      String lowerCase = name.toLowerCase();
      return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") ||
             lowerCase.endsWith(".gif") || lowerCase.endsWith(".png");
    }
  };

  private static class TestResult {
    private final int mustPassCount;
    private final float rotation;
    TestResult(int mustPassCount, float rotation) {
      this.mustPassCount = mustPassCount;
      this.rotation = rotation;
    }
    public int getMustPassCount() {
      return mustPassCount;
    }
    public float getRotation() {
      return rotation;
    }
  }

  private final File testBase;
  private final Reader barcodeReader;
  private final BarcodeFormat expectedFormat;
  private Vector<TestResult> testResults;

  protected AbstractBlackBoxTestCase(File testBase,
                                     Reader barcodeReader,
                                     BarcodeFormat expectedFormat) {
    this.testBase = testBase;
    this.barcodeReader = barcodeReader;
    this.expectedFormat = expectedFormat;
    testResults = new Vector<TestResult>();
  }

  /**
   * Adds a new test for the current directory of images.
   *
   * @param mustPassCount The number of images which must decode for the test to pass.
   * @param rotation The rotation in degrees clockwise to use for this test.
   */
  protected void addTest(int mustPassCount, float rotation) {
    testResults.add(new TestResult(mustPassCount, rotation));
  }

  protected File[] getImageFiles() {
    assertTrue("Please run from the 'core' directory", testBase.exists());
    return testBase.listFiles(IMAGE_NAME_FILTER);
  }

  protected Reader getReader() {
    return barcodeReader;
  }

  public void testBlackBox() throws IOException {
    assertFalse(testResults.isEmpty());

    File[] imageFiles = getImageFiles();
    int[] passedCounts = new int[testResults.size()];
    for (File testImage : imageFiles) {
      System.out.println("Starting " + testImage.getAbsolutePath());

      BufferedImage image = ImageIO.read(testImage);

      String testImageFileName = testImage.getName();
      File expectedTextFile = new File(testBase,
          testImageFileName.substring(0, testImageFileName.indexOf('.')) + ".txt");
      String expectedText = readFileAsString(expectedTextFile);

      for (int x = 0; x < testResults.size(); x++) {
        if (doTestOneImage(image, testResults.get(x).getRotation(), expectedText)) {
          passedCounts[x]++;
        }
      }
    }

    for (int x = 0; x < testResults.size(); x++) {
      System.out.println("Rotation " + testResults.get(x).getRotation() + " degrees: " + passedCounts[x] +
          " of " + imageFiles.length + " images passed (" + testResults.get(x).getMustPassCount() +
          " required)");
      assertTrue("Rotation " + testResults.get(x).getRotation() + " degrees: Too many images failed",
          passedCounts[x] >= testResults.get(x).getMustPassCount());
    }
  }

  private boolean doTestOneImage(BufferedImage image, float rotationInDegrees, String expectedText) {
    BufferedImage rotatedImage = rotateImage(image, rotationInDegrees);
    MonochromeBitmapSource source = new BufferedImageMonochromeBitmapSource(rotatedImage);
    Result result;
    try {
      result = barcodeReader.decode(source);
    } catch (ReaderException re) {
      System.out.println(re + " (rotation: " + rotationInDegrees + ')');
      return false;
    }

    if (!expectedFormat.equals(result.getBarcodeFormat())) {
      System.out.println("Format mismatch: expected '" + expectedFormat + "' but got '" +
          result.getBarcodeFormat() + "' (rotation: " + rotationInDegrees + ')');
      return false;
    }

    String resultText = result.getText();
    if (!expectedText.equals(resultText)) {
      System.out.println("Mismatch: expected '" + expectedText + "' but got '" + resultText +
          "' (rotation: " + rotationInDegrees + ')');
      return false;
    }

    // Try "try harder" mode
    try {
      result = barcodeReader.decode(source, TRY_HARDER_HINT);
    } catch (ReaderException re) {
      fail("Normal mode succeeded but \"try harder\" failed (rotation: " + rotationInDegrees + ')');
      return false;
    }
    if (!expectedFormat.equals(result.getBarcodeFormat())) {
      System.out.println("Try Harder Format mismatch: expected '" + expectedFormat + "' but got '" +
          result.getBarcodeFormat() + "' (rotation: " + rotationInDegrees + ')');
    } else if (!expectedText.equals(resultText)) {
      System.out.println("Try Harder Mismatch: expected '" + expectedText + "' but got '" +
          resultText + "' (rotation: " + rotationInDegrees + ')');
    }
    return true;
  }

  private static String readFileAsString(File file) throws IOException {
    StringBuilder result = new StringBuilder((int) file.length());
    InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF8");
    try {
      char[] buffer = new char[256];
      int charsRead;
      while ((charsRead = reader.read(buffer)) > 0) {
        result.append(buffer, 0, charsRead);
      }
    } finally {
      reader.close();
    }
    return result.toString();
  }

  protected static BufferedImage rotateImage(BufferedImage original, float degrees) {
    if (degrees == 0.0f) {
      return original;
    } else {
      AffineTransform at = new AffineTransform();
      at.rotate(Math.toRadians(degrees), original.getWidth() / 2.0f, original.getHeight() / 2.0f);
      BufferedImageOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
      return op.filter(original, null);
    }
  }

}