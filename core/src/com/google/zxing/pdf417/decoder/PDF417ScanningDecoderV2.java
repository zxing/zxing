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
import com.google.zxing.common.PerspectiveTransform;
import com.google.zxing.common.TransformableBitMatrix;
import com.google.zxing.pdf417.PDF417Common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Guenther Grau
 */
public final class PDF417ScanningDecoderV2 {

  private static final int STOP_PATTERN_VALUE = 130324;
  private static final int CODEWORD_SKEW_SIZE = 2;
  private static final int BARCODE_ROW_INDICATOR_COLUMN = -2;
  private static final int BARCODE_ROW_UNKNOWN = -1;

  public static DecoderResult decode(TransformableBitMatrix image, final ResultPoint imageTopLeft,
                                     final ResultPoint imageBottomLeft, final ResultPoint imageTopRight,
                                     final ResultPoint imageBottomRight, int minCodewordWidth,
                                     int maxCodewordWidth) throws NotFoundException, FormatException,
      ChecksumException {
    // TODO update minCodewordWidth and maxCodewordWidth when valid codewords are found. maybe add them as properties to detection result 
    BoundingBox boundingBox = new BoundingBox(imageTopLeft, imageBottomLeft, imageTopRight, imageBottomRight,
        maxCodewordWidth);
    DetectionResultColumn detectionResultColumn = new DetectionResultColumn(boundingBox);

    int barcodeColumn = 0;
    int startColumn = (int) imageTopLeft.getX();
    for (int imageRow = (int) imageTopLeft.getY(); imageRow <= imageBottomLeft.getY(); imageRow++) {
      Codeword codeword = detectCodeword(image, startColumn, imageRow, minCodewordWidth, maxCodewordWidth,
          BARCODE_ROW_INDICATOR_COLUMN);
      if (codeword != null) {
        detectionResultColumn.setCodeword(imageRow, codeword);
        startColumn = codeword.getStartX();
      }
    }
    DetectionResult detectionResult = getDetectionResult(detectionResultColumn);
    if (detectionResult == null) {
      // TODO if detectionResult is null, try the right row indicator column
      throw NotFoundException.getNotFoundInstance();
    }
    detectionResult.setDetectionResultColumn(barcodeColumn, detectionResultColumn);
    // TODO we could go over the the result column and try to adjust the barcode row numbers
    // the row numbers should increase monotonous and never be smaller than a previous one
    int maxBarcodeColumn = detectionResult.getBarcodeColumnCount() + 2;
    for (barcodeColumn = 1; barcodeColumn < maxBarcodeColumn; barcodeColumn++) {
      detectionResultColumn = new DetectionResultColumn(boundingBox);
      detectionResult.setDetectionResultColumn(barcodeColumn, detectionResultColumn);
      // TODO maybe remember earliest start position and start one row earlier?
      // might make it more efficient
      startColumn = -1;
      for (int imageRow = boundingBox.getMinY(); imageRow <= boundingBox.getMaxY(); imageRow++) {
        int previousStartColumn = startColumn;
        startColumn = getStartColumn(detectionResult, barcodeColumn, imageRow);
        if (startColumn == -1) {
          // TODO could use more previous columns and calculate start column estimate
          if (previousStartColumn == -1) {
            continue;
          }
          startColumn = previousStartColumn;
        }
        int barcodeRow = BARCODE_ROW_UNKNOWN;
        boolean isRightRowIndicatorColumn = barcodeColumn == maxBarcodeColumn - 1;
        if (isRightRowIndicatorColumn) {
          barcodeRow = BARCODE_ROW_INDICATOR_COLUMN;
        }
        Codeword codeword = detectCodeword(image, startColumn, imageRow, minCodewordWidth, maxCodewordWidth,
            barcodeRow);
        if (codeword != null) {
          detectionResultColumn.setCodeword(imageRow, codeword);
        }
      }
    }
    return createDecoderResult(detectionResult);
  }

  private static DecoderResult createDecoderResult(DetectionResult detectionResult) throws NotFoundException,
      FormatException, ChecksumException {
    BarcodeMatrix barcodeMatrix = createBarcodeMatrix(detectionResult);
    barcodeMatrix.dump();
    Integer value = barcodeMatrix.getValue(0, 1);
    int calculatedNumberOfCodewords = detectionResult.getBarcodeColumnCount() *
        detectionResult.getBarcodeRowCount() - getNumberOfECCodeWords(detectionResult.getBarcodeECLevel());
    if (value == null) {
      log(2, "Error, number of codewords not found", -1, -1);
      if (calculatedNumberOfCodewords < 1) {
        log(2, "Error, calculatedNumberOfCodewords to small " + calculatedNumberOfCodewords, -1, -1);
        throw NotFoundException.getNotFoundInstance();
      }
      barcodeMatrix.setValue(0, 1, calculatedNumberOfCodewords);
    } else if (value != calculatedNumberOfCodewords) {
      log(2, "Error, number of codewords " + value + " doesn't match calculated " +
          calculatedNumberOfCodewords, -1, -1);
      // TODO try decoding with calculated number of codewords
    }
    if (detectionResult.getBarcodeRowCount() <= 0 || detectionResult.getBarcodeColumnCount() <= 0) {
      log(1, "negativ barcode counts", -1, -1, detectionResult.getBarcodeRowCount(),
          detectionResult.getBarcodeColumnCount());
      throw NotFoundException.getNotFoundInstance();
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
    log(3, "WARNING: Should calculate start column more accurately, Please fix code :-)", imageRow,
        barcodeColumn);
    for (Codeword previousRowCodeword : detectionResult.getDetectionResultColumn(barcodeColumn - 1)
        .getCodewords()) {
      if (previousRowCodeword != null) {
        return previousRowCodeword.getEndX();
      }
    }

    log(3, "ERROR: Estimated start column not found. Please fix the code. :-)", imageRow, barcodeColumn);
    return -1;
  }

  // TODO add parameter to make it work for the right row indicator as well
  private static DetectionResult getDetectionResult(DetectionResultColumn detectionResultColumn) {
    Codeword[] codewords = detectionResultColumn.getCodewords();
    BarcodeValue barcodeColumnCount = new BarcodeValue();
    BarcodeValue barcodeRowCount = new BarcodeValue();
    BarcodeValue barcodeRowCountOffset = new BarcodeValue();
    BarcodeValue barcodeECLevel = new BarcodeValue();
    for (Codeword codeword : codewords) {
      if (codeword == null) {
        continue;
      }
      int rowIndicatorValue = getRowIndicatorValue(codeword.getValue());
      // TODO simply add 1 (or 2?) to the codeword row number for the right row indicator  
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
        (barcodeRowCountOffset.getValue() == null) || (barcodeECLevel.getValue() == null)) {
      return null;
    }
    return new DetectionResult(barcodeColumnCount.getValue(), barcodeRowCount.getValue() +
        barcodeRowCountOffset.getValue(), barcodeECLevel.getValue(), detectionResultColumn.getBoundingBox());
  }

  private static Codeword detectCodeword(TransformableBitMatrix image, int startColumn, int imageRow,
                                         int minCodewordWidth, int maxCodewordWidth, int barcodeRow) {
    int previousStartColumn = startColumn;
    startColumn = getBarcodeStartColumn(image, startColumn, imageRow);
    if (startColumn == -1) {
      startColumn = previousStartColumn;
    }
    int[] moduleBitCount = getModuleBitCount(image, startColumn, imageRow);
    if (moduleBitCount == null) {
      return null;
    }
    int codewordBitCount = PDF417Common.getBitCountSum(moduleBitCount);
    int endColumn = startColumn + codewordBitCount;
    // TODO use width of surrounding codewords for more accurate results 
    if (!checkCodewordSkew(codewordBitCount, minCodewordWidth, maxCodewordWidth)) {
      log(1, "Invalid Codeword size " + codewordBitCount + ", skipping", imageRow, endColumn);
      return null;
    }

    int[] bitCountCopy = Arrays.copyOf(moduleBitCount, moduleBitCount.length);
    if (!PDF417CodewordDecoder.adjustBitCount(moduleBitCount)) {
      log(2, "barcode symbol too small", imageRow, endColumn);
      // TODO maybe try to process next module? 
      return null;
    }
    int bucket = getCodewordBucketNumber(moduleBitCount);
    long decodedValue = getDecodedValue(moduleBitCount);
    if (decodedValue == STOP_PATTERN_VALUE) {
      return null;
    }
    int codeword = BitMatrixParser.getCodeword(decodedValue);
    if (codeword == -1) {
      // TODO could try to reinterpret the moduleBitCount using a different strategy
      log(1, "invalid barcode symbol for moduleBitCount " + getBitCounts(moduleBitCount) + ", original: " +
          getBitCounts(bitCountCopy) + "\n", imageRow, endColumn);
      return null;
    }

    return new Codeword(startColumn, endColumn, bucket, codeword, getBarcodeRowNumber(codeword, bucket,
        barcodeRow));
  }

  private static int getBarcodeRowNumber(int codeword, int bucket, int barcodeRow) {
    if (barcodeRow == BARCODE_ROW_INDICATOR_COLUMN) {
      return getRowIndicatorRowNumber(codeword) + bucket / 3;
    }
    if (bucket != ((barcodeRow % 3) * 3)) {
      return BARCODE_ROW_UNKNOWN;
    }
    return barcodeRow;
  }

  private static int[] getModuleBitCount(TransformableBitMatrix image, int startColumn, int imageRow) {
    final int imageWidth = image.getWidth();
    int imageColumn = startColumn;
    int[] moduleBitCount = new int[8];
    int moduleNumber = 0;
    try {
      while (imageColumn < imageWidth && moduleNumber < moduleBitCount.length) {
        while (imageColumn < imageWidth && image.get(imageColumn, imageRow)) {
          moduleBitCount[moduleNumber]++;
          imageColumn++;
        }
        moduleNumber++;
        while (imageColumn < imageWidth && !image.get(imageColumn, imageRow)) {
          moduleBitCount[moduleNumber]++;
          imageColumn++;
        }
        moduleNumber++;
      }
    } catch (Throwable e) {
      System.out.println("READ THIS");
      e.printStackTrace();
    }
    if (moduleNumber != moduleBitCount.length) {
      log(1, "incomplete codeword, stop processing current row", imageRow, imageColumn);
      return null;
    }
    return moduleBitCount;
  }

  /*
      final int codewordWidth = (minCodewordWidth + maxCodewordWidth) / 2;
      int barcodeColumnCount = Math.round((imageTopRight.getX() - imageTopLeft.getX()) / codewordWidth) - 2;

      int imageMaxX = imageWidth;
      if (imageTopRight != null && imageBottomRight != null) {
        imageMaxX = Math.max((int) imageTopRight.getX(), (int) imageBottomRight.getX());
      }
      int[] moduleBitCount = new int[8];
      int barcodeRowCount = -1;
      int barcodeECLevel = -1;
      int barcodeRow = 0;
      int previousBarcodeStartRow = 0;
      int codewordBucket = 0;
      int imageColumn = imageTopLeftX;
      int previousImageColumnStart;
      int imageColumnStart = imageTopLeftX;
      BarcodeMatrix barcodeMatrix = new BarcodeMatrix();
      for (int imageRow = imageTopLeftY; imageRow <= imageBottomLeftY; imageRow++) {
        log(0, "barcodeRow: " + barcodeRow, imageRow, imageColumn);
        previousImageColumnStart = imageColumnStart;
        imageColumnStart = getBarcodeStartColumn(image, imageColumnStart, imageRow, minCodewordWidth,
            maxCodewordWidth);
        if (imageColumnStart == -1) {
          log(2, "imageColumnStart not found: " + barcodeRow, imageRow, imageColumn);
          imageColumnStart = previousImageColumnStart;
        }

        imageColumn = imageColumnStart;
        barcodeRow = previousBarcodeStartRow;
        for (int barcodeColumn = 0; imageColumn < imageMaxX; barcodeColumn++) {
          if ((barcodeColumnCount != -1 && barcodeColumn >= (barcodeColumnCount + 2))) {
            log(1, "Exceeding precalculated barcode column count " + barcodeColumnCount, imageRow, imageColumn);
            break;
          }

          int barcodeDecodingProblems = 0;
          int previousImageColumn = imageColumn;
          imageColumn = getBarcodeStartColumn(image, imageColumn, imageRow, minCodewordWidth, maxCodewordWidth);
          if (imageColumn == -1) {
            imageColumn = previousImageColumn;
            //log(1, "Cannot find proper start position for codeword", imageRow, imageColumn);
            //imageColumn += ((minCodewordWidth + maxCodewordWidth) / 2);
            // continue;
          }
          log(0, "next codeword", imageRow, imageColumn, barcodeRow, barcodeColumn);
          Arrays.fill(moduleBitCount, 0);
          int moduleNumber = 0;
          while (imageColumn < imageWidth && moduleNumber < moduleBitCount.length) {
            while (imageColumn < imageWidth && image.get(imageColumn, imageRow)) {
              moduleBitCount[moduleNumber]++;
              imageColumn++;
            }
            moduleNumber++;
            while (imageColumn < imageWidth && !image.get(imageColumn, imageRow)) {
              moduleBitCount[moduleNumber]++;
              imageColumn++;
            }
            moduleNumber++;
          }
          if (moduleNumber != moduleBitCount.length) {
            log(1, "incomplete codeword, stop processing current row", imageRow, imageColumn);
            break;
          }
          int codewordBitCount = PDF417Common.getBitCountSum(moduleBitCount);
          if (!checkCodewordSkew(codewordBitCount, minCodewordWidth, maxCodewordWidth)) {
            log(1, "Invalid Codeword size " + codewordBitCount + ", skipping", imageRow, imageColumn);
            // TODO codewordWidth should be calculated dynamically
            imageColumn = imageColumn - codewordBitCount + codewordWidth;
            continue;
          }
          int[] bitCountCopy = Arrays.copyOf(moduleBitCount, moduleBitCount.length);
          if (!PDF417CodewordDecoder.adjustBitCount(moduleBitCount)) {
            log(2, "barcode symbol too small", imageRow, imageColumn);
            // TODO maybe try to process next module? 
            break;
          }
          long decodedValue = getDecodedValue(moduleBitCount);
          if (decodedValue == STOP_PATTERN_VALUE) {
            break;
          }
          int codeword = BitMatrixParser.getCodeword(decodedValue);
          if (codeword == -1) {
            // TODO could try to reinterpret the moduleBitCount using a different strategy

            log(1, "invalid barcode symbol for moduleBitCount " + getBitCounts(moduleBitCount) +
                ", original: " + getBitCounts(bitCountCopy) + "\n", imageRow, imageColumn, barcodeRow,
                barcodeColumn);
            continue;
          }
          codewordBucket = getCodewordBucketNumber(moduleBitCount);
          if (barcodeColumn == 0) {
            barcodeRow = getRowIndicatorRowNumber(codeword) + codewordBucket / 3;
            int estimatedBarcodeRow = getEstimatedBarcodeRow(perspectiveTransform, maxHeight, barcodeRowCount,
                imageRow, imageColumn);
            if (Math.abs(barcodeRow - estimatedBarcodeRow) > 2) {
              barcodeRow = -1;
              log(1, "skipping codeword, estimatedRow " + estimatedBarcodeRow + " differs from barcodeRow " +
                  barcodeRow, imageRow, imageColumn, barcodeRow, barcodeColumn);
              continue;
            }
            previousBarcodeStartRow = barcodeRow;

            int value = getRowIndicatorValue(codeword);
            switch (barcodeRow % 3) {
              case 0:
                if (barcodeRowCount == -1) {
                  barcodeRowCount = value * 3 + 1;
                }
                break;
              case 1:
                if (barcodeECLevel == -1) {
                  barcodeECLevel = value / 3;
                  barcodeRowCount += value % 3;
                }
                break;
              case 2:
                if (barcodeColumnCount == -1) {
                  barcodeColumnCount = value + 1;
                } else if (barcodeColumnCount != value + 1) {
                  log(1, "adjusting barcode column count from " + barcodeColumnCount + " to " + (value + 1),
                      imageRow, imageColumn);
                  barcodeColumnCount = value + 1;
                }
                break;
            }
          }
          if (barcodeColumn == barcodeColumnCount + 1) {
            barcodeRow = getRowIndicatorRowNumber(codeword) + codewordBucket / 3;
            int estimatedBarcodeRow = getEstimatedBarcodeRow(perspectiveTransform, maxHeight, barcodeRowCount,
                imageRow, imageColumn);
            if (Math.abs(barcodeRow - estimatedBarcodeRow) > 2) {
              if (Math.abs(barcodeRow - estimatedBarcodeRow) > 2) {
                log(1, "skipping codeword, estimatedRow " + estimatedBarcodeRow + " differs from barcodeRow " +
                    barcodeRow, imageRow, imageColumn, barcodeRow, barcodeColumn);
                barcodeRow = -1;
                break;
              }
            }
            previousBarcodeStartRow = barcodeRow;

            int value = getRowIndicatorValue(codeword);
            switch (barcodeRow % 3) {
              case 1:
                if (barcodeRowCount == -1) {
                  barcodeRowCount = value * 3 + 1;
                }
                break;
              case 2:
                if (barcodeECLevel == -1) {
                  barcodeECLevel = value / 3;
                  barcodeRowCount += value % 3;
                }
                break;
              case 0:
                if (barcodeColumnCount == -1) {
                  barcodeColumnCount = value + 1;
                } else if (barcodeColumnCount != value + 1) {
                  log(1, "adjusting barcode column count from " + barcodeColumnCount + " to " + (value + 1),
                      imageRow, imageColumn);
                  barcodeColumnCount = value + 1;
                }
                break;
            }
          }
          if (barcodeRow == -1) {
            log(1, "first codeword was unreadable, reusing the previous barcode start row number", imageRow,
                imageColumn);
            barcodeRow = previousBarcodeStartRow;
            barcodeDecodingProblems++;
          }
          if (barcodeColumn > 0 && codewordBucket != ((barcodeRow % 3) * 3)) {
            log(1, "we have skipped to a different barcode row", imageRow, imageColumn);
            int oldBarcodeRow = barcodeRow;
            barcodeRow = calculateSkippedBarcodeRow(barcodeRow, codewordBucket,
                getEstimatedBarcodeRow(perspectiveTransform, maxHeight, barcodeRowCount, imageRow, imageColumn));
            if (barcodeRow < 0) {
              break;
            }
            if (Math.abs(barcodeRow - oldBarcodeRow) > 1) {
              barcodeRow = oldBarcodeRow;
              break;
            }
            barcodeDecodingProblems++;
          }
          int estimatedBarcodeRow = getEstimatedBarcodeRow(perspectiveTransform, maxHeight, barcodeRowCount,
              imageRow, imageColumn);
          if (Math.abs(barcodeRow - estimatedBarcodeRow) > 1) {
            barcodeRow = calculateSkippedBarcodeRow(estimatedBarcodeRow, codewordBucket, estimatedBarcodeRow);
            previousBarcodeStartRow = barcodeRow;
            barcodeDecodingProblems++;
          }
          if (barcodeRow < 0 || barcodeColumn < 0) {
            log(2, "negative values, barcodeRow: " + barcodeRow + ", barcodeColumn: " + barcodeColumn,
                imageRow, imageColumn);
            continue;
          }

          barcodeMatrix.setValue(barcodeRow, barcodeColumn, codeword);
          if (barcodeDecodingProblems == 0) {
            minCodewordWidth = Math.min(minCodewordWidth, codewordBitCount);
            maxCodewordWidth = Math.max(maxCodewordWidth, codewordBitCount);
          }
        }
      }

      barcodeMatrix.dump();
      // TODO could check right row indicator as well
      Integer value = barcodeMatrix.getValue(0, 1);
      int calculatedNumberOfCodewords = barcodeColumnCount * barcodeRowCount -
          getNumberOfECCodeWords(barcodeECLevel);
      if (value == null) {
        log(2, "Error, number of codewords not found", -1, imageColumn);
        if (calculatedNumberOfCodewords < 1) {
          log(2, "Error, calculatedNumberOfCodewords to small " + calculatedNumberOfCodewords, -1, imageColumn);
          throw NotFoundException.getNotFoundInstance();
        }
        barcodeMatrix.setValue(0, 1, calculatedNumberOfCodewords);
      } else if (value != calculatedNumberOfCodewords) {
        log(2, "Error, number of codewords " + value + " doesn't match calculated " +
            calculatedNumberOfCodewords, -1, imageColumn);
        // TODO try decoding with calculated number of codewords
      }
      if (barcodeRowCount <= 0 || barcodeColumnCount <= 0) {
        log(1, "negativ barcode counts", -1, imageColumn, barcodeRowCount, barcodeColumnCount);
        throw NotFoundException.getNotFoundInstance();
      }
      List<Integer> erasures = new ArrayList<Integer>();
      int[] codewords = new int[barcodeRowCount * barcodeColumnCount];
      for (int row = 0; row < barcodeRowCount; row++) {
        for (int column = 0; column < barcodeColumnCount; column++) {
          Integer codeword = barcodeMatrix.getValue(row, column + 1);
          if (codeword == null) {
            erasures.add(row * barcodeColumnCount + column);
          } else {
            codewords[row * barcodeColumnCount + column] = codeword;
          }
        }
      }

      return decodeCodewords(codewords, barcodeECLevel, getErasureArray(erasures));
    }
  */
  private static String getBitCounts(int[] moduleBitCount) {
    StringBuilder result = new StringBuilder("{");
    for (int bitCount : moduleBitCount) {
      result.append(bitCount).append(',');
    }
    result.setLength(result.length() - 1);
    return result.append('}').toString();
  }

  private static int getEstimatedBarcodeRow(PerspectiveTransform perspectiveTransform, int maxHeight,
                                            int barcodeRowCount, int imageRow, int imageColumn) {
    float[] points = new float[] { imageColumn, imageRow };
    perspectiveTransform.transformPoints(points);
    return (int) (points[0] * barcodeRowCount / maxHeight);
  }

  private static int getNumberOfECCodeWords(int barcodeECLevel) {
    return 2 << barcodeECLevel;
  }

  private static int calculateSkippedBarcodeRow(int barcodeRow, int newCodewordBucket, int estimatedBarcodeRow) {

    if ((((barcodeRow + 1) % 3) * 3) == newCodewordBucket) {
      return ++barcodeRow;
    }
    return --barcodeRow;
  }

  private static int getBarcodeStartColumn(TransformableBitMatrix image, final int codewordStartColumn,
                                           final int imageRow) {
    int correctedImageStartColumn = codewordStartColumn;
    // there should be no black pixels before the start column. If there are, then we need to start earlier.
    while (correctedImageStartColumn > 0 && image.get(--correctedImageStartColumn, imageRow)) {
      if ((codewordStartColumn - correctedImageStartColumn) > CODEWORD_SKEW_SIZE) {
        log(2, "Too many black pixels before start, using previous start position", imageRow,
            codewordStartColumn);
        return -1;
      }
    }

    final int imageWidth = image.getWidth();
    while (correctedImageStartColumn < (imageWidth - 1) && !image.get(++correctedImageStartColumn, imageRow)) {
      if ((codewordStartColumn - correctedImageStartColumn) > CODEWORD_SKEW_SIZE) {
        log(2, "Too many white pixels before start, using previous start position", imageRow,
            codewordStartColumn);
        return -1;
      }
    }

    return correctedImageStartColumn;
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

  private static int getRowIndicatorValue(int codeword) {
    return (codeword % 30);
  }

  private static int getRowIndicatorRowNumber(int codeword) {
    return (codeword / 30) * 3;
  }

  public static DecoderResult decodeCodewords(int[] codewords, int ecLevel, int[] erasures)
      throws FormatException, ChecksumException {
    if (codewords.length == 0) {
      throw FormatException.getFormatInstance();
    }
    log(2, "Codewords: " + codewords.length + ", Erasures: " + erasures.length + ", ecLevel: " + ecLevel, -1,
        -1);

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

  private static class BarcodeMatrix {
    Map<String,BarcodeValue> values = new HashMap<String,BarcodeValue>();
    int maxRow = -1;
    int maxColumn = -1;

    private String getKey(int barcodeRow, int barcodeColumn) {
      return barcodeRow + "," + barcodeColumn;
    }

    public void setValue(int row, int column, int value) {
      maxRow = Math.max(maxRow, row);
      maxColumn = Math.max(maxColumn, column);
      String key = getKey(row, column);
      BarcodeValue barcodeValue = values.get(key);
      if (barcodeValue == null) {
        barcodeValue = new BarcodeValue();
        values.put(key, barcodeValue);
      }
      log(0, "setting value " + value, -1, -1, row, column);
      barcodeValue.setValue(value);
    }

    public Integer getValue(int row, int column) {
      BarcodeValue barcodeValue = values.get(getKey(row, column));
      if (barcodeValue == null) {
        return null;
      }
      return barcodeValue.getValue();
    }

    public void dump() {
      Formatter formatter = new Formatter();
      for (int row = 0; row <= maxRow; row++) {
        formatter.format("Row %2d: ", row);
        for (int column = 0; column <= maxColumn; column++) {
          BarcodeValue barcodeValue = values.get(getKey(row, column));
          if (barcodeValue == null) {
            formatter.format("        ", (Object[]) null);
          } else {
            formatter.format("%4d(%2d)", barcodeValue.getValue(),
                barcodeValue.getConfidence(barcodeValue.getValue()));
          }
        }
        formatter.format("\n", (Object[]) null);
      }
      System.err.println(formatter);
      formatter.close();
    }
  }

  private static class BarcodeValue {
    Map<Integer,Integer> values = new HashMap<Integer,Integer>();

    public void setValue(int value) {
      Integer confidence = values.get(value);
      if (confidence == null) {
        confidence = 0;
      }
      confidence = confidence + 1;
      values.put(value, confidence);
      for (Entry<Integer,Integer> entry : values.entrySet()) {
        log(0, "value: " + entry.getKey() + ", confidence: " + entry.getValue(), -1, -1);
      }
    }

    public Integer getValue() {
      int maxConfidence = -1;
      Integer result = null;
      for (Entry<Integer,Integer> entry : values.entrySet()) {
        if (entry.getValue() > maxConfidence) {
          maxConfidence = entry.getValue();
          result = entry.getKey();
        }
      }
      return result;
    }

    public Integer getConfidence(int value) {
      return values.get(value);
    }
  }

  private static class DetectionResult {
    private final int barcodeColumnCount;
    private final int barcodeRowCount;
    private final int barcodeECLevel;
    private final DetectionResultColumn[] detectionResultColumns;
    private final BoundingBox boundingBox;

    public DetectionResult(int barcodeColumnCount,
                           int barcodeRowCount,
                           int barcodeECLevel,
                           BoundingBox boundingBox) {
      this.barcodeColumnCount = barcodeColumnCount;
      this.barcodeRowCount = barcodeRowCount;
      this.barcodeECLevel = barcodeECLevel;
      this.boundingBox = boundingBox;
      detectionResultColumns = new DetectionResultColumn[barcodeColumnCount + 2];

      if (boundingBox.getTopRight() == null) {
        int rightX = boundingBox.getMinX() + (barcodeColumnCount + 1) * boundingBox.getMaxCodewordWidth();
        boundingBox.setTopRight(new ResultPoint(rightX, boundingBox.getMinY()));
        boundingBox.setBottomRight(new ResultPoint(rightX, boundingBox.getMaxY()));
      }
    }

    public int getImageStartRow(int barcodeColumn) {
      DetectionResultColumn detectionResultColumn = null;
      while (barcodeColumn > 0) {
        detectionResultColumn = detectionResultColumns[--barcodeColumn];
        // TODO compare start row with previous result columns
        // Could try detecting codewords from right to left
        // if all else fails, could calculate estimate
        Codeword[] codewords = detectionResultColumn.getCodewords();
        for (int rowNumber = 0; rowNumber < codewords.length; rowNumber++) {
          if (codewords[rowNumber] != null) {
            // next column might start earlier if barcode is not aligned with image
            if (rowNumber > 0) {
              rowNumber--;
            }
            return detectionResultColumn.getImageRow(rowNumber);
          }
        }
      }
      return -1;
    }

    public void setDetectionResultColumn(int barcodeColumn, DetectionResultColumn detectionResultColumn) {
      detectionResultColumns[barcodeColumn] = detectionResultColumn;
    }

    public DetectionResultColumn getDetectionResultColumn(int barcodeColumn) {
      return detectionResultColumns[barcodeColumn];
    }

    public DetectionResultColumn[] getDetectionResultColumns() {
      dumpDetectionResultColumns("Before adjustRowNumbers");
      // TODO implement this properly
      int previousUnadjustedCount = countUnadjusted();
      int unadjustedCount = previousUnadjustedCount;
      do {
        previousUnadjustedCount = unadjustedCount;
        adjustRowNumbers();
        unadjustedCount = countUnadjusted();
      } while (unadjustedCount < previousUnadjustedCount);

      if (unadjustedCount > 0) {
        System.err.println("Please fix the code :-) Still " + unadjustedCount +
            " codewords without valid row number left");
      }

      dumpDetectionResultColumns("After adjustRowNumbers");
      return detectionResultColumns;
    }

    private void dumpDetectionResultColumns(String message) {
      System.err.println(message);
      boolean hasModeValues = true;
      int codewordsRow = -1;
      StringBuilder builder = new StringBuilder();
      while (hasModeValues) {
        codewordsRow++;
        builder.append(String.format("CW %3d:", codewordsRow));
        hasModeValues = false;
        for (int barcodeColumn = 0; barcodeColumn < barcodeColumnCount + 2; barcodeColumn++) {
          if (detectionResultColumns[barcodeColumn] == null ||
              codewordsRow >= detectionResultColumns[barcodeColumn].getCodewords().length) {
            builder.append("    |   ");
            continue;
          }
          hasModeValues = true;
          Codeword codeword = detectionResultColumns[barcodeColumn].getCodewords()[codewordsRow];
          if (codeword == null) {
            builder.append("    |   ");
            continue;
          }
          builder.append(String.format(" %3d|%3d", codeword.getRowNumber(), codeword.getValue()));
        }
        System.err.println(builder);
        builder.setLength(0);
      }
    }

    // TODO ensure that no detected codewords with unknown row number are left
    // we should use all columns when trying to find the row number, especially the row
    // indicator columns.
    // we should be able to estimate the row height and use it as a hint for the row number
    private void adjustRowNumbers() {
      adjustIndicatorColumnRowNumbers(detectionResultColumns[0]);
      adjustIndicatorColumnRowNumbers(detectionResultColumns[barcodeColumnCount + 1]);
      for (int barcodeColumn = 1; barcodeColumn < barcodeColumnCount + 1; barcodeColumn++) {
        Codeword[] codewords = detectionResultColumns[barcodeColumn].getCodewords();
        for (int codewordsRow = 0; codewordsRow < codewords.length; codewordsRow++) {
          if (codewords[codewordsRow] == null) {
            continue;
          }
          if (codewords[codewordsRow].getRowNumber() == BARCODE_ROW_UNKNOWN) {
            adjustRowNumbers(barcodeColumn, codewordsRow, codewords);
          }
        }
      }
    }

    private int countUnadjusted() {
      int unadjustedCount = 0;
      for (int barcodeColumn = 1; barcodeColumn < barcodeColumnCount + 1; barcodeColumn++) {
        Codeword[] codewords = detectionResultColumns[barcodeColumn].getCodewords();
        for (int codewordsRow = 0; codewordsRow < codewords.length; codewordsRow++) {
          if (codewords[codewordsRow] == null) {
            continue;
          }
          if (codewords[codewordsRow].getRowNumber() == BARCODE_ROW_UNKNOWN) {
            unadjustedCount++;
          }
        }
      }
      return unadjustedCount;
    }

    private void adjustRowNumbers(int barcodeColumn, int codewordsRow, Codeword[] codewords) {
      Codeword codeword = codewords[codewordsRow];
      Codeword[] previousColumnCodewords = detectionResultColumns[barcodeColumn - 1].getCodewords();
      Codeword[] nextColumnCodewords = previousColumnCodewords;
      if (detectionResultColumns[barcodeColumn + 1] != null) {
        nextColumnCodewords = detectionResultColumns[barcodeColumn + 1].getCodewords();
      }

      Codeword[] otherCodewords = new Codeword[14];

      if (codewordsRow > 0) {
        otherCodewords[0] = codewords[codewordsRow - 1];
        otherCodewords[4] = previousColumnCodewords[codewordsRow - 1];
        otherCodewords[5] = nextColumnCodewords[codewordsRow - 1];
      }
      if (codewordsRow > 1) {
        otherCodewords[8] = codewords[codewordsRow - 2];
        otherCodewords[10] = previousColumnCodewords[codewordsRow - 2];
        otherCodewords[11] = nextColumnCodewords[codewordsRow - 2];
      }
      if (codewordsRow < codewords.length - 1) {
        otherCodewords[1] = codewords[codewordsRow + 1];
        otherCodewords[6] = previousColumnCodewords[codewordsRow + 1];
        otherCodewords[7] = nextColumnCodewords[codewordsRow + 1];
      }
      if (codewordsRow < codewords.length - 2) {
        otherCodewords[9] = codewords[codewordsRow + 2];
        otherCodewords[12] = previousColumnCodewords[codewordsRow + 2];
        otherCodewords[13] = nextColumnCodewords[codewordsRow + 2];
      }
      for (Codeword otherCodeword : otherCodewords) {
        if (adjustRowNumber(codeword, otherCodeword)) {
          return;
        }
      }
    }

    // TODO maybe we should add missing codeword to store the correct row number to make
    // finding row numbers for other columns easier
    // use row height count to make detection of invalid row numbers more reliable
    private void adjustIndicatorColumnRowNumbers(DetectionResultColumn detectionResultColumn) {
      if (detectionResultColumn == null) {
        return;
      }
      Codeword[] codewords = detectionResultColumn.getCodewords();
      int barcodeRow = 0;
      int previousRowHeight = 0;
      int currentRowHeight = 0;
      for (int codewordsRow = 0; codewordsRow < codewords.length; codewordsRow++) {
        if (codewords[codewordsRow] == null) {
          continue;
        }
        if (codewords[codewordsRow].getRowNumber() == barcodeRow) {
          currentRowHeight++;
        } else if (codewords[codewordsRow].getRowNumber() < barcodeRow) {
          log(3, "removing codeword, rowNumber should not decrease, codeword[" + codewordsRow + "]: " +
              codewords[codewordsRow].getRowNumber() + ", value: " + codewords[codewordsRow].getValue());
          codewords[codewordsRow] = null;
        } else if (codewords[codewordsRow].getRowNumber() > barcodeRowCount) {
          log(3, "removing codeword, rowNumber too big, codeword[" + codewordsRow + "]: " +
              codewords[codewordsRow].getRowNumber() + ", value: " + codewords[codewordsRow].getValue());
          codewords[codewordsRow] = null;
        } else {
          // TODO add check to prevent jumping of row numbers.
          // add more precise checks
          // can only increase by more than 1 row if we have previous skipped codewords 
          if (codewordsRow > 0) {
            if (codewords[codewordsRow].getRowNumber() > barcodeRow + 3) {
              if (codewords[codewordsRow - 1] == null) {
                log(3, "row number jump, removing codeword[" + codewordsRow + "]: previous row: " +
                    barcodeRow + ", new row: " + codewords[codewordsRow].getRowNumber() + ", value: " +
                    codewords[codewordsRow].getValue());
                codewords[codewordsRow] = null;
              } else {
                log(3, "removing codeword, rowNumber should not decrease, codeword[" + codewordsRow + "]: " +
                    codewords[codewordsRow].getRowNumber() + ", value: " + codewords[codewordsRow].getValue());
                codewords[codewordsRow] = null;
              }
            } else {
              // TODO avoid jumps in row height
              previousRowHeight = currentRowHeight;
              barcodeRow = codewords[codewordsRow].getRowNumber();
            }
          }
        }
      }
    }

    /**
     * 
     * @param codeword
     * @param otherCodeword
     * @return true, if row number was adjusted, false otherwise
     */
    private boolean adjustRowNumber(Codeword codeword, Codeword otherCodeword) {
      if (otherCodeword == null) {
        return false;
      }
      if (otherCodeword.getRowNumber() != BARCODE_ROW_UNKNOWN &&
          otherCodeword.getBucket() == codeword.getBucket()) {
        codeword.setRowNumber(otherCodeword.getRowNumber());
        log(0,
            "corrected rowNumber: codeword: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
        return true;
      }
      return false;
    }

    public int getBarcodeColumnCount() {
      return barcodeColumnCount;
    }

    public int getBarcodeRowCount() {
      return barcodeRowCount;
    }

    public int getBarcodeECLevel() {
      return barcodeECLevel;
    }

    public BoundingBox getBoundingBox() {
      return boundingBox;
    }
  }

  private static class DetectionResultColumn {
    private static final int MAX_NEARBY_DISTANCE = 5;
    private final BoundingBox boundingBox;
    private final Codeword[] codewords;

    public DetectionResultColumn(final BoundingBox boundingBox) {
      this.boundingBox = boundingBox;
      codewords = new Codeword[boundingBox.getMaxY() - boundingBox.getMinY() + 1];
    }

    public Codeword getCodewordNearby(int imageRow) {
      Codeword codeword = getCodeword(imageRow);
      if (codeword != null) {
        return codeword;
      }
      for (int i = 1; i < MAX_NEARBY_DISTANCE; i++) {
        int nearImageRow = getCodewordsIndex(imageRow) - i;
        if (nearImageRow >= 0) {
          codeword = codewords[nearImageRow];
          if (codeword != null) {
            return codeword;
          }
        }
        nearImageRow = getCodewordsIndex(imageRow) + i;
        if (nearImageRow < codewords.length) {
          codeword = codewords[nearImageRow];
          if (codeword != null) {
            return codeword;
          }
        }
      }
      return null;
    }

    private int getCodewordsIndex(int imageRow) {
      return imageRow - boundingBox.getMinY();
    }

    public int getImageRow(int codewordIndex) {
      return boundingBox.getMinY() + codewordIndex;
    }

    public void setCodeword(int imageRow, Codeword codeword) {
      codewords[getCodewordsIndex(imageRow)] = codeword;
    }

    public Codeword getCodeword(int imageRow) {
      try {
        return codewords[getCodewordsIndex(imageRow)];
      } catch (ArrayIndexOutOfBoundsException e) {
        e.printStackTrace();
      }
      return null;
    }

    public BoundingBox getBoundingBox() {
      return boundingBox;
    }

    public Codeword[] getCodewords() {
      return codewords;
    }
  }

  private static class Codeword {
    private final int startX;
    private final int endX;
    private final int bucket;
    private final int value;
    private int rowNumber;

    public Codeword(int startX, int endX, int bucket, int value, int rowNumber) {
      this.startX = startX;
      this.endX = endX;
      this.bucket = bucket;
      this.value = value;
      this.rowNumber = rowNumber;
    }

    public int getStartX() {
      return startX;
    }

    public int getEndX() {
      return endX;
    }

    public int getBucket() {
      return bucket;
    }

    public int getValue() {
      return value;
    }

    public int getRowNumber() {
      return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
      this.rowNumber = rowNumber;
    }
  }

  private static class BoundingBox {
    private final ResultPoint topLeft;
    private final ResultPoint bottomLeft;
    private ResultPoint topRight;
    private ResultPoint bottomRight;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private final int maxCodewordWidth;

    public BoundingBox(final ResultPoint topLeft,
                       final ResultPoint bottomLeft,
                       final ResultPoint topRight,
                       final ResultPoint bottomRight,
                       int maxCodewordWidth) {
      this.topLeft = topLeft;
      this.bottomLeft = bottomLeft;
      this.topRight = topRight;
      this.bottomRight = bottomRight;
      this.maxCodewordWidth = maxCodewordWidth;
      calculateMinMaxValues();
    }

    private void calculateMinMaxValues() {
      minX = (int) Math.min(topLeft.getX(), bottomLeft.getX());

      if (topRight != null && bottomRight != null) {
        maxX = (int) Math.max(topRight.getX(), bottomRight.getX());
        minY = (int) Math.min(topLeft.getY(), topRight.getY());
        maxY = (int) Math.max(bottomLeft.getY(), bottomRight.getY());
      } else {
        maxX = (int) topLeft.getX() + maxCodewordWidth;
        minY = (int) topLeft.getY();
        maxY = (int) bottomLeft.getY();
      }
    }

    public void setTopRight(ResultPoint topRight) {
      this.topRight = topRight;
      calculateMinMaxValues();
    }

    public void setBottomRight(ResultPoint bottomRight) {
      this.bottomRight = bottomRight;
      calculateMinMaxValues();
    }

    public int getMinX() {
      return minX;
    }

    public int getMaxX() {
      return maxX;
    }

    public int getMinY() {
      return minY;
    }

    public int getMaxY() {
      return maxY;
    }

    public ResultPoint getTopLeft() {
      return topLeft;
    }

    public ResultPoint getBottomLeft() {
      return bottomLeft;
    }

    public ResultPoint getTopRight() {
      return topRight;
    }

    public ResultPoint getBottomRight() {
      return bottomRight;
    }

    public int getMaxCodewordWidth() {
      return maxCodewordWidth;
    }
  }

  private static void log(int level, String string, int imageRow, int imageColumn, int barcodeRow,
                          int barcodeColumn) {
    log(level, string + ", barcodeRow: " + barcodeRow + ", barcodeColumn: " + barcodeColumn, imageRow,
        imageColumn);
  }

  private static void log(int level, String string) {
    log(level, string, -1, -1);
  }

  private static void log(int level, String string, int imageRow, int imageColumn) {
    if (level >= 4) {
      if (imageRow != -1) {
        string += ", imageRow: " + imageRow;
      }
      if (imageColumn != -1) {
        string += ", imageColumn: " + imageColumn;
      }
      System.err.println(string);
    }
  }
}
