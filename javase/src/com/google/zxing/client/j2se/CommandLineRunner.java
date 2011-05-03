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
import com.google.zxing.ResultPoint;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;

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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import javax.imageio.ImageIO;

/**
 * This simple command line utility decodes files, directories of files, or URIs which are passed
 * as arguments. By default it uses the normal decoding algorithms, but you can pass --try_harder
 * to request that hint. The raw text of each barcode is printed, and when running against
 * directories, summary statistics are also displayed.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CommandLineRunner {

  private static class Config {
    public Hashtable<DecodeHintType, Object> hints = null;
    public boolean tryHarder = false;
    public boolean pureBarcode = false;
    public boolean productsOnly = false;
    public boolean dumpResults = false;
    public boolean dumpBlackPoint = false;
    public boolean multi = false;
    public boolean brief = false;
    public int[] crop = null;
    public int threads = 1;
  }

  // Represents the collection of all images files/URLs to decode.
  private static class Inputs {
    Vector<String> inputs = new Vector<String>(10);
    int position = 0;

    public synchronized void addInput(String pathOrUrl) {
      inputs.add(pathOrUrl);
    }

    public synchronized String getNextInput() {
      if (position < inputs.size()) {
        String result = inputs.get(position);
        position++;
        return result;
      } else {
        return null;
      }
    }

    public synchronized int getInputCount() {
      return inputs.size();
    }
  }

  // One of a pool of threads which pulls images off the Inputs queue and decodes them in parallel.
  private static class DecodeThread extends Thread {
    private int successful = 0;

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
            if (config.multi) {
              Result[] results = decodeMulti(inputFile.toURI(), config.hints);
              if (results != null) {
                successful++;
                if (config.dumpResults) {
                  dumpResultMulti(inputFile, results);
                }
              }
            } else {
              Result result = decode(inputFile.toURI(), config.hints);
              if (result != null) {
                successful++;
                if (config.dumpResults) {
                  dumpResult(inputFile, result);
                }
              }
            }
          } catch (IOException e) {
          }
        } else {
          try {
            Result result = decode(new URI(input), config.hints);
            if (result != null) {
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
  }

  private static Config config = new Config();
  private static Inputs inputs = new Inputs();

  private CommandLineRunner() {
  }

  public static void main(String[] args) throws Exception {
    if (args == null || args.length == 0) {
      printUsage();
      return;
    }

    for (String arg : args) {
      if ("--try_harder".equals(arg)) {
        config.tryHarder = true;
      } else if ("--pure_barcode".equals(arg)) {
        config.pureBarcode = true;
      } else if ("--products_only".equals(arg)) {
        config.productsOnly = true;
      } else if ("--dump_results".equals(arg)) {
        config.dumpResults = true;
      } else if ("--dump_black_point".equals(arg)) {
        config.dumpBlackPoint = true;
      } else if ("--multi".equals(arg)) {
        config.multi = true;
      } else if ("--brief".equals(arg)) {
        config.brief = true;
      } else if (arg.startsWith("--crop")) {
        config.crop = new int[4];
        String[] tokens = arg.substring(7).split(",");
        for (int i = 0; i < config.crop.length; i++) {
          config.crop[i] = Integer.parseInt(tokens[i]);
        }
      } else if (arg.startsWith("--threads") && arg.length() >= 10) {
        int threads = Integer.parseInt(arg.substring(10));
        if (threads > 1) {
          config.threads = threads;
        }
      } else if (arg.startsWith("-")) {
        System.err.println("Unknown command line option " + arg);
        printUsage();
        return;
      }
    }
    config.hints = buildHints();

    for (String arg : args) {
      if (!arg.startsWith("--")) {
        addArgumentToInputs(arg);
      }
    }

    ArrayList<DecodeThread> threads = new ArrayList<DecodeThread>(config.threads);
    for (int x = 0; x < config.threads; x++) {
      DecodeThread thread = new DecodeThread();
      threads.add(thread);
      thread.start();
    }

    int successful = 0;
    for (int x = 0; x < config.threads; x++) {
      threads.get(x).join();
      successful += threads.get(x).getSuccessful();
    }
    int total = inputs.getInputCount();
    if (total > 1) {
      System.out.println("\nDecoded " + successful + " files out of " + total +
          " successfully (" + (successful * 100 / total) + "%)\n");
    }
  }

  // Build all the inputs up front into a single flat list, so the threads can atomically pull
  // paths/URLs off the queue.
  private static void addArgumentToInputs(String argument) {
    File inputFile = new File(argument);
    if (inputFile.exists()) {
      if (inputFile.isDirectory()) {
        for (File singleFile : inputFile.listFiles()) {
          if (singleFile.isDirectory()) {
            // Recurse on nested directories.
            addArgumentToInputs(singleFile.getAbsolutePath());
            continue;
          }
          String filename = singleFile.getName().toLowerCase();
          // Skip hidden files and text files (the latter is found in the blackbox tests).
          if (filename.startsWith(".") || filename.endsWith(".txt")) {
            continue;
          }
          // Skip the results of dumping the black point.
          if (filename.contains(".mono.png")) {
            continue;
          }
          inputs.addInput(singleFile.getAbsolutePath());
        }
      } else {
        inputs.addInput(inputFile.getAbsolutePath());
      }
    } else {
      inputs.addInput(argument);
    }
  }

  // Manually turn on all formats, even those not yet considered production quality.
  private static Hashtable<DecodeHintType, Object> buildHints() {
    Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
    Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>(8);
    vector.addElement(BarcodeFormat.UPC_A);
    vector.addElement(BarcodeFormat.UPC_E);
    vector.addElement(BarcodeFormat.EAN_13);
    vector.addElement(BarcodeFormat.EAN_8);
    vector.addElement(BarcodeFormat.RSS14);
    if (!config.productsOnly) {
      vector.addElement(BarcodeFormat.CODE_39);
      vector.addElement(BarcodeFormat.CODE_93);
      vector.addElement(BarcodeFormat.CODE_128);
      vector.addElement(BarcodeFormat.ITF);
      vector.addElement(BarcodeFormat.QR_CODE);
      vector.addElement(BarcodeFormat.DATA_MATRIX);
      vector.addElement(BarcodeFormat.AZTEC);
      vector.addElement(BarcodeFormat.PDF417);
      vector.addElement(BarcodeFormat.CODABAR);
    }
    hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
    if (config.tryHarder) {
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }
    if (config.pureBarcode) {
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
    System.err.println("  --multi: Scans image for multiple barcodes");
    System.err.println("  --brief: Only output one line per file, omitting the contents");
    System.err.println("  --crop=left,top,width,height: Only examine cropped region of input image(s)");
    System.err.println("  --threads=n: The number of threads to use while decoding");
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

  private static void dumpResultMulti(File input, Result[] results) throws IOException {
    String name = input.getAbsolutePath();
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

  private static Result decode(URI uri, Hashtable<DecodeHintType, Object> hints)
      throws IOException {
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
      if (config.crop == null) {
        source = new BufferedImageLuminanceSource(image);
      } else {
        source = new BufferedImageLuminanceSource(image, config.crop[0], config.crop[1],
            config.crop[2], config.crop[3]);
      }
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      if (config.dumpBlackPoint) {
        dumpBlackPoint(uri, image, bitmap);
      }
      Result result = new MultiFormatReader().decode(bitmap, hints);
      if (config.brief) {
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
      // } finally {
      // Uncomment these lines when turning on exception tracking in ReaderException.
      //System.out.println("Threw " + ReaderException.getExceptionCountAndReset() + " exceptions");
      //System.out.println("Throwers:\n" + ReaderException.getThrowersAndReset());
    }
  }

  private static Result[] decodeMulti(URI uri, Hashtable<DecodeHintType, Object> hints)
      throws IOException {
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
      if (config.crop == null) {
        source = new BufferedImageLuminanceSource(image);
      } else {
        source = new BufferedImageLuminanceSource(image, config.crop[0], config.crop[1],
            config.crop[2], config.crop[3]);
      }
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      if (config.dumpBlackPoint) {
        dumpBlackPoint(uri, image, bitmap);
      }

      MultiFormatReader multiFormatReader = new MultiFormatReader();
      GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(
          multiFormatReader);
      Result[] results = reader.decodeMultiple(bitmap, hints);

      if (config.brief) {
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

    writeResultImage(stride, height, pixels, uri, inputName, ".mono.png");
  }

  private static void writeResultImage(int stride, int height, int[] pixels, URI uri,
      String inputName, String suffix) {
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
