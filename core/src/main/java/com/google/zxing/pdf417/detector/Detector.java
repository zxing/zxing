/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.pdf417.detector;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>Encapsulates logic that can detect a PDF417 Code in an image, even if the
 * PDF417 Code is rotated or skewed, or partially obscured.</p>
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Guenther Grau
 */
public final class Detector {

  private static final int[] INDEXES_START_PATTERN = {0, 4, 1, 5};
  private static final int[] INDEXES_STOP_PATTERN = {6, 2, 7, 3};
  private static final float MAX_AVG_VARIANCE = 0.42f;
  private static final float MAX_INDIVIDUAL_VARIANCE = 0.8f;

  // B S B S B S B S Bar/Space pattern
  // 11111111 0 1 0 1 0 1 000
  private static final int[] START_PATTERN = {8, 1, 1, 1, 1, 1, 1, 3};
  // 1111111 0 1 000 1 0 1 00 1
  private static final int[] STOP_PATTERN = {7, 1, 1, 3, 1, 1, 1, 2, 1};
  private static final int MAX_PIXEL_DRIFT = 3;
  private static final int MAX_PATTERN_DRIFT = 5;
  // if we set the value too low, then we don't detect the correct height of the bar if the start patterns are damaged.
  // if we set the value too high, then we might detect the start pattern from a neighbor barcode.
  private static final int SKIPPED_ROW_COUNT_MAX = 25;
  // A PDF471 barcode should have at least 3 rows, with each row being >= 3 times the module width. Therefore it should be at least
  // 9 pixels tall. To be conservative, we use about half the size to ensure we don't miss it.
  private static final int ROW_STEP = 5;
  private static final int BARCODE_MIN_HEIGHT = 10;

  private Detector() {
  }

  /**
   * <p>Detects a PDF417 Code in an image. Only checks 0 and 180 degree rotations.</p>
   *
   * @param image barcode image to decode
   * @param hints optional hints to detector
   * @param multiple if true, then the image is searched for multiple codes. If false, then at most one code will
   * be found and returned
   * @return {@link PDF417DetectorResult} encapsulating results of detecting a PDF417 code
   * @throws NotFoundException if no PDF417 Code can be found
   */
  public static PDF417DetectorResult detect(BinaryBitmap image, Map<DecodeHintType,?> hints, boolean multiple)
      throws NotFoundException {
    // TODO detection improvement, tryHarder could try several different luminance thresholds/blackpoints or even 
    // different binarizers
    //boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);

    BitMatrix bitMatrix = image.getBlackMatrix();

    List<ResultPoint[]> barcodeCoordinates = detect(multiple, bitMatrix);
    if (barcodeCoordinates.isEmpty()) {
      bitMatrix = bitMatrix.clone();
      bitMatrix.rotate180();
      barcodeCoordinates = detect(multiple, bitMatrix);
    }
    return new PDF417DetectorResult(bitMatrix, barcodeCoordinates);
  }

  /**
   * Detects PDF417 codes in an image. Only checks 0 degree rotation
   * @param multiple if true, then the image is searched for multiple codes. If false, then at most one code will
   * be found and returned
   * @param bitMatrix bit matrix to detect barcodes in
   * @return List of ResultPoint arrays containing the coordinates of found barcodes
   */
  private static List<ResultPoint[]> detect(boolean multiple, BitMatrix bitMatrix) {
    List<ResultPoint[]> barcodeCoordinates = new ArrayList<>();
    int row = 0;
    int column = 0;
    boolean foundBarcodeInRow = false;
    while (row < bitMatrix.getHeight()) {
      ResultPoint[] vertices = findVertices(bitMatrix, row, column);

      if (vertices[0] == null && vertices[3] == null) {
        if (!foundBarcodeInRow) {
          // we didn't find any barcode so that's the end of searching 
          break;
        }
        // we didn't find a barcode starting at the given column and row. Try again from the first column and slightly
        // below the lowest barcode we found so far.
        foundBarcodeInRow = false;
        column = 0;
        for (ResultPoint[] barcodeCoordinate : barcodeCoordinates) {
          if (barcodeCoordinate[1] != null) {
            row = (int) Math.max(row, barcodeCoordinate[1].getY());
          }
          if (barcodeCoordinate[3] != null) {
            row = Math.max(row, (int) barcodeCoordinate[3].getY());
          }
        }
        row += ROW_STEP;
        continue;
      }
      foundBarcodeInRow = true;
      barcodeCoordinates.add(vertices);
      if (!multiple) {
        break;
      }
      // if we didn't find a right row indicator column, then continue the search for the next barcode after the 
      // start pattern of the barcode just found.
      if (vertices[2] != null) {
        column = (int) vertices[2].getX();
        row = (int) vertices[2].getY();
      } else {
        column = (int) vertices[4].getX();
        row = (int) vertices[4].getY();
      }
    }
    return barcodeCoordinates;
  }

  /**
   * Locate the vertices and the codewords area of a black blob using the Start
   * and Stop patterns as locators.
   *
   * @param matrix the scanned barcode image.
   * @return an array containing the vertices:
   *           vertices[0] x, y top left barcode
   *           vertices[1] x, y bottom left barcode
   *           vertices[2] x, y top right barcode
   *           vertices[3] x, y bottom right barcode
   *           vertices[4] x, y top left codeword area
   *           vertices[5] x, y bottom left codeword area
   *           vertices[6] x, y top right codeword area
   *           vertices[7] x, y bottom right codeword area
   */
  private static ResultPoint[] findVertices(BitMatrix matrix, int startRow, int startColumn) {
    int height = matrix.getHeight();
    int width = matrix.getWidth();

    ResultPoint[] result = new ResultPoint[8];
    copyToResult(result, findRowsWithPattern(matrix, height, width, startRow, startColumn, START_PATTERN),
        INDEXES_START_PATTERN);

    if (result[4] != null) {
      startColumn = (int) result[4].getX();
      startRow = (int) result[4].getY();
    }
    copyToResult(result, findRowsWithPattern(matrix, height, width, startRow, startColumn, STOP_PATTERN),
        INDEXES_STOP_PATTERN);
    return result;
  }

  private static void copyToResult(ResultPoint[] result, ResultPoint[] tmpResult, int[] destinationIndexes) {
    for (int i = 0; i < destinationIndexes.length; i++) {
      result[destinationIndexes[i]] = tmpResult[i];
    }
  }

  private static ResultPoint[] findRowsWithPattern(BitMatrix matrix,
                                                   int height,
                                                   int width,
                                                   int startRow,
                                                   int startColumn,
                                                   int[] pattern) {
    ResultPoint[] result = new ResultPoint[4];
    boolean found = false;
    int[] counters = new int[pattern.length];
    for (; startRow < height; startRow += ROW_STEP) {
      int[] loc = findGuardPattern(matrix, startColumn, startRow, width, false, pattern, counters);
      if (loc != null) {
        while (startRow > 0) {
          int[] previousRowLoc = findGuardPattern(matrix, startColumn, --startRow, width, false, pattern, counters);
          if (previousRowLoc != null) {
            loc = previousRowLoc;
          } else {
            startRow++;
            break;
          }
        }
        result[0] = new ResultPoint(loc[0], startRow);
        result[1] = new ResultPoint(loc[1], startRow);
        found = true;
        break;
      }
    }
    int stopRow = startRow + 1;
    // Last row of the current symbol that contains pattern
    if (found) {
      int skippedRowCount = 0;
      int[] previousRowLoc = {(int) result[0].getX(), (int) result[1].getX()};
      for (; stopRow < height; stopRow++) {
        int[] loc = findGuardPattern(matrix, previousRowLoc[0], stopRow, width, false, pattern, counters);
        // a found pattern is only considered to belong to the same barcode if the start and end positions
        // don't differ too much. Pattern drift should be not bigger than two for consecutive rows. With
        // a higher number of skipped rows drift could be larger. To keep it simple for now, we allow a slightly
        // larger drift and don't check for skipped rows.
        if (loc != null &&
            Math.abs(previousRowLoc[0] - loc[0]) < MAX_PATTERN_DRIFT &&
            Math.abs(previousRowLoc[1] - loc[1]) < MAX_PATTERN_DRIFT) {
          previousRowLoc = loc;
          skippedRowCount = 0;
        } else {
          if (skippedRowCount > SKIPPED_ROW_COUNT_MAX) {
            break;
          } else {
            skippedRowCount++;
          }
        }
      }
      stopRow -= skippedRowCount + 1;
      result[2] = new ResultPoint(previousRowLoc[0], stopRow);
      result[3] = new ResultPoint(previousRowLoc[1], stopRow);
    }
    if (stopRow - startRow < BARCODE_MIN_HEIGHT) {
      Arrays.fill(result, null);
    }
    return result;
  }

  /**
   * @param matrix row of black/white values to search
   * @param column x position to start search
   * @param row y position to start search
   * @param width the number of pixels to search on this row
   * @param pattern pattern of counts of number of black and white pixels that are
   *                 being searched for as a pattern
   * @param counters array of counters, as long as pattern, to re-use 
   * @return start/end horizontal offset of guard pattern, as an array of two ints.
   */
  private static int[] findGuardPattern(BitMatrix matrix,
                                        int column,
                                        int row,
                                        int width,
                                        boolean whiteFirst,
                                        int[] pattern,
                                        int[] counters) {
    Arrays.fill(counters, 0, counters.length, 0);
    int patternStart = column;
    int pixelDrift = 0;

    // if there are black pixels left of the current pixel shift to the left, but only for MAX_PIXEL_DRIFT pixels 
    while (matrix.get(patternStart, row) && patternStart > 0 && pixelDrift++ < MAX_PIXEL_DRIFT) {
      patternStart--;
    }
    int x = patternStart;
    int counterPosition = 0;
    int patternLength = pattern.length;
    for (boolean isWhite = whiteFirst; x < width; x++) {
      boolean pixel = matrix.get(x, row);
      if (pixel != isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
            return new int[] {patternStart, x};
          }
          patternStart += counters[0] + counters[1];
          System.arraycopy(counters, 2, counters, 0, counterPosition - 1);
          counters[counterPosition - 1] = 0;
          counters[counterPosition] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    if (counterPosition == patternLength - 1 &&
        patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
      return new int[] {patternStart, x - 1};
    }
    return null;
  }

  /**
   * Determines how closely a set of observed counts of runs of black/white
   * values matches a given target pattern. This is reported as the ratio of
   * the total variance from the expected pattern proportions across all
   * pattern elements, to the length of the pattern.
   *
   * @param counters observed counters
   * @param pattern expected pattern
   * @param maxIndividualVariance The most any counter can differ before we give up
   * @return ratio of total variance between counters and pattern compared to total pattern size
   */
  private static float patternMatchVariance(int[] counters, int[] pattern, float maxIndividualVariance) {
    int numCounters = counters.length;
    int total = 0;
    int patternLength = 0;
    for (int i = 0; i < numCounters; i++) {
      total += counters[i];
      patternLength += pattern[i];
    }
    if (total < patternLength) {
      // If we don't even have one pixel per unit of bar width, assume this
      // is too small to reliably match, so fail:
      return Float.POSITIVE_INFINITY;
    }
    // We're going to fake floating-point math in integers. We just need to use more bits.
    // Scale up patternLength so that intermediate values below like scaledCounter will have
    // more "significant digits".
    float unitBarWidth = (float) total / patternLength;
    maxIndividualVariance *= unitBarWidth;

    float totalVariance = 0.0f;
    for (int x = 0; x < numCounters; x++) {
      int counter = counters[x];
      float scaledPattern = pattern[x] * unitBarWidth;
      float variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
      if (variance > maxIndividualVariance) {
        return Float.POSITIVE_INFINITY;
      }
      totalVariance += variance;
    }
    return totalVariance / total;
  }
}
