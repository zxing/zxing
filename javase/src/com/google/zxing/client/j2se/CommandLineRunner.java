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
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * This simple command line utility decodes files, directories of files, or URIs which are passed
 * as arguments. By default it uses the normal decoding algorithms, but you can pass --try_harder
 * to request that hint. The raw text of each barcode is printed, and when running against
 * directories, summary statistics are also displayed.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CommandLineRunner {

  private static final String POSSIBLE_FORMATS_ARG = "--possibleFormats=";
  private static final Pattern COMMA = Pattern.compile(",");

  private CommandLineRunner() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      printUsage();
      return;
    }

    Config config = new Config();

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
        String[] tokens = COMMA.split(arg.substring(7));
        for (int i = 0; i < crop.length; i++) {
          crop[i] = Integer.parseInt(tokens[i]);
        }
        config.setCrop(crop);
      } else if (arg.startsWith(POSSIBLE_FORMATS_ARG)) {
        config.setPossibleFormats(COMMA.split(arg.substring(POSSIBLE_FORMATS_ARG.length())));
      } else if (arg.startsWith("-")) {
        System.err.println("Unknown command line option " + arg);
        printUsage();
        return;
      }
    }
    config.setHints(buildHints(config));

    Queue<String> inputs = new ConcurrentLinkedQueue<String>();
    for (String arg : args) {
      if (!arg.startsWith("--")) {
        addArgumentToInputs(arg, config, inputs);
      }
    }

    int numThreads = Math.min(inputs.size(), Runtime.getRuntime().availableProcessors());
    int successful = 0;    
    if (numThreads > 1) {
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      Collection<Future<Integer>> futures = new ArrayList<Future<Integer>>(numThreads);
      for (int x = 0; x < numThreads; x++) {
        futures.add(executor.submit(new DecodeWorker(config, inputs)));
      }
      executor.shutdown();
      for (Future<Integer> future : futures) {
        successful += future.get();
      }
    } else {
      successful += new DecodeWorker(config, inputs).call();
    }

    int total = inputs.size();
    if (total > 1) {
      System.out.println("\nDecoded " + successful + " files out of " + total +
          " successfully (" + (successful * 100 / total) + "%)\n");
    }
  }

  // Build all the inputs up front into a single flat list, so the threads can atomically pull
  // paths/URLs off the queue.
  private static void addArgumentToInputs(String argument, Config config, Queue<String> inputs) throws IOException {
    File inputFile = new File(argument);
    if (inputFile.exists()) {
      if (inputFile.isDirectory()) {
        for (File singleFile : inputFile.listFiles()) {
          String filename = singleFile.getName().toLowerCase(Locale.ENGLISH);
          // Skip hidden files and directories (e.g. svn stuff).
          if (filename.startsWith(".")) {
            continue;
          }
          // Recur on nested directories if requested, otherwise skip them.
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
          inputs.add(singleFile.getCanonicalPath());
        }
      } else {
        inputs.add(inputFile.getCanonicalPath());
      }
    } else {
      inputs.add(argument);
    }
  }

  // Manually turn on all formats, even those not yet considered production quality.
  private static Map<DecodeHintType,?> buildHints(Config config) {
    Collection<BarcodeFormat> possibleFormats = new ArrayList<BarcodeFormat>();
    String[] possibleFormatsNames = config.getPossibleFormats();
    if (possibleFormatsNames != null && possibleFormatsNames.length > 0) {
      for (String format : possibleFormatsNames) {
        possibleFormats.add(BarcodeFormat.valueOf(format));
      }
    } else {
      possibleFormats.add(BarcodeFormat.UPC_A);
      possibleFormats.add(BarcodeFormat.UPC_E);
      possibleFormats.add(BarcodeFormat.EAN_13);
      possibleFormats.add(BarcodeFormat.EAN_8);
      possibleFormats.add(BarcodeFormat.RSS_14);
      possibleFormats.add(BarcodeFormat.RSS_EXPANDED);
      if (!config.isProductsOnly()) {
        possibleFormats.add(BarcodeFormat.CODE_39);
        possibleFormats.add(BarcodeFormat.CODE_93);
        possibleFormats.add(BarcodeFormat.CODE_128);
        possibleFormats.add(BarcodeFormat.ITF);
        possibleFormats.add(BarcodeFormat.QR_CODE);
        possibleFormats.add(BarcodeFormat.DATA_MATRIX);
        possibleFormats.add(BarcodeFormat.AZTEC);
        possibleFormats.add(BarcodeFormat.PDF_417);
        possibleFormats.add(BarcodeFormat.CODABAR);
        possibleFormats.add(BarcodeFormat.MAXICODE);
      }
    }
    Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
    hints.put(DecodeHintType.POSSIBLE_FORMATS, possibleFormats);
    if (config.isTryHarder()) {
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }
    if (config.isPureBarcode()) {
      hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    }
    return hints;
  }

  private static void printUsage() {
    System.err.println("Decode barcode images using the ZXing library");
    System.err.println();
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
    StringBuilder builder = new StringBuilder(
        "  " + POSSIBLE_FORMATS_ARG + "barcodeFormat[,barcodeFormat2...] where barcodeFormat is any of: ");
    for (BarcodeFormat format : BarcodeFormat.values()) {
      builder.append(format).append(',');
    }
    builder.setLength(builder.length() - 1);
    System.err.println(builder);
  }

}
