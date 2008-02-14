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

/**
 * @author srowen@google.com (Sean Owen)
 */
public abstract class AbstractBlackBoxTestCase extends TestCase {

  private static final FilenameFilter IMAGE_NAME_FILTER = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      String lowerCase = name.toLowerCase();
      return
          lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") ||
              lowerCase.endsWith(".gif") || lowerCase.endsWith(".png") ||
              lowerCase.endsWith(".url");
    }
  };

  private final File testBase;
  private final Reader barcodeReader;
  private final double passPercent;

  protected AbstractBlackBoxTestCase(File testBase, Reader barcodeReader, double passPercent) {
    this.testBase = testBase;
    this.barcodeReader = barcodeReader;
    this.passPercent = passPercent;
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

      String testImageFileName = testImage.getName();
      File expectedTextFile =
          new File(testBase, testImageFileName.substring(0, testImageFileName.indexOf('.')) + ".txt");
      String expectedText = readFileAsString(expectedTextFile);
      String resultText = result.getText();

      if (expectedText.equals(resultText)) {
        passedCount++;
      } else {
        System.out.println("Mismatch: expected '" + expectedText + "' but got '" + resultText + '\'');
      }

    }

    System.out.println(passedCount + " of " + imageFiles.length + " images passed");
    assertTrue("Too many images failed", passedCount >= (int) (imageFiles.length * passPercent));
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