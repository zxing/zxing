/*
 * Copyright 2008 ZXing authors
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
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.BitArray;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.imageio.ImageIO;

/**
 * Utility application for evaluating the effectiveness of the BlackPointEstimator used by
 * MonochromeBitmapSource. Given a set of images on the command line, it converts each to a
 * black-and-white PNG. The result is placed in a file based on the input name, with either
 * "_converted_row" or "_converted_2d" appended.
 *
 * @author alasdair@google.com (Alasdair Mackintosh)
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ImageConverter {

  private static final String FORMAT = "png";
  private static final int WHITE = 0xFFFFFFFF;
  private static final int BLACK = 0xFF000000;
  private static final int RED = 0xFFFF0000;
  private static BlackPointEstimationMethod sMethod = BlackPointEstimationMethod.ROW_SAMPLING;

  private ImageConverter() {
  }

  public static void main(String[] args) throws Exception {
    for (String arg : args) {
      if (arg.equals("-row")) {
        sMethod = BlackPointEstimationMethod.ROW_SAMPLING;
      } else if (arg.equals("-2d")) {
        sMethod = BlackPointEstimationMethod.TWO_D_SAMPLING;
      } else if (arg.startsWith("-")) {
        System.out.println("Ignoring unrecognized option: " + arg);
      }
    }
    for (String arg : args) {
      if (arg.startsWith("-")) {
        continue;
      }
      File inputFile = new File(arg);
      if (inputFile.exists()) {
        if (inputFile.isDirectory()) {
          for (File input : inputFile.listFiles()) {
            String filename = input.getName().toLowerCase();
            // Skip hidden files and text files (the latter is found in the blackbox tests).
            if (filename.startsWith(".") || filename.endsWith(".txt")) {
              continue;
            }
            // Skip the results of dumping the black point.
            if (filename.contains(".mono.png") || filename.contains(".row.png") ||
                filename.contains(".2d.png")) {
              continue;
            }
            convertImage(input.toURI());
          }
        } else {
          convertImage(inputFile.toURI());
        }
      } else {
        convertImage(new URI(arg));
      }
    }
  }


  private static void convertImage(URI uri) throws IOException {
    BufferedImage image = ImageIO.read(uri.toURL());
    MonochromeBitmapSource src = new BufferedImageMonochromeBitmapSource(image);
    int width = src.getWidth();
    int height = src.getHeight();

    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    BitArray array = new BitArray(width);

    try {
      // Run the 2D sampling once up front
      if (sMethod == BlackPointEstimationMethod.TWO_D_SAMPLING) {
        src.estimateBlackPoint(sMethod, 0);
      }
    } catch (ReaderException e) {
      System.out.println(e.toString());
      return;
    }

      for (int y = 0; y < height; y++) {
        // Run the 1D sampling once per row
        if (sMethod == BlackPointEstimationMethod.ROW_SAMPLING) {
          try {
              src.estimateBlackPoint(sMethod, y);
          } catch (ReaderException e) {
            // Draw rows with insufficient dynamic range in red
            for (int x = 0; x < width; x++) {
              result.setRGB(x, y, RED);
            }
            continue;
          }
        }

        // Fetch the entire row at once, then fill out the result image
        src.getBlackRow(y, array, 0, width);
        for (int x = 0; x < width; x++) {
          result.setRGB(x, y, array.get(x) ? BLACK : WHITE);
        }
      }


    File output = getOutput(uri);
    System.out.printf("Writing output to %s\n", output);
    ImageIO.write(result, FORMAT, output);
  }

  private static File getFileOfUri(URI uri) {
    String name = uri.getPath();
    int slashPos = name.lastIndexOf((int) '/');
    String parent, basename;
    if (slashPos != -1 && slashPos != name.length() - 1) {
      parent = name.substring(0, slashPos);
      basename = name.substring(slashPos + 1);
    } else {
      parent = ".";
      basename = name;
    }
    File parentFile = new File(parent);
    if (!parentFile.exists()) {
      return null;
    }

    File baseFile = new File(parent, basename);
    if (!baseFile.exists()) {
      return null;
    }

    return baseFile;
  }

  private static File getOutput(URI uri) {
    File result = getFileOfUri(uri);
    if (result == null) {
      result = new File("ConvertedImage." + FORMAT);
    } else {
      String name = result.getPath();
      int dotpos = name.lastIndexOf((int) '.');
      if (dotpos != -1) {
        name = name.substring(0, dotpos);
      }
      String suffix = (sMethod == BlackPointEstimationMethod.ROW_SAMPLING) ? "row" : "2d";
      result = new File(name + '.' + suffix + '.' + FORMAT);
    }
    return result;
  }

}
