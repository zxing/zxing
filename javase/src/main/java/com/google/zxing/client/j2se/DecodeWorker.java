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
import com.google.zxing.multi.MultipleBarcodeReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;

/**
 * One of a pool of threads which pulls images off the Inputs queue and decodes them in parallel.
 *
 * @see CommandLineRunner
 */
final class DecodeWorker implements Callable<Integer> {

  private static final int RED = 0xFFFF0000;
  private static final int BLACK = 0xFF000000;
  private static final int WHITE = 0xFFFFFFFF;

  private final Config config;
  private final Queue<Path> inputs;

  DecodeWorker(Config config, Queue<Path> inputs) {
    this.config = config;
    this.inputs = inputs;
  }

  @Override
  public Integer call() throws IOException {
    int successful = 0;
    Path input;
    while ((input = inputs.poll()) != null) {
      if (Files.exists(input)) {
        if (config.isMulti()) {
          Result[] results = decodeMulti(input.toUri(), config.getHints());
          if (results != null) {
            successful++;
            if (config.isDumpResults()) {
              dumpResultMulti(input, results);
            }
          }
        } else {
          Result result = decode(input.toUri(), config.getHints());
          if (result != null) {
            successful++;
            if (config.isDumpResults()) {
              dumpResult(input, result);
            }
          }
        }
      } else {
        if (decode(input.toUri(), config.getHints()) != null) {
          successful++;
        }
      }
    }
    return successful;
  }

  private static void dumpResult(Path input, Result result) throws IOException {
    String name = input.getFileName().toString();
    int pos = name.lastIndexOf('.');
    if (pos > 0) {
      name = name.substring(0, pos) + ".txt";
    }
    Path dumpFile = input.getParent().resolve(name);
    Files.write(dumpFile, Collections.singleton(result.getText()), StandardCharsets.UTF_8);
  }

  private static void dumpResultMulti(Path input, Result[] results) throws IOException {
    String name = input.getFileName().toString();
    int pos = name.lastIndexOf('.');
    if (pos > 0) {
      name = name.substring(0, pos) + ".txt";
    }
    Path dumpFile = input.getParent().resolve(name);
    Collection<String> resultTexts = new ArrayList<>();
    for (Result result : results) {
      resultTexts.add(result.getText());
    }
    Files.write(dumpFile, resultTexts, StandardCharsets.UTF_8);
  }

  private Result decode(URI uri, Map<DecodeHintType,?> hints) throws IOException {
    BufferedImage image = ImageReader.readImage(uri);
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
        System.out.println(uri + ": Success");
      } else {
        ParsedResult parsedResult = ResultParser.parseResult(result);
        System.out.println(uri + " (format: " + result.getBarcodeFormat() + ", type: " +
            parsedResult.getType() + "):\nRaw result:\n" + result.getText() + "\nParsed result:\n" +
            parsedResult.getDisplayResult());

        System.out.println("Found " + result.getResultPoints().length + " result points.");
        for (int i = 0; i < result.getResultPoints().length; i++) {
          ResultPoint rp = result.getResultPoints()[i];
          if (rp != null) {
            System.out.println("  Point " + i + ": (" + rp.getX() + ',' + rp.getY() + ')');
          }
        }
      }

      return result;
    } catch (NotFoundException ignored) {
      System.out.println(uri + ": No barcode found");
      return null;
    }
  }

  private Result[] decodeMulti(URI uri, Map<DecodeHintType,?> hints) throws IOException {
    BufferedImage image = ImageReader.readImage(uri);
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
      MultipleBarcodeReader reader = new GenericMultipleBarcodeReader(multiFormatReader);
      Result[] results = reader.decodeMultiple(bitmap, hints);

      if (config.isBrief()) {
        System.out.println(uri + ": Success");
      } else {
        for (Result result : results) {
          ParsedResult parsedResult = ResultParser.parseResult(result);
          System.out.println(uri + " (format: "
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
    } catch (NotFoundException ignored) {
      System.out.println(uri + ": No barcode found");
      return null;
    }
  }

  /**
   * Writes out a single PNG which is three times the width of the input image, containing from left
   * to right: the original image, the row sampling monochrome version, and the 2D sampling
   * monochrome version.
   */
  private static void dumpBlackPoint(URI uri, BufferedImage image, BinaryBitmap bitmap) {
    if (uri.getPath().contains(".mono.png")) {
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
        Arrays.fill(pixels, offset, offset + width, RED);
        continue;
      }

      int offset = y * stride + width;
      for (int x = 0; x < width; x++) {
        pixels[offset + x] = row.get(x) ? BLACK : WHITE;
      }
    }

    // 2D sampling
    try {
      for (int y = 0; y < height; y++) {
        BitMatrix matrix = bitmap.getBlackMatrix();
        int offset = y * stride + width * 2;
        for (int x = 0; x < width; x++) {
          pixels[offset + x] = matrix.get(x, y) ? BLACK : WHITE;
        }
      }
    } catch (NotFoundException ignored) {
      // continue
    }

    writeResultImage(stride, height, pixels, uri, ".mono.png");
  }

  private static void writeResultImage(int stride,
                                       int height,
                                       int[] pixels,
                                       URI uri,
                                       String suffix) {
    BufferedImage result = new BufferedImage(stride, height, BufferedImage.TYPE_INT_ARGB);
    result.setRGB(0, 0, stride, height, pixels, 0, stride);

    // Use the current working directory for URLs
    String resultName = uri.getPath();
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
    try {
      if (!ImageIO.write(result, "png", Paths.get(resultName).toFile())) {
        System.err.println("Could not encode an image to " + resultName);
      }
    } catch (IOException ignored) {
      System.err.println("Could not write to " + resultName);
    }
  }

}
