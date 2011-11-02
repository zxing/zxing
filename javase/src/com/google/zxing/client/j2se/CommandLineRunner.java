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
import com.google.zxing.DecodeHintType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This simple command line utility decodes files, directories of files, or URIs which are passed
 * as arguments. By default it uses the normal decoding algorithms, but you can pass --try_harder
 * to request that hint. The raw text of each barcode is printed, and when running against
 * directories, summary statistics are also displayed.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CommandLineRunner {

  private CommandLineRunner() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      printUsage();
      return;
    }

    Config config = new Config();
    Inputs inputs = new Inputs();

    for (String arg : args) {
      if ("--try_harder".equals(arg)) {
        config.setTryHarder(true);
      } else if ("--pure_barcode".equals(arg)) {
        config.setPureBarcode(true);
      } else if ("--products_only".equals(arg)) {
        config.setProductsOnly(true);
      } else if ("--dump_results".equals(arg)) {
        config.setDumpResults(true);
      } else if ("--dump_black_point".equals(arg)) {
        config.setDumpBlackPoint(true);
      } else if ("--multi".equals(arg)) {
        config.setMulti(true);
      } else if ("--brief".equals(arg)) {
        config.setBrief(true);
      } else if ("--recursive".equals(arg)) {
        config.setRecursive(true);
      } else if (arg.startsWith("--crop")) {
        int[] crop = new int[4];
        String[] tokens = arg.substring(7).split(",");
        for (int i = 0; i < crop.length; i++) {
          crop[i] = Integer.parseInt(tokens[i]);
        }
        config.setCrop(crop);
      } else if (arg.startsWith("--threads") && arg.length() >= 10) {
        int threads = Integer.parseInt(arg.substring(10));
        if (threads > 1) {
          config.setThreads(threads);
        }
      } else if (arg.startsWith("-")) {
        System.err.println("Unknown command line option " + arg);
        printUsage();
        return;
      }
    }
    config.setHints(buildHints(config));

    for (String arg : args) {
      if (!arg.startsWith("--")) {
        addArgumentToInputs(arg, config, inputs);
      }
    }

    List<DecodeThread> threads = new ArrayList<DecodeThread>(config.getThreads());
    for (int x = 0; x < config.getThreads(); x++) {
      DecodeThread thread = new DecodeThread(config, inputs);
      threads.add(thread);
      thread.start();
    }

    int successful = 0;
    for (int x = 0; x < config.getThreads(); x++) {
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
  private static void addArgumentToInputs(String argument, Config config, Inputs inputs) throws IOException {
    File inputFile = new File(argument);
    if (inputFile.exists()) {
      if (inputFile.isDirectory()) {
        for (File singleFile : inputFile.listFiles()) {
          String filename = singleFile.getName().toLowerCase(Locale.ENGLISH);
          // Skip hidden files and directories (e.g. svn stuff).
          if (filename.startsWith(".")) {
            continue;
          }
          // Recurse on nested directories if requested, otherwise skip them.
          if (singleFile.isDirectory()) {
            if (config.isRecursive()) {
              addArgumentToInputs(singleFile.getAbsolutePath(), config, inputs);
            }
            continue;
          }
          // Skip text files and the results of dumping the black point.
          if (filename.endsWith(".txt") || filename.contains(".mono.png")) {
            continue;
          }
          inputs.addInput(singleFile.getCanonicalPath());
        }
      } else {
        inputs.addInput(inputFile.getCanonicalPath());
      }
    } else {
      inputs.addInput(argument);
    }
  }

  // Manually turn on all formats, even those not yet considered production quality.
  private static Map<DecodeHintType,?> buildHints(Config config) {
    Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType,Object>(DecodeHintType.class);
    Collection<BarcodeFormat> vector = new ArrayList<BarcodeFormat>(8);
    vector.add(BarcodeFormat.UPC_A);
    vector.add(BarcodeFormat.UPC_E);
    vector.add(BarcodeFormat.EAN_13);
    vector.add(BarcodeFormat.EAN_8);
    vector.add(BarcodeFormat.RSS_14);
    vector.add(BarcodeFormat.RSS_EXPANDED);
    if (!config.isProductsOnly()) {
      vector.add(BarcodeFormat.CODE_39);
      vector.add(BarcodeFormat.CODE_93);
      vector.add(BarcodeFormat.CODE_128);
      vector.add(BarcodeFormat.ITF);
      vector.add(BarcodeFormat.QR_CODE);
      vector.add(BarcodeFormat.DATA_MATRIX);
      vector.add(BarcodeFormat.AZTEC);
      vector.add(BarcodeFormat.PDF_417);
      vector.add(BarcodeFormat.CODABAR);
      vector.add(BarcodeFormat.MAXICODE);
    }
    hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
    if (config.isTryHarder()) {
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }
    if (config.isPureBarcode()) {
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
    System.err.println("  --recursive: Descend into subdirectories");
    System.err.println("  --crop=left,top,width,height: Only examine cropped region of input image(s)");
    System.err.println("  --threads=n: The number of threads to use while decoding");
  }

}
