/*
 * Copyright 2016 ZXing authors
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

package com.google.zxing.multi.qrcode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.BufferedImageLuminanceSource;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.common.AbstractBlackBoxTestCase;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.MultipleBarcodeReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link QRCodeMultiReader}.
 */
public final class MultiQRCodeTestCase extends Assert {

  @Test
  public void testMultiQRCodes() throws Exception {
    // Very basic test for now
    Path testBase = AbstractBlackBoxTestCase.buildTestBase("src/test/resources/blackbox/multi-qrcode-1");

    Path testImage = testBase.resolve("1.png");
    BufferedImage image = ImageIO.read(testImage.toFile());
    LuminanceSource source = new BufferedImageLuminanceSource(image);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

    MultipleBarcodeReader reader = new QRCodeMultiReader();
    Result[] results = reader.decodeMultiple(bitmap);
    assertNotNull(results);
    assertEquals(4, results.length);

    Set<String> barcodeContents = new HashSet<>();
    for (Result result : results) {
      barcodeContents.add(result.getText());
      assertEquals(BarcodeFormat.QR_CODE, result.getBarcodeFormat());
      Map<ResultMetadataType,Object> metadata = result.getResultMetadata();
      assertNotNull(metadata);
    }
    Set<String> expectedContents = new HashSet<>();
    expectedContents.add("You earned the class a 5 MINUTE DANCE PARTY!!  Awesome!  Way to go!  Let's boogie!");
    expectedContents.add("You earned the class 5 EXTRA MINUTES OF RECESS!!  Fabulous!!  Way to go!!");
    expectedContents.add("You get to SIT AT MRS. SIGMON'S DESK FOR A DAY!!  Awesome!!  Way to go!! Guess I better clean up! :)");
    expectedContents.add("You get to CREATE OUR JOURNAL PROMPT FOR THE DAY!  Yay!  Way to go!  ");
    assertEquals(expectedContents, barcodeContents);
  }

}
