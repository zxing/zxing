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

package com.google.zxing.common;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.BufferedImageLuminanceSource;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public abstract class AbstractBlackBoxTestCase extends Assert {

  private static final FilenameFilter IMAGE_NAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      String lowerCase = name.toLowerCase(Locale.ENGLISH);
      return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") ||
             lowerCase.endsWith(".gif") || lowerCase.endsWith(".png");
    }
  };

  private final File testBase;
  private final Reader barcodeReader;
  private final BarcodeFormat expectedFormat;
  private final List<TestResult> testResults;

  protected AbstractBlackBoxTestCase(String testBasePathSuffix,
                                     Reader barcodeReader,
                                     BarcodeFormat expectedFormat) {
    // A little workaround to prevent aggravation in my IDE
    File testBase = new File(testBasePathSuffix);
    if (!testBase.exists()) {
      // try starting with 'core' since the test base is often given as the project root
      testBase = new File("core/" + testBasePathSuffix);
    }
    this.testBase = testBase;
    this.barcodeReader = barcodeReader;
    this.expectedFormat = expectedFormat;
    testResults = new ArrayList<TestResult>();
  }

  protected void addTest(int mustPassCount, int tryHarderCount, float rotation) {
    addTest(mustPassCount, tryHarderCount, 0, 0, rotation);
  }

  /**
   * Adds a new test for the current directory of images.
   *
   * @param mustPassCount The number of images which must decode for the test to pass.
   * @param tryHarderCount The number of images which must pass using the try harder flag.
   * @param maxMisreads Maximum number of images which can fail due to successfully reading the wrong contents
   * @param maxTryHarderMisreads Maximum number of images which can fail due to successfully
   *                             reading the wrong contents using the try harder flag
   * @param rotation The rotation in degrees clockwise to use for this test.
   */
  protected void addTest(int mustPassCount,
                         int tryHarderCount,
                         int maxMisreads,
                         int maxTryHarderMisreads,
                         float rotation) {
    testResults.add(new TestResult(mustPassCount, tryHarderCount, maxMisreads, maxTryHarderMisreads, rotation));
  }

  protected File[] getImageFiles() {
    assertTrue("Please run from the 'core' directory", testBase.exists());
    return testBase.listFiles(IMAGE_NAME_FILTER);
  }

  protected Reader getReader() {
    return barcodeReader;
  }

  // This workaround is used because AbstractNegativeBlackBoxTestCase overrides this method but does
  // not return SummaryResults.
  @Test
  public void testBlackBox() throws IOException {
    testBlackBoxCountingResults(true);
  }

  public SummaryResults testBlackBoxCountingResults(boolean assertOnFailure) throws IOException {
    assertFalse(testResults.isEmpty());

    File[] imageFiles = getImageFiles();
    int testCount = testResults.size();

    int[] passedCounts = new int[testCount];
    int[] misreadCounts = new int[testCount];
    int[] tryHarderCounts = new int[testCount];
    int[] tryHaderMisreadCounts = new int[testCount];

    for (File testImage : imageFiles) {
      System.out.printf("Starting %s\n", testImage.getAbsolutePath());

      BufferedImage image = ImageIO.read(testImage);

      String testImageFileName = testImage.getName();
      String fileBaseName = testImageFileName.substring(0, testImageFileName.indexOf('.'));
      File expectedTextFile = new File(testBase, fileBaseName + ".txt");
      String expectedText = readFileAsString(expectedTextFile);

      File expectedMetadataFile = new File(testBase, fileBaseName + ".metadata.txt");
      Properties expectedMetadata = new Properties();
      if (expectedMetadataFile.exists()) {
        InputStream expectedStream = new FileInputStream(expectedMetadataFile);
        try {
          expectedMetadata.load(expectedStream);
        } finally {
          expectedStream.close();
        }
      }

      for (int x = 0; x < testCount; x++) {
        float rotation = testResults.get(x).getRotation();
        BufferedImage rotatedImage = rotateImage(image, rotation);
        LuminanceSource source = new BufferedImageLuminanceSource(rotatedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
          if (decode(bitmap, rotation, expectedText, expectedMetadata, false)) {
            passedCounts[x]++;
          } else {
            misreadCounts[x]++;
          }
        } catch (ReaderException re) {
          // continue
        }
        try {
          if (decode(bitmap, rotation, expectedText, expectedMetadata, true)) {
            tryHarderCounts[x]++;
          } else {
            tryHaderMisreadCounts[x]++;
          }
        } catch (ReaderException re) {
          // continue
        }
      }
    }

    // Print the results of all tests first
    int totalFound = 0;
    int totalMustPass = 0;
    int totalMisread = 0;
    int totalMaxMisread = 0;

    for (int x = 0; x < testResults.size(); x++) {
      TestResult testResult = testResults.get(x);
      System.out.printf("Rotation %d degrees:\n", (int) testResult.getRotation());
      System.out.printf("  %d of %d images passed (%d required)\n",
                        passedCounts[x], imageFiles.length, testResult.getMustPassCount());
      int failed = imageFiles.length - passedCounts[x];
      System.out.printf("    %d failed due to misreads, %d not detected\n",
                        misreadCounts[x], failed - misreadCounts[x]);
      System.out.printf("  %d of %d images passed with try harder (%d required)\n",
                        tryHarderCounts[x], imageFiles.length, testResult.getTryHarderCount());
      failed = imageFiles.length - tryHarderCounts[x];
      System.out.printf("    %d failed due to misreads, %d not detected\n",
                        tryHaderMisreadCounts[x], failed - tryHaderMisreadCounts[x]);
      totalFound += passedCounts[x] + tryHarderCounts[x];
      totalMustPass += testResult.getMustPassCount() + testResult.getTryHarderCount();
      totalMisread += misreadCounts[x] + tryHaderMisreadCounts[x];
      totalMaxMisread += testResult.getMaxMisreads() + testResult.getMaxTryHarderMisreads();
    }

    int totalTests = imageFiles.length * testCount * 2;
    System.out.printf("TOTALS:\nDecoded %d images out of %d (%d%%, %d required)\n",
                      totalFound, totalTests, totalFound * 100 / totalTests, totalMustPass);
    if (totalFound > totalMustPass) {
      System.out.printf("  +++ Test too lax by %d images\n", totalFound - totalMustPass);
    } else if (totalFound < totalMustPass) {
      System.out.printf("  --- Test failed by %d images\n", totalMustPass - totalFound);
    }

    if (totalMisread < totalMaxMisread) {
      System.out.printf("  +++ Test expects too many misreads by %d images\n", totalMaxMisread - totalMisread);
    } else if (totalMisread > totalMaxMisread) {
      System.out.printf("  --- Test had too many misreads by %d images\n", totalMisread - totalMaxMisread);
    }

    System.out.flush();

    // Then run through again and assert if any failed
    if (assertOnFailure) {
      for (int x = 0; x < testCount; x++) {
        TestResult testResult = testResults.get(x);
        String label = "Rotation " + testResult.getRotation() + " degrees: Too many images failed";
        assertTrue(label,
                   passedCounts[x] >= testResult.getMustPassCount());
        assertTrue("Try harder, " + label,
                   tryHarderCounts[x] >= testResult.getTryHarderCount());
        label = "Rotation " + testResult.getRotation() + " degrees: Too many images misread";
        assertTrue(label,
                   misreadCounts[x] <= testResult.getMaxMisreads());
        assertTrue("Try harder, " + label,
                   tryHaderMisreadCounts[x] <= testResult.getMaxTryHarderMisreads());
      }
    }
    return new SummaryResults(totalFound, totalMustPass, totalTests);
  }

  private boolean decode(BinaryBitmap source,
                         float rotation,
                         String expectedText,
                         Map<?,?> expectedMetadata,
                         boolean tryHarder) throws ReaderException {

    String suffix = String.format(" (%srotation: %d)", tryHarder ? "try harder, " : "", (int) rotation);

    Map<DecodeHintType,Object> hints = new EnumMap<DecodeHintType,Object>(DecodeHintType.class);
    if (tryHarder) {
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    Result result = barcodeReader.decode(source, hints);

    if (expectedFormat != result.getBarcodeFormat()) {
      System.out.printf("Format mismatch: expected '%s' but got '%s'%s\n",
                        expectedFormat, result.getBarcodeFormat(), suffix);
      return false;
    }

    String resultText = result.getText();
    if (!expectedText.equals(resultText)) {
      System.out.printf("Content mismatch: expected '%s' but got '%s'%s\n", 
                        expectedText, resultText, suffix);
      return false;
    }

    Map<ResultMetadataType,?> resultMetadata = result.getResultMetadata();
    for (Map.Entry<?,?> metadatum : expectedMetadata.entrySet()) {
      ResultMetadataType key = ResultMetadataType.valueOf(metadatum.getKey().toString());
      Object expectedValue = metadatum.getValue();
      Object actualValue = resultMetadata == null ? null : resultMetadata.get(key);
      if (!expectedValue.equals(actualValue)) {
        System.out.printf("Metadata mismatch for key '%s': expected '%s' but got '%s'\n",
                          key, expectedValue, actualValue);
        return false;
      }
    }

    return true;
  }

  private static String readFileAsString(File file) throws IOException {
    StringBuilder result = new StringBuilder((int) file.length());
    InputStreamReader reader = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF8"));
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

  protected static BufferedImage rotateImage(BufferedImage original, float degrees) {
    if (degrees == 0.0f) {
      return original;
    }
    double radians = Math.toRadians(degrees);

    // Transform simply to find out the new bounding box (don't actually run the image through it)
    AffineTransform at = new AffineTransform();
    at.rotate(radians, original.getWidth() / 2.0, original.getHeight() / 2.0);
    BufferedImageOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

    Rectangle2D r = op.getBounds2D(original);
    int width = (int) Math.ceil(r.getWidth());
    int height = (int) Math.ceil(r.getHeight());

    // Real transform, now that we know the size of the new image and how to translate after we rotate
    // to keep it centered
    at = new AffineTransform();
    at.rotate(radians, width / 2.0, height / 2.0);
    at.translate((width - original.getWidth()) / 2.0,
                 (height - original.getHeight()) / 2.0);
    op = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

    return op.filter(original, null);
  }

}
