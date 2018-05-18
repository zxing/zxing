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
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public abstract class AbstractBlackBoxTestCase extends Assert {

  private static final Logger log = Logger.getLogger(AbstractBlackBoxTestCase.class.getSimpleName());

  private final Path testBase;
  private final Reader barcodeReader;
  private final BarcodeFormat expectedFormat;
  private final List<TestResult> testResults;

  public static Path buildTestBase(String testBasePathSuffix) {
    // A little workaround to prevent aggravation in my IDE
    Path testBase = Paths.get(testBasePathSuffix);
    if (!Files.exists(testBase)) {
      // try starting with 'core' since the test base is often given as the project root
      testBase = Paths.get("core").resolve(testBasePathSuffix);
    }
    return testBase;
  }

  protected AbstractBlackBoxTestCase(String testBasePathSuffix,
                                     Reader barcodeReader,
                                     BarcodeFormat expectedFormat) {
    this.testBase = buildTestBase(testBasePathSuffix);
    this.barcodeReader = barcodeReader;
    this.expectedFormat = expectedFormat;
    testResults = new ArrayList<>();

    System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%6$s%n");
  }

  protected final Path getTestBase() {
    return testBase;
  }

  protected final void addTest(int mustPassCount, int tryHarderCount, float rotation) {
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
  protected final void addTest(int mustPassCount,
                               int tryHarderCount,
                               int maxMisreads,
                               int maxTryHarderMisreads,
                               float rotation) {
    testResults.add(new TestResult(mustPassCount, tryHarderCount, maxMisreads, maxTryHarderMisreads, rotation));
  }

  protected final List<Path> getImageFiles() throws IOException {
    assertTrue("Please download and install test images, and run from the 'core' directory", Files.exists(testBase));
    List<Path> paths = new ArrayList<>();
    try (DirectoryStream<Path> pathIt = Files.newDirectoryStream(testBase, "*.{jpg,jpeg,gif,png,JPG,JPEG,GIF,PNG}")) {
      for (Path path : pathIt) {
        paths.add(path);
      }
    }
    return paths;
  }

  final Reader getReader() {
    return barcodeReader;
  }

  // This workaround is used because AbstractNegativeBlackBoxTestCase overrides this method but does
  // not return SummaryResults.
  @Test
  public void testBlackBox() throws IOException {
    testBlackBoxCountingResults(true);
  }

  private void testBlackBoxCountingResults(boolean assertOnFailure) throws IOException {
    assertFalse(testResults.isEmpty());

    List<Path> imageFiles = getImageFiles();
    int testCount = testResults.size();

    int[] passedCounts = new int[testCount];
    int[] misreadCounts = new int[testCount];
    int[] tryHarderCounts = new int[testCount];
    int[] tryHarderMisreadCounts = new int[testCount];

    for (Path testImage : imageFiles) {
      log.info(String.format("Starting %s", testImage));

      BufferedImage image = ImageIO.read(testImage.toFile());

      String testImageFileName = testImage.getFileName().toString();
      String fileBaseName = testImageFileName.substring(0, testImageFileName.indexOf('.'));
      Path expectedTextFile = testBase.resolve(fileBaseName + ".txt");
      String expectedText;
      if (Files.exists(expectedTextFile)) {
        expectedText = readFileAsString(expectedTextFile, StandardCharsets.UTF_8);
      } else {
        expectedTextFile = testBase.resolve(fileBaseName + ".bin");
        assertTrue(Files.exists(expectedTextFile));
        expectedText = readFileAsString(expectedTextFile, StandardCharsets.ISO_8859_1);
      }

      Path expectedMetadataFile = testBase.resolve(fileBaseName + ".metadata.txt");
      Properties expectedMetadata = new Properties();
      if (Files.exists(expectedMetadataFile)) {
        try (BufferedReader reader = Files.newBufferedReader(expectedMetadataFile, StandardCharsets.UTF_8)) {
          expectedMetadata.load(reader);
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
        } catch (ReaderException ignored) {
          log.fine(String.format("could not read at rotation %f", rotation));
        }
        try {
          if (decode(bitmap, rotation, expectedText, expectedMetadata, true)) {
            tryHarderCounts[x]++;
          } else {
            tryHarderMisreadCounts[x]++;
          }
        } catch (ReaderException ignored) {
          log.fine(String.format("could not read at rotation %f w/TH", rotation));
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
      log.info(String.format("Rotation %d degrees:", (int) testResult.getRotation()));
      log.info(String.format(" %d of %d images passed (%d required)",
                             passedCounts[x], imageFiles.size(), testResult.getMustPassCount()));
      int failed = imageFiles.size() - passedCounts[x];
      log.info(String.format(" %d failed due to misreads, %d not detected",
                             misreadCounts[x], failed - misreadCounts[x]));
      log.info(String.format(" %d of %d images passed with try harder (%d required)",
                             tryHarderCounts[x], imageFiles.size(), testResult.getTryHarderCount()));
      failed = imageFiles.size() - tryHarderCounts[x];
      log.info(String.format(" %d failed due to misreads, %d not detected",
                             tryHarderMisreadCounts[x], failed - tryHarderMisreadCounts[x]));
      totalFound += passedCounts[x] + tryHarderCounts[x];
      totalMustPass += testResult.getMustPassCount() + testResult.getTryHarderCount();
      totalMisread += misreadCounts[x] + tryHarderMisreadCounts[x];
      totalMaxMisread += testResult.getMaxMisreads() + testResult.getMaxTryHarderMisreads();
    }

    int totalTests = imageFiles.size() * testCount * 2;
    log.info(String.format("Decoded %d images out of %d (%d%%, %d required)",
                           totalFound, totalTests, totalFound * 100 / totalTests, totalMustPass));
    if (totalFound > totalMustPass) {
      log.warning(String.format("+++ Test too lax by %d images", totalFound - totalMustPass));
    } else if (totalFound < totalMustPass) {
      log.warning(String.format("--- Test failed by %d images", totalMustPass - totalFound));
    }

    if (totalMisread < totalMaxMisread) {
      log.warning(String.format("+++ Test expects too many misreads by %d images", totalMaxMisread - totalMisread));
    } else if (totalMisread > totalMaxMisread) {
      log.warning(String.format("--- Test had too many misreads by %d images", totalMisread - totalMaxMisread));
    }

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
                   tryHarderMisreadCounts[x] <= testResult.getMaxTryHarderMisreads());
      }
    }
  }

  private boolean decode(BinaryBitmap source,
                         float rotation,
                         String expectedText,
                         Map<?,?> expectedMetadata,
                         boolean tryHarder) throws ReaderException {

    String suffix = String.format(" (%srotation: %d)", tryHarder ? "try harder, " : "", (int) rotation);

    Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
    if (tryHarder) {
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    // Try in 'pure' mode mostly to exercise PURE_BARCODE code paths for exceptions;
    // not expected to pass, generally
    Result result = null;
    try {
      Map<DecodeHintType,Object> pureHints = new EnumMap<>(hints);
      pureHints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
      result = barcodeReader.decode(source, pureHints);
    } catch (ReaderException re) {
      // continue
    }

    if (result == null) {
      result = barcodeReader.decode(source, hints);
    }

    if (expectedFormat != result.getBarcodeFormat()) {
      log.info(String.format("Format mismatch: expected '%s' but got '%s'%s",
                             expectedFormat, result.getBarcodeFormat(), suffix));
      return false;
    }

    String resultText = result.getText();
    if (!expectedText.equals(resultText)) {
      log.info(String.format("Content mismatch: expected '%s' but got '%s'%s",
                             expectedText, resultText, suffix));
      return false;
    }

    Map<ResultMetadataType,?> resultMetadata = result.getResultMetadata();
    for (Map.Entry<?,?> metadatum : expectedMetadata.entrySet()) {
      ResultMetadataType key = ResultMetadataType.valueOf(metadatum.getKey().toString());
      Object expectedValue = metadatum.getValue();
      Object actualValue = resultMetadata == null ? null : resultMetadata.get(key);
      if (!expectedValue.equals(actualValue)) {
        log.info(String.format("Metadata mismatch for key '%s': expected '%s' but got '%s'",
                               key, expectedValue, actualValue));
        return false;
      }
    }

    return true;
  }

  protected static String readFileAsString(Path file, Charset charset) throws IOException {
    String stringContents = new String(Files.readAllBytes(file), charset);
    if (stringContents.endsWith("\n")) {
      log.info("String contents of file " + file + " end with a newline. " +
                  "This may not be intended and cause a test failure");
    }
    return stringContents;
  }

  protected static BufferedImage rotateImage(BufferedImage original, float degrees) {
    if (degrees == 0.0f) {
      return original;
    }

    switch (original.getType()) {
      case BufferedImage.TYPE_BYTE_INDEXED:
      case BufferedImage.TYPE_BYTE_BINARY:
        BufferedImage argb = new BufferedImage(original.getWidth(),
                                               original.getHeight(),
                                               BufferedImage.TYPE_INT_ARGB);
        Graphics g = argb.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        original = argb;
        break;
    }

    double radians = Math.toRadians(degrees);

    // Transform simply to find out the new bounding box (don't actually run the image through it)
    AffineTransform at = new AffineTransform();
    at.rotate(radians, original.getWidth() / 2.0, original.getHeight() / 2.0);
    BufferedImageOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

    RectangularShape r = op.getBounds2D(original);
    int width = (int) Math.ceil(r.getWidth());
    int height = (int) Math.ceil(r.getHeight());

    // Real transform, now that we know the size of the new image and how to translate after we rotate
    // to keep it centered
    at = new AffineTransform();
    at.rotate(radians, width / 2.0, height / 2.0);
    at.translate((width - original.getWidth()) / 2.0,
                 (height - original.getHeight()) / 2.0);
    op = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

    return op.filter(original, new BufferedImage(width, height, original.getType()));
  }

}
