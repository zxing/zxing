/*
 * Copyright 2007 Google Inc.
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

import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * <p>Simply attempts to decode the barcode in the image indicated by the single argument
 * to this program, which may be file or a URI. The raw text is printed.</p>
 *
 * @author srowen@google.com (Sean Owen), dswitkin@google.com (Daniel Switkin)
 */
public final class CommandLineRunner {

  private CommandLineRunner() {
  }

  public static void main(String[] args) throws Exception {
    File inputFile = new File(args[0]);
    if (inputFile.exists()) {
      if (inputFile.isDirectory()) {
        int successful = 0;
        for (File input : inputFile.listFiles()) {
          if (decode(input.toURI())) {
            successful++;
          }
        }
        System.out.println("Decoded " + successful + " files successfully");
      } else {
        decode(inputFile.toURI());
      }
    } else {
      decode(new URI(args[0]));
    }
  }

  private static boolean decode(URI uri) throws IOException {
    BufferedImage image = ImageIO.read(uri.toURL());
    if (image == null) {
      System.err.println(uri.toString() + ": Could not load image");
      return false;
    }
    try {
      BufferedImageMonochromeBitmapSource source = new BufferedImageMonochromeBitmapSource(image);
      String result = new MultiFormatReader().decode(source).getText();
      System.out.println(uri.toString() + ": " + result);
      return true;
    } catch (ReaderException e) {
      System.out.println(uri.toString() + ": No barcode found");
      return false;
    }
  }

}
