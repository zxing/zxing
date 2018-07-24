/*
 * Copyright 2018 ZXing authors
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

import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests {@link CommandLineEncoder}.
 */
public final class CommandLineEncoderTestCase extends Assert {

  @Test
  public void testEncode() throws Exception {
    File out = File.createTempFile("qrcode", ".png");
    out.deleteOnExit();
    String[] args = {
        "--barcode_format", "QR_CODE",
        "--image_format", "PNG",
        "--output", out.toString(),
        "--width", "1", "--height", "1",
        "--error-correction-level", "L",
        "HELLO" };
    CommandLineEncoder.main(args);

    Path outPath = out.toPath();
    assertTrue(Files.exists(outPath));
    assertTrue(Files.size(outPath) > 0);
    BufferedImage image = ImageIO.read(out);
    assertEquals(33, image.getHeight());
    assertEquals(33, image.getWidth());
  }

}
