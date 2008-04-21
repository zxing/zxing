/*
 * Copyright 2008 Google Inc.
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

package com.google.zxing.common;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageMonochromeBitmapSource;
import junit.framework.TestCase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;

/**
 * @author srowen@google.com (Sean Owen)
 */
public abstract class AbstractBlackBoxTestCase extends TestCase {

  private static final Hashtable TRY_HARDER_HINT;
  static {
    TRY_HARDER_HINT = new Hashtable();
    TRY_HARDER_HINT.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
  }

  private static final FilenameFilter IMAGE_NAME_FILTER = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      String lowerCase = name.toLowerCase();
      return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") ||
              lowerCase.endsWith(".gif") || lowerCase.endsWith(".png") ||
              lowerCase.endsWith(".url");
    }
  };

  private final File testBase;
  private final Reader barcodeReader;
  private final int mustPassCount;
  private final BarcodeFormat expectedFormat;

  protected AbstractBlackBoxTestCase(File testBase,
                                     Reader barcodeReader,
                                     int mustPassCount,
                                     BarcodeFormat expectedFormat) {
    this.testBase = testBase;
    this.barcodeReader = barcodeReader;
    this.mustPassCount = mustPassCount;
    this.expectedFormat = expectedFormat;
  }

  public void testBlackBox() throws IOException {

    assertTrue("Please run from the 'core' directory", testBase.exists());

    File[] imageFiles = testBase.listFiles(IMAGE_NAME_FILTER);
    int passedCount = 0;
    for (File testImage : imageFiles) {
      System.out.println("Starting " + testImage.getAbsolutePath());

      BufferedImage image;
      if (testImage.getName().endsWith(".url")) {
        String urlString = readFileAsString(testImage);
        image = ImageIO.read(new URL(urlString));
      } else {
        image = ImageIO.read(testImage);
      }
      MonochromeBitmapSource source = new BufferedImageMonochromeBitmapSource(image);
      Result result;
      try {
        result = barcodeReader.decode(source);
      } catch (ReaderException re) {
        System.out.println(re);
        continue;
      }

      if (expectedFormat != result.getBarcodeFormat()) {
        System.out.println("Format mismatch: expected '" + expectedFormat + "' but got '" +
            result.getBarcodeFormat() + '\'');
        continue;
      }

      String testImageFileName = testImage.getName();
      File expectedTextFile = new File(testBase,
          testImageFileName.substring(0, testImageFileName.indexOf('.')) + ".txt");
      String expectedText = readFileAsString(expectedTextFile);
      String resultText = result.getText();

      boolean passed = expectedText.equals(resultText);
      if (passed) {
        passedCount++;
      } else {
        System.out.println("Mismatch: expected '" + expectedText + "' but got '" + resultText + '\'');
        continue;
      }

      // Try "try harder" mode
      try {
        result = barcodeReader.decode(source, TRY_HARDER_HINT);
      } catch (ReaderException re) {
        if (passed) {
          fail("Normal mode succeeded but \"try harder\" failed");
        }
        continue;
      }
      if (expectedFormat != result.getBarcodeFormat()) {
        System.out.println("Try Harder Format mismatch: expected '" + expectedFormat + "' but got '" +
            result.getBarcodeFormat() + '\'');
      } else if (!expectedText.equals(resultText)) {
        System.out.println("Try Harder Mismatch: expected '" + expectedText + "' but got '" +
            resultText + '\'');
      }
    }

    System.out.println(passedCount + " of " + imageFiles.length + " images passed (" +
        mustPassCount + " required)");
    assertTrue("Too many images failed", passedCount >= mustPassCount);
  }

  private static String readFileAsString(File file) throws IOException {
    StringBuilder result = new StringBuilder();
    InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
    try {
      char[] buffer = new char[256];
      int charsRead;
      while ((charsRead = reader.read(buffer)) > 0) {
        result.append(buffer, 0, charsRead);
      }
    } finally {
      reader.close();
    }
    return result.toString();
  }

}