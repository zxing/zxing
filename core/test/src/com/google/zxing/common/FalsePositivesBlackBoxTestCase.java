/*
 * Copyright 2008 Google Inc.
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

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageMonochromeBitmapSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This test ensures that random, noisy, or unsupported barcode images do not decode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class FalsePositivesBlackBoxTestCase extends AbstractBlackBoxTestCase {

  // This number should be reduced as we get better at rejecting false positives.
  private static final int FALSE_POSITIVES_ALLOWED = 15;

  // Use the multiformat reader to evaluate all decoders in the system.
  public FalsePositivesBlackBoxTestCase() {
    super(new File("test/data/blackbox/falsepositives"), new MultiFormatReader(), null);
  }

  @Override
  public void testBlackBox() throws IOException {
    File[] imageFiles = getImageFiles();
    int falsePositives = 0;
    for (File testImage : imageFiles) {
      System.out.println("Starting " + testImage.getAbsolutePath());

      // Try all four rotations, since many of the test images don't have a notion of up, and we
      // want to be as robust as possible.
      BufferedImage image = ImageIO.read(testImage);
      if (image == null) {
        throw new IOException("Could not read image: " + testImage);
      }
      for (int x = 0; x < 4; x++) {
        if (!checkForFalsePositives(image, x * 90.0f)) {
          falsePositives++;
        }
      }
    }

    System.out.println("Found " + falsePositives + " false positives (" + FALSE_POSITIVES_ALLOWED +
        " max)");
    assertTrue("Too many false positives found", falsePositives <= FALSE_POSITIVES_ALLOWED);
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
    MonochromeBitmapSource source = new BufferedImageMonochromeBitmapSource(rotatedImage);
    Result result;
    try {
      result = getReader().decode(source);
      System.out.println("Found false positive: '" + result.getText() + "' with format '" +
          result.getBarcodeFormat() + "' (rotation: " + rotationInDegrees + ')');
      return false;
    } catch (ReaderException re) {
    }

    // Try "try harder" mode
    try {
      result = getReader().decode(source, TRY_HARDER_HINT);
      System.out.println("Try harder found false positive: '" + result.getText() + "' with format '" +
          result.getBarcodeFormat() + "' (rotation: " + rotationInDegrees + ')');
      return false;
    } catch (ReaderException re) {
    }
    return true;
  }

}
