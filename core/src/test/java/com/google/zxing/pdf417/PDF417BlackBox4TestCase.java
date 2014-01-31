/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.pdf417;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.BufferedImageLuminanceSource;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.common.AbstractBlackBoxTestCase;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.SummaryResults;
import com.google.zxing.common.TestResult;

import com.google.zxing.multi.MultipleBarcodeReader;
import org.junit.Test;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * This class tests Macro PDF417 barcode specific functionality. It ensures that information, which is split into
 * several barcodes can be properly combined again to yield the original data content.
 * 
 * @author Guenther Grau
 */
public final class PDF417BlackBox4TestCase extends AbstractBlackBoxTestCase {
  private static final Logger log = Logger.getLogger(AbstractBlackBoxTestCase.class.getSimpleName());

  private final MultipleBarcodeReader barcodeReader = new PDF417Reader();

  private final List<TestResult> testResults = new ArrayList<>();

  public PDF417BlackBox4TestCase() {
    super("src/test/resources/blackbox/pdf417-4", null, BarcodeFormat.PDF_417);
    testResults.add(new TestResult(2, 2, 0, 0, 0.0f));
  }

  @Test
  @Override
  public void testBlackBox() throws IOException {
    testPDF417BlackBoxCountingResults(true);
  }

  SummaryResults testPDF417BlackBoxCountingResults(boolean assertOnFailure) throws IOException {
    assertFalse(testResults.isEmpty());

    Map<String,List<Path>> imageFiles = getImageFileLists();
    int testCount = testResults.size();

    int[] passedCounts = new int[testCount];
    int[] misreadCounts = new int[testCount];
    int[] tryHarderCounts = new int[testCount];
    int[] tryHaderMisreadCounts = new int[testCount];

    Path testBase = getTestBase();

    for (Entry<String,List<Path>> testImageGroup : imageFiles.entrySet()) {
      log.fine(String.format("Starting Image Group %s", testImageGroup.getKey()));

      String fileBaseName = testImageGroup.getKey();
      String expectedText;
      Path expectedTextFile = testBase.resolve(fileBaseName + ".txt");
      if (Files.exists(expectedTextFile)) {
        expectedText = readFileAsString(expectedTextFile, StandardCharsets.UTF_8);
      } else {
        expectedTextFile = testBase.resolve(fileBaseName + ".bin");
        assertTrue(Files.exists(expectedTextFile));
        expectedText = readFileAsString(expectedTextFile, StandardCharsets.ISO_8859_1);
      }

      for (int x = 0; x < testCount; x++) {
        List<Result> results = new ArrayList<>();
        for (Path imageFile : testImageGroup.getValue()) {
          BufferedImage image = ImageIO.read(imageFile.toFile());
          float rotation = testResults.get(x).getRotation();
          BufferedImage rotatedImage = rotateImage(image, rotation);
          LuminanceSource source = new BufferedImageLuminanceSource(rotatedImage);
          BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

          try {
            results.addAll(Arrays.asList(decode(bitmap, false)));
          } catch (ReaderException ignored) {
            // ignore
          }
        }
        Collections.sort(results, new Comparator<Result>() {
          @Override
          public int compare(Result arg0, Result arg1) {
            PDF417ResultMetadata resultMetadata = getMeta(arg0);
            PDF417ResultMetadata otherResultMetadata = getMeta(arg1);
            return resultMetadata.getSegmentIndex() - otherResultMetadata.getSegmentIndex();
          }
        });
        StringBuilder resultText = new StringBuilder();
        String fileId = null;
        for (Result result : results) {
          PDF417ResultMetadata resultMetadata = getMeta(result);
          assertNotNull("resultMetadata", resultMetadata);
          if (fileId == null) {
            fileId = resultMetadata.getFileId();
          }
          assertEquals("FileId", fileId, resultMetadata.getFileId());
          resultText.append(result.getText());
        }
        assertEquals("ExpectedText", expectedText, resultText.toString());
        passedCounts[x]++;
        tryHarderCounts[x]++;
      }
    }

    // Print the results of all tests first
    int totalFound = 0;
    int totalMustPass = 0;
    int totalMisread = 0;
    int totalMaxMisread = 0;

    int numberOfTests = imageFiles.keySet().size();
    for (int x = 0; x < testResults.size(); x++) {
      TestResult testResult = testResults.get(x);
      log.info(String.format("Rotation %d degrees:", (int) testResult.getRotation()));
      log.info(String.format(" %d of %d images passed (%d required)", passedCounts[x], numberOfTests,
          testResult.getMustPassCount()));
      int failed = numberOfTests - passedCounts[x];
      log.info(String
          .format(" %d failed due to misreads, %d not detected", misreadCounts[x], failed - misreadCounts[x]));
      log.info(String.format(" %d of %d images passed with try harder (%d required)", tryHarderCounts[x],
          numberOfTests, testResult.getTryHarderCount()));
      failed = numberOfTests - tryHarderCounts[x];
      log.info(String.format(" %d failed due to misreads, %d not detected", tryHaderMisreadCounts[x], failed -
          tryHaderMisreadCounts[x]));
      totalFound += passedCounts[x] + tryHarderCounts[x];
      totalMustPass += testResult.getMustPassCount() + testResult.getTryHarderCount();
      totalMisread += misreadCounts[x] + tryHaderMisreadCounts[x];
      totalMaxMisread += testResult.getMaxMisreads() + testResult.getMaxTryHarderMisreads();
    }

    int totalTests = numberOfTests * testCount * 2;
    log.info(String.format("Decoded %d images out of %d (%d%%, %d required)", totalFound, totalTests, totalFound *
        100 /
        totalTests, totalMustPass));
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
        assertTrue(label, passedCounts[x] >= testResult.getMustPassCount());
        assertTrue("Try harder, " + label, tryHarderCounts[x] >= testResult.getTryHarderCount());
        label = "Rotation " + testResult.getRotation() + " degrees: Too many images misread";
        assertTrue(label, misreadCounts[x] <= testResult.getMaxMisreads());
        assertTrue("Try harder, " + label, tryHaderMisreadCounts[x] <= testResult.getMaxTryHarderMisreads());
      }
    }
    return new SummaryResults(totalFound, totalMustPass, totalTests);
  }

  private static PDF417ResultMetadata getMeta(Result result) {
    return result.getResultMetadata() == null ? null : (PDF417ResultMetadata) result.getResultMetadata().get(
        ResultMetadataType.PDF417_EXTRA_METADATA);
  }

  private Result[] decode(BinaryBitmap source, boolean tryHarder) throws ReaderException {
    Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
    if (tryHarder) {
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    return barcodeReader.decodeMultiple(source, hints);
  }

  private Map<String,List<Path>> getImageFileLists() throws IOException {
    Map<String,List<Path>> result = new HashMap<>();
    for (Path file : getImageFiles()) {
      String testImageFileName = file.getFileName().toString();
      String fileBaseName = testImageFileName.substring(0, testImageFileName.indexOf('-'));
      List<Path> files = result.get(fileBaseName);
      if (files == null) {
        files = new ArrayList<>();
        result.put(fileBaseName, files);
      }
      files.add(file);
    }
    return result;
  }

}
