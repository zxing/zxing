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

package com.google.zxing.pdf417.decoder;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.pdf417.PDF417Common;
import com.google.zxing.pdf417.PDF417DecoderResult;
import com.google.zxing.pdf417.decoder.SimpleLog.LEVEL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Guenther Grau
 */
public final class PDF417ScanningDecoder {

  private static final int STOP_PATTERN_VALUE = 130324;
  private static final int CODEWORD_SKEW_SIZE = 2;
  private static final int MIN_BARCODE_ROWS = 3;
  private static final int MAX_BARCODE_ROWS = 90;

  public static DecoderResult decode(BitMatrix image, final ResultPoint imageTopLeft,
                                     final ResultPoint imageBottomLeft, final ResultPoint imageTopRight,
                                     final ResultPoint imageBottomRight, int minCodewordWidth, int maxCodewordWidth)
      throws NotFoundException, FormatException, ChecksumException {
    BoundingBox boundingBox = new BoundingBox(image, imageTopLeft, imageBottomLeft, imageTopRight, imageBottomRight);
    DetectionResultColumn leftRowIndicatorColumn = null;
    DetectionResultColumn rightRowIndicatorColumn = null;
    if (imageTopLeft != null) {
      leftRowIndicatorColumn = getRowIndicatorColumn(image, boundingBox, true, (int) imageTopLeft.getX(),
          (int) imageTopLeft.getY(), (int) imageBottomLeft.getY(), minCodewordWidth, maxCodewordWidth);
    }
    if (imageTopRight != null) {
      rightRowIndicatorColumn = getRowIndicatorColumn(image, boundingBox, false, (int) imageTopRight.getX(),
          (int) imageTopRight.getY(), (int) imageBottomRight.getY(), minCodewordWidth, maxCodewordWidth);
    }
    DetectionResult detectionResult = merge(getDetectionResult(image, leftRowIndicatorColumn, true),
        getDetectionResult(image, rightRowIndicatorColumn, false));
    if (detectionResult == null) {
      throw NotFoundException.getNotFoundInstance();
    }
    // TODO adjust bounding box to reflect the results from the row indicator columns. Could be that we didn't find
    // the correct min or max row numbers when doing the start and stop pattern search.
    detectionResult.setDetectionResultColumn(0, leftRowIndicatorColumn);
    detectionResult.setDetectionResultColumn(detectionResult.getBarcodeColumnCount() + 1, rightRowIndicatorColumn);
    int maxBarcodeColumn = detectionResult.getBarcodeColumnCount() + 1;
    boolean leftToRight = leftRowIndicatorColumn != null;
    for (int barcodeColumnCount = 1; barcodeColumnCount <= maxBarcodeColumn; barcodeColumnCount++) {
      int barcodeColumn = leftToRight ? barcodeColumnCount : maxBarcodeColumn - barcodeColumnCount;
      // not sure if reading the ending row indicator column again will add any value, so we'll safe us the trouble 
      if (detectionResult.getDetectionResultColumn(barcodeColumn) != null) {
        continue;
      }
      DetectionResultColumn detectionResultColumn = new DetectionResultColumn(boundingBox);
      if (detectionResult.getDetectionResultColumn(barcodeColumn) == null) {
        detectionResult.setDetectionResultColumn(barcodeColumn, detectionResultColumn);
      }
      int startColumn = -1;
      int previousStartColumn = startColumn;
      for (int imageRow = boundingBox.getMinY(); imageRow < boundingBox.getMaxY(); imageRow++) {
        startColumn = getStartColumn(detectionResult, barcodeColumn, imageRow, leftToRight);
        if (startColumn < 0 || startColumn > boundingBox.getMaxX()) {
          if (previousStartColumn == -1) {
            SimpleLog.log(LEVEL.WARNING, "Cannot find startColumn, skipping column");
            continue;
          }
          startColumn = previousStartColumn;
        }
        Codeword codeword = detectCodeword(image, boundingBox.getMinX(), boundingBox.getMaxX(), leftToRight,
            startColumn, imageRow, minCodewordWidth, maxCodewordWidth);
        if (codeword != null) {
          detectionResultColumn.setCodeword(imageRow, codeword);
          previousStartColumn = startColumn;
          minCodewordWidth = Math.min(minCodewordWidth, codeword.getWidth());
          maxCodewordWidth = Math.max(maxCodewordWidth, codeword.getWidth());
        }
      }
    }
    return createDecoderResult(detectionResult);
  }

  private static DetectionResult merge(DetectionResult leftDetectionResult, DetectionResult rightDetectionResult) {
    if (leftDetectionResult == null) {
      return rightDetectionResult;
    }
    if (rightDetectionResult == null) {
      return leftDetectionResult;
    }
    if (leftDetectionResult.getBarcodeColumnCount() != rightDetectionResult.getBarcodeColumnCount() &&
        leftDetectionResult.getBarcodeECLevel() != rightDetectionResult.getBarcodeECLevel() &&
        leftDetectionResult.getBarcodeRowCount() != rightDetectionResult.getBarcodeRowCount()) {
      return null;
    }

    return leftDetectionResult;
  }

  private static DetectionResultColumn getRowIndicatorColumn(BitMatrix image, BoundingBox boundingBox,
                                                             boolean leftToRight, int startColumn, int startRow,
                                                             int endRow, int minCodewordWidth, int maxCodewordWidth) {
    DetectionResultColumn rowIndicatorColumn = new DetectionResultColumn(boundingBox);
    for (int imageRow = startRow; imageRow < endRow; imageRow++) {
      Codeword codeword = detectCodeword(image, 0, image.getWidth(), leftToRight, startColumn, imageRow,
          minCodewordWidth, maxCodewordWidth);
      if (codeword != null) {
        rowIndicatorColumn.setCodeword(imageRow, codeword);
        if (leftToRight) {
          startColumn = codeword.getStartX();
        } else {
          startColumn = codeword.getEndX();
        }
      }
    }
    return rowIndicatorColumn;
  }

  private static DecoderResult createDecoderResult(DetectionResult detectionResult) throws NotFoundException,
      FormatException, ChecksumException {
    BarcodeMatrix barcodeMatrix = createBarcodeMatrix(detectionResult);
    SimpleLog.log(LEVEL.DEVEL, barcodeMatrix);
    Integer numberOfCodewords = barcodeMatrix.getValue(0, 1);
    int calculatedNumberOfCodewords = detectionResult.getBarcodeColumnCount() * detectionResult.getBarcodeRowCount() -
        getNumberOfECCodeWords(detectionResult.getBarcodeECLevel());
    if (numberOfCodewords == null) {
      SimpleLog.log(LEVEL.WARNING, "Error, number of codewords not found");
      if (calculatedNumberOfCodewords < 1 || calculatedNumberOfCodewords > 900) {
        SimpleLog.log(LEVEL.FATAL, "Error, invalid calculatedNumberOfCodewords " + calculatedNumberOfCodewords);
        throw NotFoundException.getNotFoundInstance();
      }
      barcodeMatrix.setValue(0, 1, calculatedNumberOfCodewords);
    } else if (numberOfCodewords != calculatedNumberOfCodewords) {
      SimpleLog.log(LEVEL.WARNING, "Error, number of codewords " + numberOfCodewords + " doesn't match calculated: " +
          calculatedNumberOfCodewords);
      // The calculated one is more reliable as it is derived from the row indicator column
      numberOfCodewords = calculatedNumberOfCodewords;
    }
    List<Integer> erasures = new ArrayList<Integer>();
    int[] codewords = new int[detectionResult.getBarcodeRowCount() * detectionResult.getBarcodeColumnCount()];
    for (int row = 0; row < detectionResult.getBarcodeRowCount(); row++) {
      for (int column = 0; column < detectionResult.getBarcodeColumnCount(); column++) {
        Integer codeword = barcodeMatrix.getValue(row, column + 1);
        if (codeword == null) {
          erasures.add(row * detectionResult.getBarcodeColumnCount() + column);
        } else {
          codewords[row * detectionResult.getBarcodeColumnCount() + column] = codeword;
        }
      }
    }
    return decodeCodewords(codewords, detectionResult.getBarcodeECLevel(), getErasureArray(erasures));
  }

  private static BarcodeMatrix createBarcodeMatrix(DetectionResult detectionResult) {
    BarcodeMatrix barcodeMatrix = new BarcodeMatrix();

    int column = -1;
    for (DetectionResultColumn detectionResultColumn : detectionResult.getDetectionResultColumns()) {
      column++;
      if (detectionResultColumn == null) {
        continue;
      }
      for (Codeword codeword : detectionResultColumn.getCodewords()) {
        if (codeword == null) {
          continue;
        }
        barcodeMatrix.setValue(codeword.getRowNumber(), column, codeword.getValue());
      }
    }
    return barcodeMatrix;
  }

  private static int getStartColumn(DetectionResult detectionResult, int barcodeColumn, int imageRow,
                                    boolean leftToRight) {
    int offset = leftToRight ? 1 : -1;
    Codeword codeword = detectionResult.getDetectionResultColumn(barcodeColumn - offset).getCodeword(imageRow);
    if (codeword != null) {
      return leftToRight ? codeword.getEndX() : codeword.getStartX();
    }
    codeword = detectionResult.getDetectionResultColumn(barcodeColumn).getCodewordNearby(imageRow);
    if (codeword != null) {
      return leftToRight ? codeword.getStartX() : codeword.getEndX();
    }
    codeword = detectionResult.getDetectionResultColumn(barcodeColumn - offset).getCodewordNearby(imageRow);
    if (codeword != null) {
      return leftToRight ? codeword.getEndX() : codeword.getStartX();
    }
    SimpleLog.log(LEVEL.INFO, "Cannot find accurate start column");
    int skippedColumns = 0;

    while (true) {
      barcodeColumn -= offset;
      for (Codeword previousRowCodeword : detectionResult.getDetectionResultColumn(barcodeColumn).getCodewords()) {
        if (previousRowCodeword != null) {
          return (leftToRight ? previousRowCodeword.getEndX() : previousRowCodeword.getStartX()) + offset *
              skippedColumns * (previousRowCodeword.getEndX() - previousRowCodeword.getStartX());
        }
      }
      skippedColumns++;
    }
  }

  // could add parameter to make it work for the right row indicator as well
  private static DetectionResult getDetectionResult(BitMatrix image, DetectionResultColumn rowIndicatorColumn,
                                                    boolean isLeftIndicatorColumn) {
    if (rowIndicatorColumn == null) {
      return null;
    }
    Codeword[] codewords = rowIndicatorColumn.getCodewords();
    BarcodeValue barcodeColumnCount = new BarcodeValue();
    BarcodeValue barcodeRowCount = new BarcodeValue();
    BarcodeValue barcodeRowCountOffset = new BarcodeValue();
    BarcodeValue barcodeECLevel = new BarcodeValue();
    for (Codeword codeword : codewords) {
      if (codeword == null) {
        continue;
      }
      codeword.setRowNumberAsRowIndicatorColumn();
      int rowIndicatorValue = codeword.getValue() % 30;
      int codewordRowNumber = codeword.getRowNumber();
      if (!isLeftIndicatorColumn) {
        codewordRowNumber += 2;
      }
      switch (codewordRowNumber % 3) {
        case 0:
          barcodeRowCount.setValue(rowIndicatorValue * 3 + 1);
          break;
        case 1:
          barcodeECLevel.setValue(rowIndicatorValue / 3);
          barcodeRowCountOffset.setValue(rowIndicatorValue % 3);
          break;
        case 2:
          barcodeColumnCount.setValue(rowIndicatorValue + 1);
          break;
      }
    }
    if ((barcodeColumnCount.getValue() == null) || (barcodeRowCount.getValue() == null) ||
        (barcodeRowCountOffset.getValue() == null) || (barcodeECLevel.getValue() == null) ||
        barcodeColumnCount.getValue() < 1 ||
        barcodeRowCount.getValue() + barcodeRowCountOffset.getValue() < MIN_BARCODE_ROWS ||
        barcodeRowCount.getValue() + barcodeRowCountOffset.getValue() > MAX_BARCODE_ROWS) {
      return null;
    }

    return new DetectionResult(image, barcodeColumnCount.getValue(), barcodeRowCount.getValue() +
        barcodeRowCountOffset.getValue(), barcodeECLevel.getValue(), rowIndicatorColumn.getBoundingBox());
  }

  private static Codeword detectCodeword(BitMatrix image, int minColumn, int maxColumn, boolean leftToRight,
                                         int startColumn, int imageRow, int minCodewordWidth, int maxCodewordWidth) {
    startColumn = adjustCodewordStartColumn(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
    // we usually know fairly exact now how long a codeword is. We should provide minimum and maximum expected length
    // and try to adjust the read pixels, e.g. remove single pixel errors or try to cut off exceeding pixels.
    int[] moduleBitCount = getModuleBitCount(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
    if (moduleBitCount == null) {
      return null;
    }
    int endColumn;
    int codewordBitCount = PDF417Common.getBitCountSum(moduleBitCount);
    if (!leftToRight) {
      for (int i = 0; i < moduleBitCount.length >> 1; i++) {
        int tmpCount = moduleBitCount[i];
        moduleBitCount[i] = moduleBitCount[moduleBitCount.length - 1 - i];
        moduleBitCount[moduleBitCount.length - 1 - i] = tmpCount;
      }
      endColumn = startColumn;
      startColumn = endColumn - codewordBitCount;
    } else {
      endColumn = startColumn + codewordBitCount;
    }
    // TODO implement check for width and correction of black and white bars
    // use start (and maybe stop pattern) to determine if blackbars are wider than white bars. If so, adjust.
    // should probably done only for codewords with a lot more than 17 bits. 
    // The following fixes 10-1.png, which has wide black bars and small white bars
    //    for (int i = 0; i < moduleBitCount.length; i++) {
    //      if (i % 2 == 0) {
    //        moduleBitCount[i]--;
    //      } else {
    //        moduleBitCount[i]++;
    //      }
    //    }

    // We could also use the width of surrounding codewords for more accurate results, but this seems
    // sufficient for now
    if (!checkCodewordSkew(codewordBitCount, minCodewordWidth, maxCodewordWidth)) {
      // We could try to use the startX and endX position of the codeword in the same column in the previous row,
      // create the bit count from it and normalize it to 8. This would help with single pixel errors.
      SimpleLog.log(LEVEL.INFO, "Invalid codeword size " + codewordBitCount + ", skipping", imageRow, startColumn);
      return null;
    }
    int[] bitCountCopy = Arrays.copyOf(moduleBitCount, moduleBitCount.length);
    AdjustmentResults adjustmentResults = PDF417CodewordDecoder.adjustBitCount(moduleBitCount);
    int codeword = -1;
    for (int resultIndex = 0; resultIndex < adjustmentResults.size() && codeword == -1; resultIndex++) {
      moduleBitCount = adjustmentResults.get(resultIndex).getModuleCount();
      long decodedValue = getDecodedValue(moduleBitCount);
      if (decodedValue == STOP_PATTERN_VALUE) {
        return null;
      }
      codeword = BitMatrixParser.getCodeword(decodedValue);
    }
    if (codeword == -1) {
      SimpleLog.log(LEVEL.INFO, "Invalid barcode symbol for original pixel count: " + getBitCounts(bitCountCopy) +
          ", Results: " + adjustmentResults.size(), imageRow, endColumn);
      for (int resultIndex = 0; resultIndex < adjustmentResults.size(); resultIndex++) {
        SimpleLog.log(LEVEL.INFO, "Result[" + resultIndex + "]: " +
            getBitCounts(adjustmentResults.get(resultIndex).getModuleCount()), imageRow, endColumn);
      }
      return null;
    }
    return new Codeword(startColumn, endColumn, getCodewordBucketNumber(moduleBitCount), codeword);
  }

  private static int[] getModuleBitCount(BitMatrix image, int minColumn, int maxColumn, boolean leftToRight,
                                         int startColumn, int imageRow) {
    int imageColumn = startColumn;
    int[] moduleBitCount = new int[8];
    int moduleNumber = 0;
    final int increment = leftToRight ? 1 : -1;
    boolean previousPixelValue = leftToRight;
    while (((leftToRight && imageColumn < maxColumn) || (!leftToRight && imageColumn >= minColumn)) &&
        moduleNumber < moduleBitCount.length) {
      if (image.get(imageColumn, imageRow) == previousPixelValue) {
        moduleBitCount[moduleNumber]++;
        imageColumn += increment;
      } else {
        moduleNumber++;
        previousPixelValue = !previousPixelValue;
      }
    }
    if (moduleNumber != moduleBitCount.length) {
      SimpleLog.log(LEVEL.INFO, "incomplete codeword", imageRow, imageColumn);
      return null;
    }
    return moduleBitCount;
  }

  private static String getBitCounts(int[] moduleBitCount) {
    StringBuilder result = new StringBuilder("{");
    for (int bitCount : moduleBitCount) {
      result.append(bitCount).append(',');
    }
    result.setLength(result.length() - 1);
    return result.append('}').toString();
  }

  private static int getNumberOfECCodeWords(int barcodeECLevel) {
    return 2 << barcodeECLevel;
  }

  private static int adjustCodewordStartColumn(BitMatrix image, final int minColumn, final int maxColumn,
                                               boolean leftToRight, final int codewordStartColumn, final int imageRow) {
    int correctedStartColumn = codewordStartColumn;
    int increment = leftToRight ? -1 : 1;
    // there should be no black pixels before the start column. If there are, then we need to start earlier.
    for (int i = 0; i < 2; i++) {
      while (((leftToRight && correctedStartColumn >= minColumn) || (!leftToRight && correctedStartColumn < maxColumn)) &&
          leftToRight == image.get(correctedStartColumn, imageRow)) {
        if (Math.abs(codewordStartColumn - correctedStartColumn) > CODEWORD_SKEW_SIZE) {
          SimpleLog.log(LEVEL.INFO, "Corrected start position would deviate too much, using previous start position",
              imageRow, codewordStartColumn);
          return codewordStartColumn;
        }
        correctedStartColumn += increment;
      }
      increment = -increment;
      leftToRight = !leftToRight;
    }
    return correctedStartColumn;
  }

  private static boolean checkCodewordSkew(int codewordSize, int minCodewordWidth, int maxCodewordWidth) {
    return minCodewordWidth - CODEWORD_SKEW_SIZE <= codewordSize &&
        codewordSize <= maxCodewordWidth + CODEWORD_SKEW_SIZE;
  }

  private static int[] getErasureArray(List<Integer> list) {
    int[] result = new int[list.size()];
    int i = 0;
    for (Integer integer : list) {
      result[i++] = integer;
    }
    return result;
  }

  public static DecoderResult decodeCodewords(int[] codewords, int ecLevel, int[] erasures) throws FormatException,
      ChecksumException {
    if (codewords.length == 0) {
      throw FormatException.getFormatInstance();
    }
    SimpleLog.log(LEVEL.DEVEL, "Codewords: " + codewords.length + ", Erasures: " + erasures.length + ", ecLevel: " +
        ecLevel);

    int numECCodewords = 1 << (ecLevel + 1);

    int correctedErrorsCount = Decoder.correctErrors(codewords, erasures, numECCodewords);
    Decoder.verifyCodewordCount(codewords, numECCodewords);

    // Decode the codewords
    PDF417DecoderResult decorderResult = DecodedBitStreamParser.decode(codewords, String.valueOf(ecLevel));
    decorderResult.getResultMetadata().setCorrectedErrorsCount(correctedErrorsCount);
    decorderResult.getResultMetadata().setErasureCount(erasures.length);
    return decorderResult;
  }

  private static int getCodewordBucketNumber(int[] moduleBitCount) {
    return (moduleBitCount[0] - moduleBitCount[2] + moduleBitCount[4] - moduleBitCount[6] + 9) % 9;
  }

  private static long getDecodedValue(int[] moduleBitCount) {
    long result = 0;
    for (int i = 0; i < moduleBitCount.length; i++) {
      for (int bit = 0; bit < moduleBitCount[i]; bit++) {
        result = (result << 1) | (i % 2 == 0 ? 1 : 0);
      }
    }
    return result;
  }
}
