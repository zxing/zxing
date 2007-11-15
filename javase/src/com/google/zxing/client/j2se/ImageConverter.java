/*
 * Copyright 2007 Google Inc.
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

import com.google.zxing.MonochromeBitmapSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Utility application for evaluating the effectiveness of the
 * MonochromeBitmapSource. Given a set of images on the command line,
 * converts each to a black-and-white GIF. The result is placed in 
 * a file based on the input name, with "_converted" appended.
 *
 * @author alasdair@google.com (Alasdair Mackintosh)
 */
public final class ImageConverter {

  private static final String FORMAT = "gif";
  private static final int WHITE = 0xFFFFFFFF;
  private static final int BLACK = 0x000000FF;

  private ImageConverter() {
  }

  public static void main(String[] args) throws Exception {
    for (String arg : args) {
      File inputFile = new File(arg);
      if (inputFile.exists()) {
        if (inputFile.isDirectory()) {
          int count = 0;
          for (File input : inputFile.listFiles()) {
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
    
    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        result.setRGB(i, j, src.isBlack(i,j) ? BLACK : WHITE);
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
    if (slashPos != -1 && slashPos != name.length()-1) {
      parent = name.substring(0, slashPos);
      basename = name.substring(slashPos+1);
    } else {
      parent = ".";
      basename = name;
    }
    File parentFile = new File(parent);
    if (!parentFile.exists()) {
      return null;
    }

    File baseFile = new File(parent,basename);
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
      result = new File(name + "_converted." + FORMAT);
    }
    return result;
  }

}
