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
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.TransformableBitMatrix;
import com.google.zxing.pdf417.PDF417Common;
import com.google.zxing.pdf417.decoder.SimpleLog.LEVEL;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Guenther Grau
 */
public final class PDF417ScanningDecoder {

  private static final int STOP_PATTERN_VALUE = 130324;
  private static final int CODEWORD_SKEW_SIZE = 2;
  private static final int MIN_BARCODE_ROWS = 3;
  private static final int MAX_BARCODE_ROWS = 90;

  public static DecoderResult decode(TransformableBitMatrix image, final ResultPoint imageTopLeft,
                                     final ResultPoint imageBottomLeft, final ResultPoint imageTopRight,
                                     final ResultPoint imageBottomRight, int minCodewordWidth,
                                     int maxCodewordWidth) throws NotFoundException, FormatException,
      ChecksumException {
    BoundingBox boundingBox = new BoundingBox(imageTopLeft, imageBottomLeft, imageTopRight, imageBottomRight,
        maxCodewordWidth);
    DetectionResultColumn detectionResultColumn = new DetectionResultColumn(boundingBox);

    int barcodeColumn = 0;
    int startColumn = (int) imageTopLeft.getX();
    for (int imageRow = (int) imageTopLeft.getY(); imageRow < imageBottomLeft.getY(); imageRow++) {
      Codeword codeword = detectCodeword(image, startColumn, imageRow, minCodewordWidth, maxCodewordWidth);
      if (codeword != null) {
        detectionResultColumn.setCodeword(imageRow, codeword);
        startColumn = codeword.getStartX();
      }
    }
    DetectionResult detectionResult = getDetectionResult(image, detectionResultColumn);
    if (detectionResult == null) {
      // If detectionResult is null, we could try the right row indicator column. This should happen very
      // rarely, though, as it means that the complete left row indicator column was unreadable. If someone
      // provides a test PDF, I'll look into it.
      throw NotFoundException.getNotFoundInstance();
    }

    detectionResult.setDetectionResultColumn(barcodeColumn, detectionResultColumn);
    int maxBarcodeColumn = detectionResult.getBarcodeColumnCount() + 2;
    for (barcodeColumn = 1; barcodeColumn < maxBarcodeColumn; barcodeColumn++) {
      detectionResultColumn = new DetectionResultColumn(boundingBox);
      detectionResult.setDetectionResultColumn(barcodeColumn, detectionResultColumn);
      startColumn = -1;
      int previousStartColumn = startColumn;
      for (int imageRow = boundingBox.getMinY(); imageRow < boundingBox.getMaxY(); imageRow++) {
        startColumn = getStartColumn(detectionResult, barcodeColumn, imageRow);
        if (startColumn == -1 || startColumn > boundingBox.getMaxX()) {
          if (previousStartColumn == -1) {
            SimpleLog.log(LEVEL.ERROR, "Cannot find startColumn, skipping column");
            continue;
          }
          startColumn = previousStartColumn;
        }
        Codeword codeword = detectCodeword(image, startColumn, imageRow, minCodewordWidth, maxCodewordWidth);
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

  private static DecoderResult createDecoderResult(DetectionResult detectionResult) throws NotFoundException,
      FormatException, ChecksumException {
    BarcodeMatrix barcodeMatrix = createBarcodeMatrix(detectionResult);
    SimpleLog.log(LEVEL.DEBUG, barcodeMatrix);
    Integer numberOfCodewords = barcodeMatrix.getValue(0, 1);
    int calculatedNumberOfCodewords = detectionResult.getBarcodeColumnCount() *
        detectionResult.getBarcodeRowCount() - getNumberOfECCodeWords(detectionResult.getBarcodeECLevel());
    if (numberOfCodewords == null) {
      SimpleLog.log(LEVEL.WARNING, "Error, number of codewords not found");
      if (calculatedNumberOfCodewords < 1 || calculatedNumberOfCodewords > 900) {
        SimpleLog.log(LEVEL.FATAL, "Error, invalid calculatedNumberOfCodewords " +
            calculatedNumberOfCodewords);
        throw NotFoundException.getNotFoundInstance();
      }
      barcodeMatrix.setValue(0, 1, calculatedNumberOfCodewords);
    } else if (numberOfCodewords != calculatedNumberOfCodewords) {
      SimpleLog.log(LEVEL.WARNING, "Error, number of codewords " + numberOfCodewords +
          " doesn't match calculated: " + calculatedNumberOfCodewords);
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

  private static int getStartColumn(DetectionResult detectionResult, int barcodeColumn, int imageRow) {
    Codeword codeword = detectionResult.getDetectionResultColumn(barcodeColumn - 1).getCodeword(imageRow);
    if (codeword != null) {
      return codeword.getEndX();
    }
    codeword = detectionResult.getDetectionResultColumn(barcodeColumn).getCodewordNearby(imageRow);
    if (codeword != null) {
      return codeword.getStartX();
    }
    codeword = detectionResult.getDetectionResultColumn(barcodeColumn - 1).getCodewordNearby(imageRow);
    if (codeword != null) {
      return codeword.getEndX();
    }
    SimpleLog.log(LEVEL.INFO, "Cannot find accurate start column");
    int skippedColumns = 0;
    while (barcodeColumn > 0) {
      barcodeColumn--;
      for (Codeword previousRowCodeword : detectionResult.getDetectionResultColumn(barcodeColumn).getCodewords()) {
        if (previousRowCodeword != null) {
          return previousRowCodeword.getEndX() + skippedColumns *
              (previousRowCodeword.getEndX() - previousRowCodeword.getStartX());
        }
      }
      skippedColumns++;
    }
    SimpleLog.log(LEVEL.ERROR, "Estimated start column not found.", imageRow, barcodeColumn);
    return -1;
  }

  // could add parameter to make it work for the right row indicator as well
  private static DetectionResult getDetectionResult(TransformableBitMatrix image,
                                                    DetectionResultColumn detectionResultColumn) {
    Codeword[] codewords = detectionResultColumn.getCodewords();
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
      switch (codeword.getRowNumber() % 3) {
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
        barcodeRowCountOffset.getValue(), barcodeECLevel.getValue(), detectionResultColumn.getBoundingBox());
  }

  private static Codeword detectCodeword(TransformableBitMatrix image, int startColumn, int imageRow,
                                         int minCodewordWidth, int maxCodewordWidth) {
    startColumn = adjustCodewordStartColumn(image, startColumn, imageRow);
    int[] moduleBitCount = getModuleBitCount(image, startColumn, imageRow);
    if (moduleBitCount == null) {
      return null;
    }
    int codewordBitCount = PDF417Common.getBitCountSum(moduleBitCount);
    int endColumn = startColumn + codewordBitCount;
    // We could also use the width of surrounding codewords for more accurate results, but this seems
    // sufficient for now
    if (!checkCodewordSkew(codewordBitCount, minCodewordWidth, maxCodewordWidth)) {
      // We could try to use the startX and endX position of the codeword in the same column in the previous row,
      // create the bit count from it and normalize it to 8. This would help with single pixel errors.
      SimpleLog.log(LEVEL.INFO, "Invalid codeword size " + codewordBitCount + ", skipping", imageRow,
          startColumn);
      return null;
    }

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
      SimpleLog.log(LEVEL.INFO, "Invalid barcode symbol for moduleBitCount: " + getBitCounts(moduleBitCount),
          imageRow, endColumn);
      return null;
    }
    return new Codeword(startColumn, endColumn, getCodewordBucketNumber(moduleBitCount), codeword);
  }

  private static int[] getModuleBitCount(TransformableBitMatrix image, int startColumn, int imageRow) {
    final int imageWidth = image.getWidth();
    int imageColumn = startColumn;
    int[] moduleBitCount = new int[8];
    int moduleNumber = 0;
    boolean previousPixelValue = true;
    while (imageColumn < imageWidth && moduleNumber < moduleBitCount.length) {
      if (image.get(imageColumn, imageRow) == previousPixelValue) {
        moduleBitCount[moduleNumber]++;
        imageColumn++;
      } else {
        moduleNumber++;
        previousPixelValue = !previousPixelValue;
      }
    }
    if (imageColumn < imageWidth && moduleNumber != moduleBitCount.length) {
      SimpleLog.log(LEVEL.INFO, "incomplete codeword, stop processing current row", imageRow, imageColumn);
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

  private static int adjustCodewordStartColumn(TransformableBitMatrix image, final int codewordStartColumn,
                                               final int imageRow) {
    int correctedStartColumn = codewordStartColumn;
    // there should be no black pixels before the start column. If there are, then we need to start earlier.
    while (correctedStartColumn > 0 && image.get(--correctedStartColumn, imageRow)) {
      if ((codewordStartColumn - correctedStartColumn) > CODEWORD_SKEW_SIZE) {
        SimpleLog.log(LEVEL.INFO, "Too many black pixels before start, using previous start position", imageRow,
            codewordStartColumn);
        return codewordStartColumn;
      }
    }
    // The codeword starts with a black pixel. If the current pixel is white, we need to start later.
    final int imageWidth = image.getWidth();
    while (correctedStartColumn < (imageWidth - 1) && !image.get(++correctedStartColumn, imageRow)) {
      if ((correctedStartColumn - codewordStartColumn) > CODEWORD_SKEW_SIZE) {
        SimpleLog.log(LEVEL.INFO, "Too many white pixels before start, using previous start position", imageRow,
            codewordStartColumn);
        return codewordStartColumn;
      }
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

  public static DecoderResult decodeCodewords(int[] codewords, int ecLevel, int[] erasures)
      throws FormatException, ChecksumException {
    if (codewords.length == 0) {
      throw FormatException.getFormatInstance();
    }
    SimpleLog.log(LEVEL.DEBUG, "Codewords: " + codewords.length + ", Erasures: " + erasures.length + ", ecLevel: " +
        ecLevel);

    int numECCodewords = 1 << (ecLevel + 1);

    Decoder.correctErrors(codewords, erasures, numECCodewords);
    Decoder.verifyCodewordCount(codewords, numECCodewords);

    // Decode the codewords
    return DecodedBitStreamParser.decode(codewords, String.valueOf(ecLevel), erasures.length);
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
