/*
 * Copyright 2026 ZXing authors
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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.BufferedImageLuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.HybridBinarizer;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Regression test for #1567: a noisy image with many spurious finder
 * pattern candidates must not cause the decoder to hang.
 */
public final class FinderPatternFinderPerformanceTestCase
    extends Assert {

  private static final String IMAGE_PATH =
      "src/test/resources/blackbox/triggeringImage1567.jpg";

  @Test(timeout = 5000)
  public void testNoisyImageDoesNotHang() throws Exception {
    Path file = Paths.get(IMAGE_PATH);
    if (!Files.exists(file)) {
      file = Paths.get("core/").resolve(IMAGE_PATH);
    }
    assertTrue("Test image not found", Files.exists(file));

    BufferedImage image = ImageIO.read(file.toFile());
    assertNotNull("Failed to load test image", image);

    BinaryBitmap bitmap = new BinaryBitmap(
        new HybridBinarizer(
            new BufferedImageLuminanceSource(image)));

    MultiFormatReader reader = new MultiFormatReader();
    try {
      reader.decode(bitmap);
    } catch (NotFoundException e) {
      // Expected: no QR code in the image
    }
    // If we get here without timeout, the test passes
  }
}
