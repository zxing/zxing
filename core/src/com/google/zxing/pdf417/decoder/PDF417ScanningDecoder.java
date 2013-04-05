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
public final class PDF417ScanningDecoder {

  private static final int STOP_PATTERN_VALUE = 130324;

  private static final int CODEWORD_SKEW_SIZE = 2;

  public static DecoderResult decode(TransformableBitMatrix image,
                                     final ResultPoint imageTopLeft,
                                     final ResultPoint imageBottomLeft,
                                     final ResultPoint imageTopRight,
                                     final ResultPoint imageBottomRight,
                                     int minCodewordWidth, int maxCodewordWidth)
      throws NotFoundException, FormatException, ChecksumException {

    int maxWidth = (int) (Math.max(imageTopRight.getX(), imageBottomRight.getX()) - Math
        .min(
            imageTopLeft.getX(), imageBottomLeft.getX()));
    int maxHeight = (int) (Math.max(imageBottomLeft.getY(), imageBottomRight.getY()) - Math
        .min(
            imageTopLeft.getY(), imageTopRight.getY()));

    PerspectiveTransform perspectiveTransform = PerspectiveTransform
        .quadrilateralToQuadrilateral(
            imageTopLeft.getX(), imageTopLeft.getY(),
            imageTopRight.getX(), imageTopRight.getY(),
            imageBottomRight.getX(), imageBottomRight.getY(),
            imageBottomLeft.getX(), imageBottomLeft.getY(),
            0, 0,
            maxWidth, 0,
            maxWidth, maxHeight,
            0, maxHeight
        );

    float[] testPoints = new float[] { 170, 63, 343, 89, 319, 187, 150, 182, 219, 68 };
    perspectiveTransform.transformPoints(testPoints);
    System.out.println("x: " + testPoints[0] + ", y: " + testPoints[1]);
    final int imageWidth = image.getWidth();
    final int imageTopLeftX = (int) imageTopLeft.getX();
    final int imageTopLeftY = (int) imageTopLeft.getY();
    final int imageBottomLeftY = (int) imageBottomLeft.getY();
    int imageMaxX = imageWidth;
    if (imageTopRight != null && imageBottomRight != null) {
      imageMaxX = Math.max((int) imageTopRight.getX(), (int) imageBottomRight.getX());
    }
    final int codewordWidth = (minCodewordWidth + maxCodewordWidth) / 2;
    int[] moduleBitCount = new int[8];
    int barcodeRowCount = -1;
    int barcodeColumnCount = -1;
    if (imageTopRight != null) {
      barcodeColumnCount = Math.round((imageTopRight.getX() - imageTopLeftX) /
          codewordWidth) - 2;
    }
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
      imageColumnStart = getBarcodeStartColumn(image, imageColumnStart, imageRow,
          minCodewordWidth, maxCodewordWidth);
      if (imageColumnStart == -1) {
        log(2, "imageColumnStart not found: " + barcodeRow, imageRow, imageColumn);
        imageColumnStart = previousImageColumnStart;
      }

      imageColumn = imageColumnStart;
      barcodeRow = previousBarcodeStartRow;
      for (int barcodeColumn = 0; imageColumn < imageMaxX; barcodeColumn++) {
        if ((barcodeColumnCount != -1 && barcodeColumn >= (barcodeColumnCount + 2))) {
          log(1, "Exceeding precalculated barcode column count " + barcodeColumnCount,
              imageRow, imageColumn);
          break;
        }

        int barcodeDecodingProblems = 0;
        int previousImageColumn = imageColumn;
        imageColumn = getBarcodeStartColumn(image, imageColumn, imageRow, minCodewordWidth,
            maxCodewordWidth);
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
          log(1, "Invalid Codeword size " + codewordBitCount + ", skipping", imageRow,
              imageColumn);
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

          log(1, "invalid barcode symbol for moduleBitCount " +
              getBitCounts(moduleBitCount) + ", original: " + getBitCounts(bitCountCopy) +
              "\n", imageRow, imageColumn, barcodeRow, barcodeColumn);
          continue;
        }
        codewordBucket = getCodewordBucketNumber(moduleBitCount);
        if (barcodeColumn == 0) {
          barcodeRow = getRowIndicatorRowNumber(codeword) + codewordBucket / 3;
          int estimatedBarcodeRow = getEstimatedBarcodeRow(perspectiveTransform,
              maxHeight, barcodeRowCount,
              imageRow, imageColumn);
          if (Math.abs(barcodeRow - estimatedBarcodeRow) > 2) {
            barcodeRow = -1;
            log(1, "skipping codeword, estimatedRow " + estimatedBarcodeRow +
                " differs from barcodeRow " + barcodeRow, imageRow, imageColumn,
                barcodeRow, barcodeColumn);
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
                log(1, "adjusting barcode column count from " + barcodeColumnCount +
                    " to " + (value + 1), imageRow, imageColumn);
                barcodeColumnCount = value + 1;
              }
              break;
          }
        }
        if (barcodeColumn == barcodeColumnCount + 1) {
          barcodeRow = getRowIndicatorRowNumber(codeword) + codewordBucket / 3;
          int estimatedBarcodeRow = getEstimatedBarcodeRow(perspectiveTransform,
              maxHeight, barcodeRowCount, imageRow, imageColumn);
          if (Math.abs(barcodeRow - estimatedBarcodeRow) > 2) {
            if (Math.abs(barcodeRow - estimatedBarcodeRow) > 2) {
              log(1, "skipping codeword, estimatedRow " + estimatedBarcodeRow +
                  " differs from barcodeRow " + barcodeRow, imageRow, imageColumn,
                  barcodeRow, barcodeColumn);
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
                log(1, "adjusting barcode column count from " + barcodeColumnCount +
                    " to " + (value + 1), imageRow, imageColumn);
                barcodeColumnCount = value + 1;
              }
              break;
          }
        }
        if (barcodeRow == -1) {
          log(1,
              "first codeword was unreadable, reusing the previous barcode start row number",
              imageRow, imageColumn);
          barcodeRow = previousBarcodeStartRow;
          barcodeDecodingProblems++;
        }
        if (barcodeColumn > 0 && codewordBucket != ((barcodeRow % 3) * 3)) {
          log(1, "we have skipped to a different barcode row", imageRow, imageColumn);
          int oldBarcodeRow = barcodeRow;
          barcodeRow = calculateSkippedBarcodeRow(barcodeRow, codewordBucket,
              getEstimatedBarcodeRow(perspectiveTransform,
                  maxHeight, barcodeRowCount, imageRow, imageColumn));
          if (barcodeRow < 0) {
            break;
          }
          if (Math.abs(barcodeRow - oldBarcodeRow) > 1) {
            barcodeRow = oldBarcodeRow;
            break;
          }
          barcodeDecodingProblems++;
        }
        int estimatedBarcodeRow = getEstimatedBarcodeRow(perspectiveTransform,
            maxHeight, barcodeRowCount, imageRow, imageColumn);
        if (Math.abs(barcodeRow - estimatedBarcodeRow) > 1) {
          barcodeRow = calculateSkippedBarcodeRow(estimatedBarcodeRow, codewordBucket,
              estimatedBarcodeRow);
          previousBarcodeStartRow = barcodeRow;
          barcodeDecodingProblems++;
        }
        if (barcodeRow < 0 || barcodeColumn < 0) {
          log(2, "negative values, barcodeRow: " + barcodeRow + ", barcodeColumn: " +
              barcodeColumn, imageRow, imageColumn);
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
        log(2,
            "Error, calculatedNumberOfCodewords to small " + calculatedNumberOfCodewords,
            -1, imageColumn);
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

  private static String getBitCounts(int[] moduleBitCount) {
    StringBuilder result = new StringBuilder("{");
    for (int bitCount : moduleBitCount) {
      result.append(bitCount).append(',');
    }
    result.setLength(result.length() - 1);
    return result.append('}').toString();
  }

  private static int getEstimatedBarcodeRow(PerspectiveTransform perspectiveTransform,
                                            int maxHeight, int barcodeRowCount,
                                            int imageRow, int imageColumn) {
    float[] points = new float[] { imageColumn, imageRow };
    perspectiveTransform.transformPoints(points);
    return (int) (points[0] * barcodeRowCount / maxHeight);
  }

  private static int getNumberOfECCodeWords(int barcodeECLevel) {
    return 2 << barcodeECLevel;
  }

  private static int calculateSkippedBarcodeRow(int barcodeRow, int newCodewordBucket,
                                                int estimatedBarcodeRow) {

    if ((((barcodeRow + 1) % 3) * 3) == newCodewordBucket) {
      return ++barcodeRow;
    }
    return --barcodeRow;
  }

  private static int getBarcodeStartColumn(TransformableBitMatrix image,
                                           final int codewordStartColumn,
                                           final int imageRow, final int minCodewordWidth,
                                           final int maxCodewordWidth) {
    int correctedImageStartColumn = codewordStartColumn;
    // there should be no black pixels before the start column. If there are, then we need to start earlier.
    while (correctedImageStartColumn > 0 &&
        image.get(--correctedImageStartColumn, imageRow)) {
      if ((codewordStartColumn - correctedImageStartColumn) > CODEWORD_SKEW_SIZE) {
        log(2, "Too many black pixels before start, using previous start position",
            imageRow, codewordStartColumn);
        return -1;
      }
    }

    final int imageWidth = image.getWidth();
    while (correctedImageStartColumn < (imageWidth - 1) &&
        !image.get(++correctedImageStartColumn, imageRow)) {
      if ((codewordStartColumn - correctedImageStartColumn) > CODEWORD_SKEW_SIZE) {
        log(2, "Too many white pixels before start, using previous start position",
            imageRow, codewordStartColumn);
        return -1;
      }
    }

    return correctedImageStartColumn;
  }

  private static boolean checkCodewordSkew(int codewordSize, int minCodewordWidth,
                                           int maxCodewordWidth) {
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
    log(2, "Codewords: " + codewords.length + ", Erasures: " + erasures.length +
        ", ecLevel: " + ecLevel, -1, -1);

    int numECCodewords = 1 << (ecLevel + 1);

    Decoder.correctErrors(codewords, erasures, numECCodewords);
    Decoder.verifyCodewordCount(codewords, numECCodewords);

    // Decode the codewords
    return DecodedBitStreamParser.decode(codewords, String.valueOf(ecLevel),
        erasures.length);
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

  private static void log(int level, String string, int imageRow, int imageColumn,
                          int barcodeRow, int barcodeColumn) {
    log(level,
        string + ", barcodeRow: " + barcodeRow + ", barcodeColumn: " + barcodeColumn,
        imageRow, imageColumn);
  }

  private static void log(int level, String string, int imageRow, int imageColumn) {
    if (level >= 0) {
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
