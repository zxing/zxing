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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.BufferedImageLuminanceSource;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This abstract class looks for negative results, i.e. it only allows a certain number of false
 * positives in images which should not decode. This helps ensure that we are not too lenient.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public abstract class AbstractNegativeBlackBoxTestCase extends AbstractBlackBoxTestCase {

  private static class TestResult {
    private final int falsePositivesAllowed;
    private final float rotation;

    TestResult(int falsePositivesAllowed, float rotation) {
      this.falsePositivesAllowed = falsePositivesAllowed;
      this.rotation = rotation;
    }

    public int getFalsePositivesAllowed() {
      return falsePositivesAllowed;
    }

    public float getRotation() {
      return rotation;
    }
  }

  private final List<TestResult> testResults;

  // Use the multiformat reader to evaluate all decoders in the system.
  protected AbstractNegativeBlackBoxTestCase(String testBasePathSuffix) {
    super(testBasePathSuffix, new MultiFormatReader(), null);
    testResults = new ArrayList<TestResult>();
  }

  protected void addTest(int falsePositivesAllowed, float rotation) {
    testResults.add(new TestResult(falsePositivesAllowed, rotation));
  }

  @Override
  @Test
  public void testBlackBox() throws IOException {
    assertFalse(testResults.isEmpty());

    File[] imageFiles = getImageFiles();
    int[] falsePositives = new int[testResults.size()];
    for (File testImage : imageFiles) {
      System.out.printf("Starting %s\n", testImage.getAbsolutePath());

      BufferedImage image = ImageIO.read(testImage);
      if (image == null) {
        throw new IOException("Could not read image: " + testImage);
      }
      for (int x = 0; x < testResults.size(); x++) {
        TestResult testResult = testResults.get(x);
        if (!checkForFalsePositives(image, testResult.getRotation())) {
          falsePositives[x]++;
        }
      }
    }

    int totalFalsePositives = 0;
    int totalAllowed = 0;

    for (int x = 0; x < testResults.size(); x++) {
      TestResult testResult = testResults.get(x);
      totalFalsePositives += falsePositives[x];
      totalAllowed += testResult.getFalsePositivesAllowed();
    }

    if (totalFalsePositives < totalAllowed) {
      System.out.printf("  +++ Test too lax by %d images\n", totalAllowed - totalFalsePositives);
    } else if (totalFalsePositives > totalAllowed) {
      System.out.printf("  --- Test failed by %d images\n", totalFalsePositives - totalAllowed);
    }

    for (int x = 0; x < testResults.size(); x++) {
      TestResult testResult = testResults.get(x);
      System.out.printf("Rotation %d degrees: %d of %d images were false positives (%d allowed)\n",
                        (int) testResult.getRotation(), falsePositives[x], imageFiles.length,
                        testResult.getFalsePositivesAllowed());
      assertTrue("Rotation " + testResult.getRotation() + " degrees: Too many false positives found",
                 falsePositives[x] <= testResult.getFalsePositivesAllowed());
    }
  }

  /**
   * Make sure ZXing does NOT find a barcode in the image.
   *
   * @param image The image to test
   * @param rotationInDegrees The amount of rotation to apply
   * @return true if nothing found, false if a non-existant barcode was detected
   */
  private boolean checkForFalsePositives(BufferedImage image, float rotationInDegrees) {
    BufferedImage rotatedImage = rotateImage(image, rotationInDegrees);
    LuminanceSource source = new BufferedImageLuminanceSource(rotatedImage);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    Result result;
    try {
      result = getReader().decode(bitmap);
      System.out.printf("Found false positive: '%s' with format '%s' (rotation: %d)\n",
                        result.getText(), result.getBarcodeFormat(), (int) rotationInDegrees);
      return false;
    } catch (ReaderException re) {
    }

    // Try "try harder" getMode
    Map<DecodeHintType,Object> hints = new EnumMap<DecodeHintType,Object>(DecodeHintType.class);
    hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    try {
      result = getReader().decode(bitmap, hints);
      System.out.printf("Try harder found false positive: '%s' with format '%s' (rotation: %d)\n", 
                        result.getText(), result.getBarcodeFormat(), (int) rotationInDegrees);
      return false;
    } catch (ReaderException re) {
    }
    return true;
  }

}
