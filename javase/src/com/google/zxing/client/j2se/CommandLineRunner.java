/*
 * Copyright 2007 ZXing authors
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
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Hashtable;

/**
 * <p>This simple command line utility decodes files, directories of files, or URIs which are passed
 * as arguments. By default it uses the normal decoding algorithms, but you can pass --try_harder to
 * request that hint. The raw text of each barcode is printed, and when running against directories,
 * summary statistics are also displayed.</p>
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CommandLineRunner {

  private CommandLineRunner() {
  }

  public static void main(String[] args) throws Exception {
    Hashtable<DecodeHintType, Object> hints = null;
    boolean dumpResults = false;
    for (String arg : args) {
      if ("--try_harder".equals(arg)) {
        hints = new Hashtable<DecodeHintType, Object>(3);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
      } else if ("--dump_results".equals(arg)) {
        dumpResults = true;
      } else if (arg.startsWith("--")) {
        System.out.println("Unknown command line option " + arg);
        return;
      }
    }
    for (String arg : args) {
      if (!arg.startsWith("--")) {
        decodeOneArgument(arg, hints, dumpResults);
      }
    }
  }

  private static void decodeOneArgument(String argument, Hashtable<DecodeHintType, Object> hints,
      boolean dumpResults) throws IOException, URISyntaxException {

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
          Result result = decode(input.toURI(), hints);
          if (result != null) {
            successful++;
            if (dumpResults) {
              dumpResult(input, result);
            }
          }
          total++;
        }
        System.out.println("\nDecoded " + successful + " files out of " + total +
            " successfully (" + (successful * 100 / total) + "%)\n");
      } else {
        Result result = decode(inputFile.toURI(), hints);
        if (dumpResults) {
          dumpResult(inputFile, result);
        }
      }
    } else {
      decode(new URI(argument), hints);
    }
  }

  private static void dumpResult(File input, Result result) throws IOException {
    String name = input.getAbsolutePath();
    int pos = name.lastIndexOf('.');
    if (pos > 0) {
      name = name.substring(0, pos);
    }
    File dump = new File(name + ".txt");
    writeStringToFile(result.getText(), dump);
  }

  private static void writeStringToFile(String value, File file) throws IOException {
    Writer out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF8"));
    try {
      out.write(value);
    } finally {
      out.close();
    }
  }

  private static Result decode(URI uri, Hashtable<DecodeHintType, Object> hints) throws IOException {
    BufferedImage image;
    try {
      image = ImageIO.read(uri.toURL());
    } catch (IllegalArgumentException iae) {
      throw new FileNotFoundException("Resource not found: " + uri);
    }
    if (image == null) {
      System.err.println(uri.toString() + ": Could not load image");
      return null;
    }
    try {
      MonochromeBitmapSource source = new BufferedImageMonochromeBitmapSource(image);
      Result result = new MultiFormatReader().decode(source, hints);
      ParsedResult parsedResult = ResultParser.parseResult(result);
      System.out.println(uri.toString() + " (format: " + result.getBarcodeFormat() +
          ", type: " + parsedResult.getType() + "):\nRaw result:\n" + result.getText() +
          "\nParsed result:\n" + parsedResult.getDisplayResult());
      return result;
    } catch (ReaderException e) {
      System.out.println(uri.toString() + ": No barcode found");
      return null;
    }
  }

}
