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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.util.Locale;

/**
 * Command line utility for encoding barcodes.
 * 
 * @author Sean Owen
 */
public final class CommandLineEncoder {
  
  private static final BarcodeFormat DEFAULT_BARCODE_FORMAT = BarcodeFormat.QR_CODE;
  private static final String DEFAULT_IMAGE_FORMAT = "PNG";
  private static final String DEFAULT_OUTPUT_FILE = "out";
  private static final int DEFAULT_WIDTH = 300;
  private static final int DEFAULT_HEIGHT = 300;

  private CommandLineEncoder() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      printUsage();
      return;
    }

    BarcodeFormat barcodeFormat = DEFAULT_BARCODE_FORMAT;
    String imageFormat = DEFAULT_IMAGE_FORMAT;
    String outFileString = DEFAULT_OUTPUT_FILE;
    int width = DEFAULT_WIDTH;
    int height = DEFAULT_HEIGHT;
    for (String arg : args) {
      if (arg.startsWith("--barcode_format")) {
        barcodeFormat = BarcodeFormat.valueOf(arg.split("=")[1]);
      } else if (arg.startsWith("--image_format")) {
        imageFormat = arg.split("=")[1];
      } else if (arg.startsWith("--output")) {
        outFileString = arg.split("=")[1];
      } else if (arg.startsWith("--width")) {
        width = Integer.parseInt(arg.split("=")[1]);
      } else if (arg.startsWith("--height")) {
        height = Integer.parseInt(arg.split("=")[1]);
      }
    }
    
    if (DEFAULT_OUTPUT_FILE.equals(outFileString)) {
      outFileString += '.' + imageFormat.toLowerCase(Locale.ENGLISH);
    }
        
    String contents = null;
    for (String arg : args) {
      if (!arg.startsWith("--")) {
        contents = arg;
        break;
      }
    }
    
    if (contents == null) {
      printUsage();
      return;
    }
    
    MultiFormatWriter barcodeWriter = new MultiFormatWriter();
    BitMatrix matrix = barcodeWriter.encode(contents, barcodeFormat, width, height);
    MatrixToImageWriter.writeToFile(matrix, imageFormat, new File(outFileString));
  }

  private static void printUsage() {
    System.err.println("Encodes barcode images using the ZXing library\n");
    System.err.println("usage: CommandLineEncoder [ options ] content_to_encode");
    System.err.println("  --barcode_format=format: Format to encode, from BarcodeFormat class. " +
                           "Not all formats are supported. Defaults to QR_CODE.");
    System.err.println("  --image_format=format: image output format, such as PNG, JPG, GIF. Defaults to PNG");
    System.err.println("  --output=filename: File to write to. Defaults to out.png");
    System.err.println("  --width=pixels: Image width. Defaults to 300");
    System.err.println("  --height=pixels: Image height. Defaults to 300");
  }

}
