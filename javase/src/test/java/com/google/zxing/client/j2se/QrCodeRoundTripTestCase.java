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

package com.google.zxing.client.j2se;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.Assert;
import org.junit.Test;

import java.awt.image.BufferedImage;

/**
 * Regression test for #1976: QR codes encoded at native size
 * (width=0, height=0) must round-trip through decode.
 */
public class QrCodeRoundTripTestCase extends Assert {

  @Test
  public void testRoundTripNativeSize() throws Exception {
    String payload = "test-payload-uuid-1234";

    QRCodeWriter writer = new QRCodeWriter();
    BitMatrix matrix =
        writer.encode(payload, BarcodeFormat.QR_CODE, 0, 0);

    BufferedImage image =
        MatrixToImageWriter.toBufferedImage(matrix);
    BufferedImageLuminanceSource source =
        new BufferedImageLuminanceSource(image);
    BinaryBitmap bitmap =
        new BinaryBitmap(new HybridBinarizer(source));
    MultiFormatReader reader = new MultiFormatReader();

    assertEquals(payload, reader.decode(bitmap).getText());
  }
}
