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

import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.BitArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    if (args == null || args.length == 0) {
      printUsage();
      return;
    }
    Hashtable<DecodeHintType, Object> hints = null;
    boolean dumpResults = false;
    boolean dumpBlackPoint = false;
    for (String arg : args) {
      if ("--try_harder".equals(arg)) {
        hints = new Hashtable<DecodeHintType, Object>(3);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
      } else if ("--dump_results".equals(arg)) {
        dumpResults = true;
      } else if ("--dump_black_point".equals(arg)) {
        dumpBlackPoint = true;
      } else if (arg.startsWith("-")) {
        System.out.println("Unknown command line option " + arg);
        printUsage();
        return;
      }
    }
    for (String arg : args) {
      if (!arg.startsWith("--")) {
        decodeOneArgument(arg, hints, dumpResults, dumpBlackPoint);
      }
    }
  }

  private static void printUsage() {
    System.out.println("Decode barcode images using the ZXing library\n");
    System.out.println("usage: CommandLineRunner { file | dir | url } [ options ]");
    System.out.println("  --try_harder: Use the TRY_HARDER hint, default is normal (mobile) mode");
    System.out.println("  --dump_results: Write the decoded contents to input.txt");
    System.out.println("  --dump_black_point: Compare black point algorithms as input.mono.png");
  }

  private static void decodeOneArgument(String argument, Hashtable<DecodeHintType, Object> hints,
      boolean dumpResults, boolean dumpBlackPoint) throws IOException,
      URISyntaxException {

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
          // Skip the results of dumping the black point.
          if (filename.contains(".mono.png")) {
            continue;
          }
          Result result = decode(input.toURI(), hints, dumpBlackPoint);
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
        Result result = decode(inputFile.toURI(), hints, dumpBlackPoint);
        if (dumpResults) {
          dumpResult(inputFile, result);
        }
      }
    } else {
      decode(new URI(argument), hints, dumpBlackPoint);
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

  private static Result decode(URI uri, Hashtable<DecodeHintType, Object> hints,
      boolean dumpBlackPoint) throws IOException {
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
      if (dumpBlackPoint) {
        dumpBlackPoint(uri, image, source);
      }
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

  // Writes out a single PNG which is three times the width of the input image, containing from left
  // to right: the original image, the row sampling monochrome version, and the 2D sampling
  // monochrome version.
  private static void dumpBlackPoint(URI uri, BufferedImage image, MonochromeBitmapSource source) {
    String inputName = uri.getPath();
    if (inputName.contains(".mono.png")) {
      return;
    }

    int width = source.getWidth();
    int height = source.getHeight();
    int stride = width * 3;
    int[] pixels = new int[stride * height];

    // The original image
    int[] argb = new int[width];
    for (int y = 0; y < height; y++) {
      image.getRGB(0, y, width, 1, argb, 0, width);
      System.arraycopy(argb, 0, pixels, y * stride, width);
    }
    argb = null;

    // Row sampling
    BitArray row = new BitArray(width);
    for (int y = 0; y < height; y++) {
      try {
        source.estimateBlackPoint(BlackPointEstimationMethod.ROW_SAMPLING, y);
      } catch (ReaderException e) {
        // If the row histogram failed, draw a red line and keep going
        int offset = y * stride + width;
        for (int x = 0; x < width; x++) {
          pixels[offset + x] = 0xffff0000;
        }
        continue;
      }
      source.getBlackRow(y, row, 0, width);
      int offset = y * stride + width;
      for (int x = 0; x < width; x++) {
        if (row.get(x)) {
          pixels[offset + x] = 0xff000000;
        } else {
          pixels[offset + x] = 0xffffffff;
        }
      }
    }

    // 2D sampling
    try {
      source.estimateBlackPoint(BlackPointEstimationMethod.TWO_D_SAMPLING, 0);
      for (int y = 0; y < height; y++) {
        source.getBlackRow(y, row, 0, width);
        int offset = y * stride + width * 2;
        for (int x = 0; x < width; x++) {
          if (row.get(x)) {
            pixels[offset + x] = 0xff000000;
          } else {
            pixels[offset + x] = 0xffffffff;
          }
        }
      }
    } catch (ReaderException e) {
    }

    // Write the result
    BufferedImage result = new BufferedImage(stride, height, BufferedImage.TYPE_INT_ARGB);
    result.setRGB(0, 0, stride, height, pixels, 0, stride);

    // Use the current working directory for URLs
    String resultName = inputName;
    if (uri.getScheme().equals("http")) {
      int pos = resultName.lastIndexOf('/');
      if (pos > 0) {
        resultName = "." + resultName.substring(pos);
      }
    }
    int pos = resultName.lastIndexOf('.');
    if (pos > 0) {
      resultName = resultName.substring(0, pos);
    }
    resultName += ".mono.png";
    try {
      OutputStream outStream = new FileOutputStream(resultName);
      ImageIO.write(result, "png", outStream);
    } catch (FileNotFoundException e) {
      System.out.println("Could not create " + resultName);
    } catch (IOException e) {
      System.out.println("Could not write to " + resultName);
    }
  }

}
