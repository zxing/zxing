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
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

  private final DecoderConfig config;
  private final Queue<URI> inputs;
  private final Map<DecodeHintType,?> hints;

  DecodeWorker(DecoderConfig config, Queue<URI> inputs) {
    this.config = config;
    this.inputs = inputs;
    hints = config.buildHints();
  }

  @Override
  public Integer call() throws IOException {
    int successful = 0;
    for (URI input; (input = inputs.poll()) != null;) {
      Result[] results = decode(input, hints);
      if (results != null) {
        successful++;
        if (config.dumpResults) {
          dumpResult(input, results);
        }
      }
    }
    return successful;
  }

  private static Path buildOutputPath(URI input, String suffix) throws IOException {
    Path outDir;
    String inputFileName;
    if ("file".equals(input.getScheme())) {
      Path inputPath = Paths.get(input);
      outDir = inputPath.getParent();
      inputFileName = inputPath.getFileName().toString();
    } else {
      outDir = Paths.get(".").toRealPath();
      String[] pathElements = input.getPath().split("/");
      inputFileName = pathElements[pathElements.length - 1];
    }

    // Replace/add extension
    int pos = inputFileName.lastIndexOf('.');
    if (pos > 0) {
      inputFileName = inputFileName.substring(0, pos) + suffix;
    } else {
      inputFileName += suffix;
    }

    return outDir.resolve(inputFileName);
  }

  private static void dumpResult(URI input, Result... results) throws IOException {
    Collection<String> resultTexts = new ArrayList<>();
    for (Result result : results) {
      resultTexts.add(result.getText());
    }
    Files.write(buildOutputPath(input, ".txt"), resultTexts, StandardCharsets.UTF_8);
  }

  private Result[] decode(URI uri, Map<DecodeHintType,?> hints) throws IOException {
    BufferedImage image = ImageReader.readImage(uri);

    LuminanceSource source;
    if (config.crop == null) {
      source = new BufferedImageLuminanceSource(image);
    } else {
      List<Integer> crop = config.crop;
      source = new BufferedImageLuminanceSource(
          image, crop.get(0), crop.get(1), crop.get(2), crop.get(3));
    }

    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    if (config.dumpBlackPoint) {
      dumpBlackPoint(uri, image, bitmap);
    }

    MultiFormatReader multiFormatReader = new MultiFormatReader();
    Result[] results;
    try {
      if (config.multi) {
        MultipleBarcodeReader reader = new GenericMultipleBarcodeReader(multiFormatReader);
        results = reader.decodeMultiple(bitmap, hints);
      } else {
        results = new Result[]{multiFormatReader.decode(bitmap, hints)};
      }
    } catch (NotFoundException ignored) {
      System.out.println(uri + ": No barcode found");
      return null;
    }

    if (config.brief) {
      System.out.println(uri + ": Success");
    } else {
      StringWriter output = new StringWriter();
      for (Result result : results) {
        ParsedResult parsedResult = ResultParser.parseResult(result);
        output.write(uri +
            " (format: " + result.getBarcodeFormat() +
            ", type: " + parsedResult.getType() + "):\n" +
            "Raw result:\n" +
            result.getText() + "\n" +
            "Parsed result:\n" +
            parsedResult.getDisplayResult() + "\n");
        ResultPoint[] resultPoints = result.getResultPoints();
        int numResultPoints = resultPoints.length;
        output.write("Found " + numResultPoints + " result points.\n");
        for (int pointIndex = 0; pointIndex < numResultPoints; pointIndex++) {
          ResultPoint rp = resultPoints[pointIndex];
          if (rp != null) {
            output.write("  Point " + pointIndex + ": (" + rp.getX() + ',' + rp.getY() + ')');
            if (pointIndex != numResultPoints - 1) {
              output.write('\n');
            }
          }
        }
        output.write('\n');
      }
      System.out.println(output);
    }

    return results;
  }

  /**
   * Writes out a single PNG which is three times the width of the input image, containing from left
   * to right: the original image, the row sampling monochrome version, and the 2D sampling
   * monochrome version.
   */
  private static void dumpBlackPoint(URI uri, BufferedImage image, BinaryBitmap bitmap) throws IOException {
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
                                       URI input,
                                       String suffix) throws IOException {
    BufferedImage result = new BufferedImage(stride, height, BufferedImage.TYPE_INT_ARGB);
    result.setRGB(0, 0, stride, height, pixels, 0, stride);
    Path imagePath = buildOutputPath(input, suffix);
    try {
      if (!ImageIO.write(result, "png", imagePath.toFile())) {
        System.err.println("Could not encode an image to " + imagePath);
      }
    } catch (IOException ignored) {
      System.err.println("Could not write to " + imagePath);
    }
  }

}
