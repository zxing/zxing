/*
 * Copyright 2011 ZXing authors
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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;

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
import java.nio.charset.Charset;
import java.util.Map;

/**
 * One of a pool of threads which pulls images off the Inputs queue and decodes them in parallel.
 *
 * @see CommandLineRunner
 */
final class DecodeThread extends Thread {

  private int successful;
  private final Config config;
  private final Inputs inputs;

  DecodeThread(Config config, Inputs inputs) {
    this.config = config;
    this.inputs = inputs;
  }

  @Override
  public void run() {
    while (true) {
      String input = inputs.getNextInput();
      if (input == null) {
        break;
      }

      File inputFile = new File(input);
      if (inputFile.exists()) {
        try {
          if (config.isMulti()) {
            Result[] results = decodeMulti(inputFile.toURI(), config.getHints());
            if (results != null) {
              successful++;
              if (config.isDumpResults()) {
                dumpResultMulti(inputFile, results);
              }
            }
          } else {
            Result result = decode(inputFile.toURI(), config.getHints());
            if (result != null) {
              successful++;
              if (config.isDumpResults()) {
                dumpResult(inputFile, result);
              }
            }
          }
        } catch (IOException e) {
        }
      } else {
        try {
          if (decode(new URI(input), config.getHints()) != null) {
            successful++;
          }
        } catch (Exception e) {
        }
      }
    }
  }

  public int getSuccessful() {
    return successful;
  }

  private static void dumpResult(File input, Result result) throws IOException {
    String name = input.getCanonicalPath();
    int pos = name.lastIndexOf('.');
    if (pos > 0) {
      name = name.substring(0, pos);
    }
    File dump = new File(name + ".txt");
    writeStringToFile(result.getText(), dump);
  }

  private static void dumpResultMulti(File input, Result[] results) throws IOException {
    String name = input.getCanonicalPath();
    int pos = name.lastIndexOf('.');
    if (pos > 0) {
      name = name.substring(0, pos);
    }
    File dump = new File(name + ".txt");
    writeResultsToFile(results, dump);
  }

  private static void writeStringToFile(String value, File file) throws IOException {
    Writer out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF8"));
    try {
      out.write(value);
    } finally {
      out.close();
    }
  }

  private static void writeResultsToFile(Result[] results, File file) throws IOException {
    String newline = System.getProperty("line.separator");
    Writer out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF8"));
    try {
      for (Result result : results) {
        out.write(result.getText());
        out.write(newline);
      }
    } finally {
      out.close();
    }
  }

  private Result decode(URI uri, Map<DecodeHintType,?> hints) throws IOException {
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
      LuminanceSource source;
      if (config.getCrop() == null) {
        source = new BufferedImageLuminanceSource(image);
      } else {
        int[] crop = config.getCrop();
        source = new BufferedImageLuminanceSource(image, crop[0], crop[1], crop[2], crop[3]);
      }
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      if (config.isDumpBlackPoint()) {
        dumpBlackPoint(uri, image, bitmap);
      }
      Result result = new MultiFormatReader().decode(bitmap, hints);
      if (config.isBrief()) {
        System.out.println(uri.toString() + ": Success");
      } else {
        ParsedResult parsedResult = ResultParser.parseResult(result);
        System.out.println(uri.toString() + " (format: " + result.getBarcodeFormat() + ", type: " +
            parsedResult.getType() + "):\nRaw result:\n" + result.getText() + "\nParsed result:\n" +
            parsedResult.getDisplayResult());

        System.out.println("Found " + result.getResultPoints().length + " result points.");
        for (int i = 0; i < result.getResultPoints().length; i++) {
          ResultPoint rp = result.getResultPoints()[i];
          System.out.println("  Point " + i + ": (" + rp.getX() + ',' + rp.getY() + ')');
        }
      }

      return result;
    } catch (NotFoundException nfe) {
      System.out.println(uri.toString() + ": No barcode found");
      return null;
    }
  }

  private Result[] decodeMulti(URI uri, Map<DecodeHintType,?> hints) throws IOException {
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
      LuminanceSource source;
      if (config.getCrop() == null) {
        source = new BufferedImageLuminanceSource(image);
      } else {
        int[] crop = config.getCrop();
        source = new BufferedImageLuminanceSource(image, crop[0], crop[1], crop[2], crop[3]);
      }
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      if (config.isDumpBlackPoint()) {
        dumpBlackPoint(uri, image, bitmap);
      }

      MultiFormatReader multiFormatReader = new MultiFormatReader();
      GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(
          multiFormatReader);
      Result[] results = reader.decodeMultiple(bitmap, hints);

      if (config.isBrief()) {
        System.out.println(uri.toString() + ": Success");
      } else {
        for (Result result : results) {
          ParsedResult parsedResult = ResultParser.parseResult(result);
          System.out.println(uri.toString() + " (format: "
              + result.getBarcodeFormat() + ", type: "
              + parsedResult.getType() + "):\nRaw result:\n"
              + result.getText() + "\nParsed result:\n"
              + parsedResult.getDisplayResult());
          System.out.println("Found " + result.getResultPoints().length + " result points.");
          for (int i = 0; i < result.getResultPoints().length; i++) {
            ResultPoint rp = result.getResultPoints()[i];
            System.out.println("  Point " + i + ": (" + rp.getX() + ',' + rp.getY() + ')');
          }
        }
      }
      return results;
    } catch (NotFoundException nfe) {
      System.out.println(uri.toString() + ": No barcode found");
      return null;
    }
  }

  /**
   * Writes out a single PNG which is three times the width of the input image, containing from left
   * to right: the original image, the row sampling monochrome version, and the 2D sampling
   * monochrome version.
   */
  private static void dumpBlackPoint(URI uri, BufferedImage image, BinaryBitmap bitmap) {
    // TODO: Update to compare different Binarizer implementations.
    String inputName = uri.getPath();
    if (inputName.contains(".mono.png")) {
      return;
    }

    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int stride = width * 3;
    int[] pixels = new int[stride * height];

    // The original image
    int[] argb = new int[width];
    for (int y = 0; y < height; y++) {
      image.getRGB(0, y, width, 1, argb, 0, width);
      System.arraycopy(argb, 0, pixels, y * stride, width);
    }

    // Row sampling
    BitArray row = new BitArray(width);
    for (int y = 0; y < height; y++) {
      try {
        row = bitmap.getBlackRow(y, row);
      } catch (NotFoundException nfe) {
        // If fetching the row failed, draw a red line and keep going.
        int offset = y * stride + width;
        for (int x = 0; x < width; x++) {
          pixels[offset + x] = 0xffff0000;
        }
        continue;
      }

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
      for (int y = 0; y < height; y++) {
        BitMatrix matrix = bitmap.getBlackMatrix();
        int offset = y * stride + width * 2;
        for (int x = 0; x < width; x++) {
          if (matrix.get(x, y)) {
            pixels[offset + x] = 0xff000000;
          } else {
            pixels[offset + x] = 0xffffffff;
          }
        }
      }
    } catch (NotFoundException nfe) {
    }

    writeResultImage(stride, height, pixels, uri, inputName, ".mono.png");
  }

  private static void writeResultImage(int stride,
                                       int height,
                                       int[] pixels,
                                       URI uri,
                                       String inputName,
                                       String suffix) {
    // Write the result
    BufferedImage result = new BufferedImage(stride, height, BufferedImage.TYPE_INT_ARGB);
    result.setRGB(0, 0, stride, height, pixels, 0, stride);

    // Use the current working directory for URLs
    String resultName = inputName;
    if ("http".equals(uri.getScheme())) {
      int pos = resultName.lastIndexOf('/');
      if (pos > 0) {
        resultName = '.' + resultName.substring(pos);
      }
    }
    int pos = resultName.lastIndexOf('.');
    if (pos > 0) {
      resultName = resultName.substring(0, pos);
    }
    resultName += suffix;
    OutputStream outStream = null;
    try {
      outStream = new FileOutputStream(resultName);
      if (!ImageIO.write(result, "png", outStream)) {
        System.err.println("Could not encode an image to " + resultName);
      }
    } catch (FileNotFoundException e) {
      System.err.println("Could not create " + resultName);
    } catch (IOException e) {
      System.err.println("Could not write to " + resultName);
    } finally {
      try {
        if (outStream != null) {
          outStream.close();
        }
      } catch (IOException ioe) {
        // continue
      }
    }
  }

}
