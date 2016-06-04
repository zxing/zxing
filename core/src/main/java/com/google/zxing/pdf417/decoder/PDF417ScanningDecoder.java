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

package com.google.zxing.pdf417.decoder;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.detector.MathUtils;
import com.google.zxing.pdf417.PDF417Common;
import com.google.zxing.pdf417.decoder.ec.ErrorCorrection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;

/**
 * @author Guenther Grau
 */
public final class PDF417ScanningDecoder {

  private static final int CODEWORD_SKEW_SIZE = 2;

  private static final int MAX_ERRORS = 3;
  private static final int MAX_EC_CODEWORDS = 512;
  private static final ErrorCorrection errorCorrection = new ErrorCorrection();

  private PDF417ScanningDecoder() {
  }

  // TODO don't pass in minCodewordWidth and maxCodewordWidth, pass in barcode columns for start and stop pattern
  // columns. That way width can be deducted from the pattern column.
  // This approach also allows to detect more details about the barcode, e.g. if a bar type (white or black) is wider 
  // than it should be. This can happen if the scanner used a bad blackpoint.
  public static DecoderResult decode(BitMatrix image,
                                     ResultPoint imageTopLeft,
                                     ResultPoint imageBottomLeft,
                                     ResultPoint imageTopRight,
                                     ResultPoint imageBottomRight,
                                     int minCodewordWidth,
                                     int maxCodewordWidth) throws NotFoundException, FormatException, ChecksumException {
    BoundingBox boundingBox = new BoundingBox(image, imageTopLeft, imageBottomLeft, imageTopRight, imageBottomRight);
    DetectionResultRowIndicatorColumn leftRowIndicatorColumn = null;
    DetectionResultRowIndicatorColumn rightRowIndicatorColumn = null;
    DetectionResult detectionResult = null;
    for (int i = 0; i < 2; i++) {
      if (imageTopLeft != null) {
        leftRowIndicatorColumn = getRowIndicatorColumn(image, boundingBox, imageTopLeft, true, minCodewordWidth,
            maxCodewordWidth);
      }
      if (imageTopRight != null) {
        rightRowIndicatorColumn = getRowIndicatorColumn(image, boundingBox, imageTopRight, false, minCodewordWidth,
            maxCodewordWidth);
      }
      detectionResult = merge(leftRowIndicatorColumn, rightRowIndicatorColumn);
      if (detectionResult == null) {
        throw NotFoundException.getNotFoundInstance();
      }
      if (i == 0 && detectionResult.getBoundingBox() != null &&
          (detectionResult.getBoundingBox().getMinY() < boundingBox.getMinY() || detectionResult.getBoundingBox()
              .getMaxY() > boundingBox.getMaxY())) {
        boundingBox = detectionResult.getBoundingBox();
      } else {
        detectionResult.setBoundingBox(boundingBox);
        break;
      }
    }
    int maxBarcodeColumn = detectionResult.getBarcodeColumnCount() + 1;
    detectionResult.setDetectionResultColumn(0, leftRowIndicatorColumn);
    detectionResult.setDetectionResultColumn(maxBarcodeColumn, rightRowIndicatorColumn);

    boolean leftToRight = leftRowIndicatorColumn != null;
    for (int barcodeColumnCount = 1; barcodeColumnCount <= maxBarcodeColumn; barcodeColumnCount++) {
      int barcodeColumn = leftToRight ? barcodeColumnCount : maxBarcodeColumn - barcodeColumnCount;
      if (detectionResult.getDetectionResultColumn(barcodeColumn) != null) {
        // This will be the case for the opposite row indicator column, which doesn't need to be decoded again.
        continue;
      }
      DetectionResultColumn detectionResultColumn;
      if (barcodeColumn == 0 || barcodeColumn == maxBarcodeColumn) {
        detectionResultColumn = new DetectionResultRowIndicatorColumn(boundingBox, barcodeColumn == 0);
      } else {
        detectionResultColumn = new DetectionResultColumn(boundingBox);
      }
      detectionResult.setDetectionResultColumn(barcodeColumn, detectionResultColumn);
      int startColumn = -1;
      int previousStartColumn = startColumn;
      // TODO start at a row for which we know the start position, then detect upwards and downwards from there.
      for (int imageRow = boundingBox.getMinY(); imageRow <= boundingBox.getMaxY(); imageRow++) {
        startColumn = getStartColumn(detectionResult, barcodeColumn, imageRow, leftToRight);
        if (startColumn < 0 || startColumn > boundingBox.getMaxX()) {
          if (previousStartColumn == -1) {
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

  private static DetectionResult merge(DetectionResultRowIndicatorColumn leftRowIndicatorColumn,
                                       DetectionResultRowIndicatorColumn rightRowIndicatorColumn)
      throws NotFoundException, FormatException {
    if (leftRowIndicatorColumn == null && rightRowIndicatorColumn == null) {
      return null;
    }
    BarcodeMetadata barcodeMetadata = getBarcodeMetadata(leftRowIndicatorColumn, rightRowIndicatorColumn);
    if (barcodeMetadata == null) {
      return null;
    }
    BoundingBox boundingBox = BoundingBox.merge(adjustBoundingBox(leftRowIndicatorColumn),
        adjustBoundingBox(rightRowIndicatorColumn));
    return new DetectionResult(barcodeMetadata, boundingBox);
  }

  private static BoundingBox adjustBoundingBox(DetectionResultRowIndicatorColumn rowIndicatorColumn)
      throws NotFoundException, FormatException {
    if (rowIndicatorColumn == null) {
      return null;
    }
    int[] rowHeights = rowIndicatorColumn.getRowHeights();
    if (rowHeights == null) {
      return null;
    }
    int maxRowHeight = getMax(rowHeights);
    int missingStartRows = 0;
    for (int rowHeight : rowHeights) {
      missingStartRows += maxRowHeight - rowHeight;
      if (rowHeight > 0) {
        break;
      }
    }
    Codeword[] codewords = rowIndicatorColumn.getCodewords();
    for (int row = 0; missingStartRows > 0 && codewords[row] == null; row++) {
      missingStartRows--;
    }
    int missingEndRows = 0;
    for (int row = rowHeights.length - 1; row >= 0; row--) {
      missingEndRows += maxRowHeight - rowHeights[row];
      if (rowHeights[row] > 0) {
        break;
      }
    }
    for (int row = codewords.length - 1; missingEndRows > 0 && codewords[row] == null; row--) {
      missingEndRows--;
    }
    return rowIndicatorColumn.getBoundingBox().addMissingRows(missingStartRows, missingEndRows,
        rowIndicatorColumn.isLeft());
  }

  private static int getMax(int[] values) {
    int maxValue = -1;
    for (int value : values) {
      maxValue = Math.max(maxValue, value);
    }
    return maxValue;
  }

  private static BarcodeMetadata getBarcodeMetadata(DetectionResultRowIndicatorColumn leftRowIndicatorColumn,
                                                    DetectionResultRowIndicatorColumn rightRowIndicatorColumn) {
    BarcodeMetadata leftBarcodeMetadata;
    if (leftRowIndicatorColumn == null ||
        (leftBarcodeMetadata = leftRowIndicatorColumn.getBarcodeMetadata()) == null) {
      return rightRowIndicatorColumn == null ? null : rightRowIndicatorColumn.getBarcodeMetadata();
    }
    BarcodeMetadata rightBarcodeMetadata;
    if (rightRowIndicatorColumn == null ||
        (rightBarcodeMetadata = rightRowIndicatorColumn.getBarcodeMetadata()) == null) {
      return leftBarcodeMetadata;
    }

    if (leftBarcodeMetadata.getColumnCount() != rightBarcodeMetadata.getColumnCount() &&
        leftBarcodeMetadata.getErrorCorrectionLevel() != rightBarcodeMetadata.getErrorCorrectionLevel() &&
        leftBarcodeMetadata.getRowCount() != rightBarcodeMetadata.getRowCount()) {
      return null;
    }
    return leftBarcodeMetadata;
  }

  private static DetectionResultRowIndicatorColumn getRowIndicatorColumn(BitMatrix image,
                                                                         BoundingBox boundingBox,
                                                                         ResultPoint startPoint,
                                                                         boolean leftToRight,
                                                                         int minCodewordWidth,
                                                                         int maxCodewordWidth) {
    DetectionResultRowIndicatorColumn rowIndicatorColumn = new DetectionResultRowIndicatorColumn(boundingBox,
        leftToRight);
    for (int i = 0; i < 2; i++) {
      int increment = i == 0 ? 1 : -1;
      int startColumn = (int) startPoint.getX();
      for (int imageRow = (int) startPoint.getY(); imageRow <= boundingBox.getMaxY() &&
          imageRow >= boundingBox.getMinY(); imageRow += increment) {
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
    }
    return rowIndicatorColumn;
  }

  private static void adjustCodewordCount(DetectionResult detectionResult, BarcodeValue[][] barcodeMatrix)
      throws NotFoundException {
    int[] numberOfCodewords = barcodeMatrix[0][1].getValue();
    int calculatedNumberOfCodewords = detectionResult.getBarcodeColumnCount() *
        detectionResult.getBarcodeRowCount() -
        getNumberOfECCodeWords(detectionResult.getBarcodeECLevel());
    if (numberOfCodewords.length == 0) {
      if (calculatedNumberOfCodewords < 1 || calculatedNumberOfCodewords > PDF417Common.MAX_CODEWORDS_IN_BARCODE) {
        throw NotFoundException.getNotFoundInstance();
      }
      barcodeMatrix[0][1].setValue(calculatedNumberOfCodewords);
    } else if (numberOfCodewords[0] != calculatedNumberOfCodewords) {
      // The calculated one is more reliable as it is derived from the row indicator columns
      barcodeMatrix[0][1].setValue(calculatedNumberOfCodewords);
    }
  }

  private static DecoderResult createDecoderResult(DetectionResult detectionResult) throws FormatException,
      ChecksumException, NotFoundException {
    BarcodeValue[][] barcodeMatrix = createBarcodeMatrix(detectionResult);
    adjustCodewordCount(detectionResult, barcodeMatrix);
    Collection<Integer> erasures = new ArrayList<>();
    int[] codewords = new int[detectionResult.getBarcodeRowCount() * detectionResult.getBarcodeColumnCount()];
    List<int[]> ambiguousIndexValuesList = new ArrayList<>();
    List<Integer> ambiguousIndexesList = new ArrayList<>();
    for (int row = 0; row < detectionResult.getBarcodeRowCount(); row++) {
      for (int column = 0; column < detectionResult.getBarcodeColumnCount(); column++) {
        int[] values = barcodeMatrix[row][column + 1].getValue();
        int codewordIndex = row * detectionResult.getBarcodeColumnCount() + column;
        if (values.length == 0) {
          erasures.add(codewordIndex);
        } else if (values.length == 1) {
          codewords[codewordIndex] = values[0];
        } else {
          ambiguousIndexesList.add(codewordIndex);
          ambiguousIndexValuesList.add(values);
        }
      }
    }
    int[][] ambiguousIndexValues = new int[ambiguousIndexValuesList.size()][];
    for (int i = 0; i < ambiguousIndexValues.length; i++) {
      ambiguousIndexValues[i] = ambiguousIndexValuesList.get(i);
    }
    return createDecoderResultFromAmbiguousValues(detectionResult.getBarcodeECLevel(), codewords,
        PDF417Common.toIntArray(erasures), PDF417Common.toIntArray(ambiguousIndexesList), ambiguousIndexValues);
  }

  /**
   * This method deals with the fact, that the decoding process doesn't always yield a single most likely value. The
   * current error correction implementation doesn't deal with erasures very well, so it's better to provide a value
   * for these ambiguous codewords instead of treating it as an erasure. The problem is that we don't know which of
   * the ambiguous values to choose. We try decode using the first value, and if that fails, we use another of the
   * ambiguous values and try to decode again. This usually only happens on very hard to read and decode barcodes,
   * so decoding the normal barcodes is not affected by this. 
   *
   * @param erasureArray contains the indexes of erasures
   * @param ambiguousIndexes array with the indexes that have more than one most likely value
   * @param ambiguousIndexValues two dimensional array that contains the ambiguous values. The first dimension must
   * be the same length as the ambiguousIndexes array
   */
  private static DecoderResult createDecoderResultFromAmbiguousValues(int ecLevel,
                                                                      int[] codewords,
                                                                      int[] erasureArray,
                                                                      int[] ambiguousIndexes,
                                                                      int[][] ambiguousIndexValues)
      throws FormatException, ChecksumException {
    int[] ambiguousIndexCount = new int[ambiguousIndexes.length];

    int tries = 100;
    while (tries-- > 0) {
      for (int i = 0; i < ambiguousIndexCount.length; i++) {
        codewords[ambiguousIndexes[i]] = ambiguousIndexValues[i][ambiguousIndexCount[i]];
      }
      try {
        return decodeCodewords(codewords, ecLevel, erasureArray);
      } catch (ChecksumException ignored) {
        //
      }
      if (ambiguousIndexCount.length == 0) {
        throw ChecksumException.getChecksumInstance();
      }
      for (int i = 0; i < ambiguousIndexCount.length; i++) {
        if (ambiguousIndexCount[i] < ambiguousIndexValues[i].length - 1) {
          ambiguousIndexCount[i]++;
          break;
        } else {
          ambiguousIndexCount[i] = 0;
          if (i == ambiguousIndexCount.length - 1) {
            throw ChecksumException.getChecksumInstance();
          }
        }
      }
    }
    throw ChecksumException.getChecksumInstance();
  }

  private static BarcodeValue[][] createBarcodeMatrix(DetectionResult detectionResult) {
    BarcodeValue[][] barcodeMatrix =
        new BarcodeValue[detectionResult.getBarcodeRowCount()][detectionResult.getBarcodeColumnCount() + 2];
    for (int row = 0; row < barcodeMatrix.length; row++) {
      for (int column = 0; column < barcodeMatrix[row].length; column++) {
        barcodeMatrix[row][column] = new BarcodeValue();
      }
    }

    int column = 0;
    for (DetectionResultColumn detectionResultColumn : detectionResult.getDetectionResultColumns()) {
      if (detectionResultColumn != null) {
        for (Codeword codeword : detectionResultColumn.getCodewords()) {
          if (codeword != null) {
            int rowNumber = codeword.getRowNumber();
            if (rowNumber >= 0) {
              if (rowNumber >= barcodeMatrix.length) {
                // We have more rows than the barcode metadata allows for, ignore them.
                continue;
              }
              barcodeMatrix[rowNumber][column].setValue(codeword.getValue());
            }
          }
        }
      }
      column++;
    }
    return barcodeMatrix;
  }

  private static boolean isValidBarcodeColumn(DetectionResult detectionResult, int barcodeColumn) {
    return barcodeColumn >= 0 && barcodeColumn <= detectionResult.getBarcodeColumnCount() + 1;
  }

  private static int getStartColumn(DetectionResult detectionResult,
                                    int barcodeColumn,
                                    int imageRow,
                                    boolean leftToRight) {
    int offset = leftToRight ? 1 : -1;
    Codeword codeword = null;
    if (isValidBarcodeColumn(detectionResult, barcodeColumn - offset)) {
      codeword = detectionResult.getDetectionResultColumn(barcodeColumn - offset).getCodeword(imageRow);
    }
    if (codeword != null) {
      return leftToRight ? codeword.getEndX() : codeword.getStartX();
    }
    codeword = detectionResult.getDetectionResultColumn(barcodeColumn).getCodewordNearby(imageRow);
    if (codeword != null) {
      return leftToRight ? codeword.getStartX() : codeword.getEndX();
    }
    if (isValidBarcodeColumn(detectionResult, barcodeColumn - offset)) {
      codeword = detectionResult.getDetectionResultColumn(barcodeColumn - offset).getCodewordNearby(imageRow);
    }
    if (codeword != null) {
      return leftToRight ? codeword.getEndX() : codeword.getStartX();
    }
    int skippedColumns = 0;

    while (isValidBarcodeColumn(detectionResult, barcodeColumn - offset)) {
      barcodeColumn -= offset;
      for (Codeword previousRowCodeword : detectionResult.getDetectionResultColumn(barcodeColumn).getCodewords()) {
        if (previousRowCodeword != null) {
          return (leftToRight ? previousRowCodeword.getEndX() : previousRowCodeword.getStartX()) +
              offset *
              skippedColumns *
              (previousRowCodeword.getEndX() - previousRowCodeword.getStartX());
        }
      }
      skippedColumns++;
    }
    return leftToRight ? detectionResult.getBoundingBox().getMinX() : detectionResult.getBoundingBox().getMaxX();
  }

  private static Codeword detectCodeword(BitMatrix image,
                                         int minColumn,
                                         int maxColumn,
                                         boolean leftToRight,
                                         int startColumn,
                                         int imageRow,
                                         int minCodewordWidth,
                                         int maxCodewordWidth) {
    startColumn = adjustCodewordStartColumn(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
    // we usually know fairly exact now how long a codeword is. We should provide minimum and maximum expected length
    // and try to adjust the read pixels, e.g. remove single pixel errors or try to cut off exceeding pixels.
    // min and maxCodewordWidth should not be used as they are calculated for the whole barcode an can be inaccurate
    // for the current position
    int[] moduleBitCount = getModuleBitCount(image, minColumn, maxColumn, leftToRight, startColumn, imageRow);
    if (moduleBitCount == null) {
      return null;
    }
    int endColumn;
    int codewordBitCount = MathUtils.sum(moduleBitCount);
    if (leftToRight) {
      endColumn = startColumn + codewordBitCount;
    } else {
      for (int i = 0; i < moduleBitCount.length / 2; i++) {
        int tmpCount = moduleBitCount[i];
        moduleBitCount[i] = moduleBitCount[moduleBitCount.length - 1 - i];
        moduleBitCount[moduleBitCount.length - 1 - i] = tmpCount;
      }
      endColumn = startColumn;
      startColumn = endColumn - codewordBitCount;
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
      return null;
    }

    int decodedValue = PDF417CodewordDecoder.getDecodedValue(moduleBitCount);
    int codeword = PDF417Common.getCodeword(decodedValue);
    if (codeword == -1) {
      return null;
    }
    return new Codeword(startColumn, endColumn, getCodewordBucketNumber(decodedValue), codeword);
  }

  private static int[] getModuleBitCount(BitMatrix image,
                                         int minColumn,
                                         int maxColumn,
                                         boolean leftToRight,
                                         int startColumn,
                                         int imageRow) {
    int imageColumn = startColumn;
    int[] moduleBitCount = new int[8];
    int moduleNumber = 0;
    int increment = leftToRight ? 1 : -1;
    boolean previousPixelValue = leftToRight;
    while ((leftToRight ? imageColumn < maxColumn : imageColumn >= minColumn) &&
           moduleNumber < moduleBitCount.length) {
      if (image.get(imageColumn, imageRow) == previousPixelValue) {
        moduleBitCount[moduleNumber]++;
        imageColumn += increment;
      } else {
        moduleNumber++;
        previousPixelValue = !previousPixelValue;
      }
    }
    if (moduleNumber == moduleBitCount.length ||
        ((imageColumn == (leftToRight ? maxColumn : minColumn)) &&
         moduleNumber == moduleBitCount.length - 1)) {
      return moduleBitCount;
    }
    return null;
  }

  private static int getNumberOfECCodeWords(int barcodeECLevel) {
    return 2 << barcodeECLevel;
  }

  private static int adjustCodewordStartColumn(BitMatrix image,
                                               int minColumn,
                                               int maxColumn,
                                               boolean leftToRight,
                                               int codewordStartColumn,
                                               int imageRow) {
    int correctedStartColumn = codewordStartColumn;
    int increment = leftToRight ? -1 : 1;
    // there should be no black pixels before the start column. If there are, then we need to start earlier.
    for (int i = 0; i < 2; i++) {
      while ((leftToRight ? correctedStartColumn >= minColumn : correctedStartColumn < maxColumn) &&
             leftToRight == image.get(correctedStartColumn, imageRow)) {
        if (Math.abs(codewordStartColumn - correctedStartColumn) > CODEWORD_SKEW_SIZE) {
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

  private static DecoderResult decodeCodewords(int[] codewords, int ecLevel, int[] erasures) throws FormatException,
      ChecksumException {
    if (codewords.length == 0) {
      throw FormatException.getFormatInstance();
    }

    int numECCodewords = 1 << (ecLevel + 1);
    int correctedErrorsCount = correctErrors(codewords, erasures, numECCodewords);
    verifyCodewordCount(codewords, numECCodewords);

    // Decode the codewords
    DecoderResult decoderResult = DecodedBitStreamParser.decode(codewords, String.valueOf(ecLevel));
    decoderResult.setErrorsCorrected(correctedErrorsCount);
    decoderResult.setErasures(erasures.length);
    return decoderResult;
  }

  /**
   * <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
   * correct the errors in-place.</p>
   *
   * @param codewords   data and error correction codewords
   * @param erasures positions of any known erasures
   * @param numECCodewords number of error correction codewords that are available in codewords
   * @throws ChecksumException if error correction fails
   */
  private static int correctErrors(int[] codewords, int[] erasures, int numECCodewords) throws ChecksumException {
    if (erasures != null &&
        erasures.length > numECCodewords / 2 + MAX_ERRORS ||
        numECCodewords < 0 ||
        numECCodewords > MAX_EC_CODEWORDS) {
      // Too many errors or EC Codewords is corrupted
      throw ChecksumException.getChecksumInstance();
    }
    return errorCorrection.decode(codewords, numECCodewords, erasures);
  }

  /**
   * Verify that all is OK with the codeword array.
   */
  private static void verifyCodewordCount(int[] codewords, int numECCodewords) throws FormatException {
    if (codewords.length < 4) {
      // Codeword array size should be at least 4 allowing for
      // Count CW, At least one Data CW, Error Correction CW, Error Correction CW
      throw FormatException.getFormatInstance();
    }
    // The first codeword, the Symbol Length Descriptor, shall always encode the total number of data
    // codewords in the symbol, including the Symbol Length Descriptor itself, data codewords and pad
    // codewords, but excluding the number of error correction codewords.
    int numberOfCodewords = codewords[0];
    if (numberOfCodewords > codewords.length) {
      throw FormatException.getFormatInstance();
    }
    if (numberOfCodewords == 0) {
      // Reset to the length of the array - 8 (Allow for at least level 3 Error Correction (8 Error Codewords)
      if (numECCodewords < codewords.length) {
        codewords[0] = codewords.length - numECCodewords;
      } else {
        throw FormatException.getFormatInstance();
      }
    }
  }

  private static int[] getBitCountForCodeword(int codeword) {
    int[] result = new int[8];
    int previousValue = 0;
    int i = result.length - 1;
    while (true) {
      if ((codeword & 0x1) != previousValue) {
        previousValue = codeword & 0x1;
        i--;
        if (i < 0) {
          break;
        }
      }
      result[i]++;
      codeword >>= 1;
    }
    return result;
  }

  private static int getCodewordBucketNumber(int codeword) {
    return getCodewordBucketNumber(getBitCountForCodeword(codeword));
  }

  private static int getCodewordBucketNumber(int[] moduleBitCount) {
    return (moduleBitCount[0] - moduleBitCount[2] + moduleBitCount[4] - moduleBitCount[6] + 9) % 9;
  }

  public static String toString(BarcodeValue[][] barcodeMatrix) {
    Formatter formatter = new Formatter();
    for (int row = 0; row < barcodeMatrix.length; row++) {
      formatter.format("Row %2d: ", row);
      for (int column = 0; column < barcodeMatrix[row].length; column++) {
        BarcodeValue barcodeValue = barcodeMatrix[row][column];
        if (barcodeValue.getValue().length == 0) {
          formatter.format("        ", (Object[]) null);
        } else {
          formatter.format("%4d(%2d)", barcodeValue.getValue()[0],
              barcodeValue.getConfidence(barcodeValue.getValue()[0]));
        }
      }
      formatter.format("%n");
    }
    String result = formatter.toString();
    formatter.close();
    return result;
  }

}
