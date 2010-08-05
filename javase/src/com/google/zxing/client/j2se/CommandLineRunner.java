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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.ResultPoint;

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
import java.util.Vector;

import javax.imageio.ImageIO;

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

    boolean tryHarder = false;
    boolean pureBarcode = false;
    boolean productsOnly = false;
    boolean dumpResults = false;
    boolean dumpBlackPoint = false;
    int[] crop = null;
    for (String arg : args) {
      if ("--try_harder".equals(arg)) {
        tryHarder = true;
      } else if ("--pure_barcode".equals(arg)) {
        pureBarcode = true;
      } else if ("--products_only".equals(arg)) {
        productsOnly = true;
      } else if ("--dump_results".equals(arg)) {
        dumpResults = true;
      } else if ("--dump_black_point".equals(arg)) {
        dumpBlackPoint = true;
      } else if (arg.startsWith("--crop")) {
        crop = new int[4];
        String[] tokens = arg.substring(7).split(",");
        for (int i = 0; i < crop.length; i++) {
          crop[i] = Integer.parseInt(tokens[i]);
        }
      } else if (arg.startsWith("-")) {
        System.err.println("Unknown command line option " + arg);
        printUsage();
        return;
      }
    }

    Hashtable<DecodeHintType, Object> hints = buildHints(tryHarder, pureBarcode, productsOnly);
    for (String arg : args) {
      if (!arg.startsWith("--")) {
        decodeOneArgument(arg, hints, dumpResults, dumpBlackPoint, crop);
      }
    }
  }

  // Manually turn on all formats, even those not yet considered production quality.
  private static Hashtable<DecodeHintType, Object> buildHints(boolean tryHarder,
                                                              boolean pureBarcode,
                                                              boolean productsOnly) {
    Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
    Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>(8);
    vector.addElement(BarcodeFormat.UPC_A);
    vector.addElement(BarcodeFormat.UPC_E);
    vector.addElement(BarcodeFormat.EAN_13);
    vector.addElement(BarcodeFormat.EAN_8);
    vector.addElement(BarcodeFormat.RSS14);
    if (!productsOnly) {
      vector.addElement(BarcodeFormat.CODE_39);
      vector.addElement(BarcodeFormat.CODE_93);
      vector.addElement(BarcodeFormat.CODE_128);
      vector.addElement(BarcodeFormat.ITF);
      vector.addElement(BarcodeFormat.QR_CODE);
      vector.addElement(BarcodeFormat.DATA_MATRIX);
      vector.addElement(BarcodeFormat.PDF417);
      //vector.addElement(BarcodeFormat.CODABAR);
    }
    hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
    if (tryHarder) {
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }
    if (pureBarcode) {
      hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    }
    return hints;
  }

  private static void printUsage() {
    System.err.println("Decode barcode images using the ZXing library\n");
    System.err.println("usage: CommandLineRunner { file | dir | url } [ options ]");
    System.err.println("  --try_harder: Use the TRY_HARDER hint, default is normal (mobile) mode");
    System.err.println("  --pure_barcode: Input image is a pure monochrome barcode image, not a photo");
    System.err.println("  --products_only: Only decode the UPC and EAN families of barcodes");
    System.err.println("  --dump_results: Write the decoded contents to input.txt");
    System.err.println("  --dump_black_point: Compare black point algorithms as input.mono.png");
    System.err.println("  --crop=left,top,width,height: Only examine cropped region of input image(s)");
  }

  private static void decodeOneArgument(String argument,
                                        Hashtable<DecodeHintType, Object> hints,
                                        boolean dumpResults,
                                        boolean dumpBlackPoint,
                                        int[] crop) throws IOException,
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
          Result result = decode(input.toURI(), hints, dumpBlackPoint, crop);
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
        Result result = decode(inputFile.toURI(), hints, dumpBlackPoint, crop);
        if (dumpResults) {
          dumpResult(inputFile, result);
        }
      }
    } else {
      decode(new URI(argument), hints, dumpBlackPoint, crop);
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

  private static Result decode(URI uri,
                               Hashtable<DecodeHintType, Object> hints,
                               boolean dumpBlackPoint,
                               int[] crop) throws IOException {
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
      if (crop == null) {
        source = new BufferedImageLuminanceSource(image);
      } else {
        source = new BufferedImageLuminanceSource(image, crop[0], crop[1], crop[2], crop[3]);
      }
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      if (dumpBlackPoint) {
        dumpBlackPoint(uri, image, bitmap);
      }
      Result result = new MultiFormatReader().decode(bitmap, hints);
      ParsedResult parsedResult = ResultParser.parseResult(result);
      System.out.println(uri.toString() + " (format: " + result.getBarcodeFormat() +
          ", type: " + parsedResult.getType() + "):\nRaw result:\n" + result.getText() +
          "\nParsed result:\n" + parsedResult.getDisplayResult());

      System.out.println("Also, there were " + result.getResultPoints().length + " result points.");
      for (int i = 0; i < result.getResultPoints().length; i++) {
        ResultPoint rp = result.getResultPoints()[i];
        System.out.println("  Point " + i + ": (" + rp.getX() + "," + rp.getY() + ")");
      }

      return result;
    } catch (NotFoundException nfe) {
      System.out.println(uri.toString() + ": No barcode found");
      return null;
    } finally {
      // Uncomment these lines when turning on exception tracking in ReaderException.
      //System.out.println("Threw " + ReaderException.getExceptionCountAndReset() + " exceptions");
      //System.out.println("Throwers:\n" + ReaderException.getThrowersAndReset());
    }
  }

  // Writes out a single PNG which is three times the width of the input image, containing from left
  // to right: the original image, the row sampling monochrome version, and the 2D sampling
  // monochrome version.
  // TODO: Update to compare different Binarizer implementations.
  private static void dumpBlackPoint(URI uri, BufferedImage image, BinaryBitmap bitmap) {
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
    resultName += ".mono.png";
    OutputStream outStream = null;
    try {
      outStream = new FileOutputStream(resultName);
      ImageIO.write(result, "png", outStream);
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
