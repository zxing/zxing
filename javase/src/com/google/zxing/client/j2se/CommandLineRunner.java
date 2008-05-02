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

import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.MonochromeBitmapSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Hashtable;

/**
 * <p>This simple command line utility decodes files, directories of files, or URIs which are passed
 * as arguments. By default it uses the normal decoding algorithms, but you can pass --try_harder to
 * request that hint. The raw text of each barcode is printed, and when running against directories,
 * summary statistics are also displayed.</p>
 *
 * @author srowen@google.com (Sean Owen), dswitkin@google.com (Daniel Switkin)
 */
public final class CommandLineRunner {

  private CommandLineRunner() {
  }

  public static void main(String[] args) throws Exception {
    Hashtable<DecodeHintType, Object> hints = null;
    for (String arg : args) {
      if ("--try_harder".equals(arg)) {
        hints = new Hashtable<DecodeHintType, Object>(3);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
      } else if (arg.startsWith("--")) {
        System.out.println("Unknown command line option " + arg);
        return;
      }
    }
    for (String arg : args) {
      if (!arg.startsWith("--")) {
        decodeOneArgument(arg, hints);
      }
    }
  }

  private static void decodeOneArgument(String argument, Hashtable<DecodeHintType, Object> hints) throws Exception {
    File inputFile = new File(argument);
    if (inputFile.exists()) {
      if (inputFile.isDirectory()) {
        int successful = 0;
        int total = 0;
        for (File input : inputFile.listFiles()) {
          String filename = input.getName().toLowerCase();
          // Skip hidden files and text files (the latter is found in the blackbox tests).
          if (filename.startsWith(".") || filename.endsWith(".txt")) {
            continue;
          }
          if (decode(input.toURI(), hints)) {
            successful++;
          }
          total++;
        }
        System.out.println("\nDecoded " + successful + " files out of " + total +
            " successfully (" + (successful * 100 / total) + "%)\n");
      } else {
        decode(inputFile.toURI(), hints);
      }
    } else {
      decode(new URI(argument), hints);
    }
  }

  private static boolean decode(URI uri, Hashtable<DecodeHintType, Object> hints) throws IOException {
    BufferedImage image = ImageIO.read(uri.toURL());
    if (image == null) {
      System.err.println(uri.toString() + ": Could not load image");
      return false;
    }
    try {
      MonochromeBitmapSource source = new BufferedImageMonochromeBitmapSource(image);
      String result = new MultiFormatReader().decode(source, hints).getText();
      System.out.println(uri.toString() + ": " + result);
      return true;
    } catch (ReaderException e) {
      System.out.println(uri.toString() + ": No barcode found");
      return false;
    }
  }

}
