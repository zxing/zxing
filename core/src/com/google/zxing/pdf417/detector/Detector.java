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
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.GridSampler;

import java.util.Arrays;
import java.util.Map;

/**
 * <p>Encapsulates logic that can detect a PDF417 Code in an image, even if the
 * PDF417 Code is rotated or skewed, or partially obscured.</p>
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class Detector {

  private static final int MAX_AVG_VARIANCE = (int) ((1 << 8) * 0.42f);
  private static final int MAX_INDIVIDUAL_VARIANCE = (int) ((1 << 8) * 0.8f);
  private static final int SKEW_THRESHOLD = 2;

  // B S B S B S B S Bar/Space pattern
  // 11111111 0 1 0 1 0 1 000
  private static final int[] START_PATTERN = {8, 1, 1, 1, 1, 1, 1, 3};

  // 11111111 0 1 0 1 0 1 000
  private static final int[] START_PATTERN_REVERSE = {3, 1, 1, 1, 1, 1, 1, 8};

  // 1111111 0 1 000 1 0 1 00 1
  private static final int[] STOP_PATTERN = {7, 1, 1, 3, 1, 1, 1, 2, 1};

  // B S B S B S B S B Bar/Space pattern
  // 1111111 0 1 000 1 0 1 00 1
  private static final int[] STOP_PATTERN_REVERSE = {1, 2, 1, 1, 1, 3, 1, 1, 7};

  private final BinaryBitmap image;

  public Detector(BinaryBitmap image) {
    this.image = image;
  }

  /**
   * <p>Detects a PDF417 Code in an image, simply.</p>
   *
   * @return {@link DetectorResult} encapsulating results of detecting a PDF417 Code
   * @throws NotFoundException if no QR Code can be found
   */
  public DetectorResult detect() throws NotFoundException {
    return detect(null);
  }

  /**
   * <p>Detects a PDF417 Code in an image. Only checks 0 and 180 degree rotations.</p>
   *
   * @param hints optional hints to detector
   * @return {@link DetectorResult} encapsulating results of detecting a PDF417 Code
   * @throws NotFoundException if no PDF417 Code can be found
   */
  public DetectorResult detect(Map<DecodeHintType,?> hints) throws NotFoundException {
    // Fetch the 1 bit matrix once up front.
    BitMatrix matrix = image.getBlackMatrix();

    // Try to find the vertices assuming the image is upright.
    ResultPoint[] vertices = findVertices(matrix);
    if (vertices == null) {
      // Maybe the image is rotated 180 degrees?
      vertices = findVertices180(matrix);
      if (vertices != null) {
        correctCodeWordVertices(vertices, true);
      }
    } else {
      correctCodeWordVertices(vertices, false);
    }

    if (vertices == null) {
      throw NotFoundException.getNotFoundInstance();
    }

    float moduleWidth = computeModuleWidth(vertices);
    if (moduleWidth < 1.0f) {
      throw NotFoundException.getNotFoundInstance();
    }

    int dimension = computeDimension(vertices[4], vertices[6],
        vertices[5], vertices[7], moduleWidth);
    if (dimension < 1) {
      throw NotFoundException.getNotFoundInstance();
    }

    // Deskew and sample image.
    BitMatrix bits = sampleGrid(matrix, vertices[4], vertices[5],
        vertices[6], vertices[7], dimension);
    return new DetectorResult(bits, new ResultPoint[]{vertices[5],
        vertices[4], vertices[6], vertices[7]});
  }

  /**
   * Locate the vertices and the codewords area of a black blob using the Start
   * and Stop patterns as locators.
   * TODO: Scanning every row is very expensive. We should only do this for TRY_HARDER.
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
  private static ResultPoint[] findVertices(BitMatrix matrix) {
    int height = matrix.getHeight();
    int width = matrix.getWidth();

    ResultPoint[] result = new ResultPoint[8];
    boolean found = false;

    int[] counters = new int[START_PATTERN.length];

    // Top Left
    for (int i = 0; i < height; i++) {
      int[] loc = findGuardPattern(matrix, 0, i, width, false, START_PATTERN, counters);
      if (loc != null) {
        result[0] = new ResultPoint(loc[0], i);
        result[4] = new ResultPoint(loc[1], i);
        found = true;
        break;
      }
    }
    // Bottom left
    if (found) { // Found the Top Left vertex
      found = false;
      for (int i = height - 1; i > 0; i--) {
        int[] loc = findGuardPattern(matrix, 0, i, width, false, START_PATTERN, counters);
        if (loc != null) {
          result[1] = new ResultPoint(loc[0], i);
          result[5] = new ResultPoint(loc[1], i);
          found = true;
          break;
        }
      }
    }

    counters = new int[STOP_PATTERN.length];

    // Top right
    if (found) { // Found the Bottom Left vertex
      found = false;
      for (int i = 0; i < height; i++) {
        int[] loc = findGuardPattern(matrix, 0, i, width, false, STOP_PATTERN, counters);
        if (loc != null) {
          result[2] = new ResultPoint(loc[1], i);
          result[6] = new ResultPoint(loc[0], i);
          found = true;
          break;
        }
      }
    }
    // Bottom right
    if (found) { // Found the Top right vertex
      found = false;
      for (int i = height - 1; i > 0; i--) {
        int[] loc = findGuardPattern(matrix, 0, i, width, false, STOP_PATTERN, counters);
        if (loc != null) {
          result[3] = new ResultPoint(loc[1], i);
          result[7] = new ResultPoint(loc[0], i);
          found = true;
          break;
        }
      }
    }
    return found ? result : null;
  }

  /**
   * Locate the vertices and the codewords area of a black blob using the Start
   * and Stop patterns as locators. This assumes that the image is rotated 180
   * degrees and if it locates the start and stop patterns at it will re-map
   * the vertices for a 0 degree rotation.
   * TODO: Change assumption about barcode location.
   * TODO: Scanning every row is very expensive. We should only do this for TRY_HARDER.
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
  private static ResultPoint[] findVertices180(BitMatrix matrix) {
    int height = matrix.getHeight();
    int width = matrix.getWidth();
    int halfWidth = width >> 1;

    ResultPoint[] result = new ResultPoint[8];
    boolean found = false;

    int[] counters = new int[START_PATTERN_REVERSE.length];
    
    // Top Left
    for (int i = height - 1; i > 0; i--) {
      int[] loc = findGuardPattern(matrix, halfWidth, i, halfWidth, true, START_PATTERN_REVERSE, counters);
      if (loc != null) {
        result[0] = new ResultPoint(loc[1], i);
        result[4] = new ResultPoint(loc[0], i);
        found = true;
        break;
      }
    }
    // Bottom Left
    if (found) { // Found the Top Left vertex
      found = false;
      for (int i = 0; i < height; i++) {
        int[] loc = findGuardPattern(matrix, halfWidth, i, halfWidth, true, START_PATTERN_REVERSE, counters);
        if (loc != null) {
          result[1] = new ResultPoint(loc[1], i);
          result[5] = new ResultPoint(loc[0], i);
          found = true;
          break;
        }
      }
    }
    
    counters = new int[STOP_PATTERN_REVERSE.length];
    
    // Top Right
    if (found) { // Found the Bottom Left vertex
      found = false;
      for (int i = height - 1; i > 0; i--) {
        int[] loc = findGuardPattern(matrix, 0, i, halfWidth, false, STOP_PATTERN_REVERSE, counters);
        if (loc != null) {
          result[2] = new ResultPoint(loc[0], i);
          result[6] = new ResultPoint(loc[1], i);
          found = true;
          break;
        }
      }
    }
    // Bottom Right
    if (found) { // Found the Top Right vertex
      found = false;
      for (int i = 0; i < height; i++) {
        int[] loc = findGuardPattern(matrix, 0, i, halfWidth, false, STOP_PATTERN_REVERSE, counters);
        if (loc != null) {
          result[3] = new ResultPoint(loc[0], i);
          result[7] = new ResultPoint(loc[1], i);
          found = true;
          break;
        }
      }
    }
    return found ? result : null;
  }

  /**
   * Because we scan horizontally to detect the start and stop patterns, the vertical component of
   * the codeword coordinates will be slightly wrong if there is any skew or rotation in the image.
   * This method moves those points back onto the edges of the theoretically perfect bounding
   * quadrilateral if needed.
   *
   * @param vertices The eight vertices located by findVertices().
   */
  private static void correctCodeWordVertices(ResultPoint[] vertices, boolean upsideDown) {
    float skew = vertices[4].getY() - vertices[6].getY();
    if (upsideDown) {
      skew = -skew;
    }
    if (skew > SKEW_THRESHOLD) {
      // Fix v4
      float length = vertices[4].getX() - vertices[0].getX();
      float deltax = vertices[6].getX() - vertices[0].getX();
      float deltay = vertices[6].getY() - vertices[0].getY();
      float correction = length * deltay / deltax;
      vertices[4] = new ResultPoint(vertices[4].getX(), vertices[4].getY() + correction);
    } else if (-skew > SKEW_THRESHOLD) {
      // Fix v6
      float length = vertices[2].getX() - vertices[6].getX();
      float deltax = vertices[2].getX() - vertices[4].getX();
      float deltay = vertices[2].getY() - vertices[4].getY();
      float correction = length * deltay / deltax;
      vertices[6] = new ResultPoint(vertices[6].getX(), vertices[6].getY() - correction);
    }

    skew = vertices[7].getY() - vertices[5].getY();
    if (upsideDown) {
      skew = -skew;
    }
    if (skew > SKEW_THRESHOLD) {
      // Fix v5
      float length = vertices[5].getX() - vertices[1].getX();
      float deltax = vertices[7].getX() - vertices[1].getX();
      float deltay = vertices[7].getY() - vertices[1].getY();
      float correction = length * deltay / deltax;
      vertices[5] = new ResultPoint(vertices[5].getX(), vertices[5].getY() + correction);
    } else if (-skew > SKEW_THRESHOLD) {
      // Fix v7
      float length = vertices[3].getX() - vertices[7].getX();
      float deltax = vertices[3].getX() - vertices[5].getX();
      float deltay = vertices[3].getY() - vertices[5].getY();
      float correction = length * deltay / deltax;
      vertices[7] = new ResultPoint(vertices[7].getX(), vertices[7].getY() - correction);
    }
  }

  /**
   * <p>Estimates module size (pixels in a module) based on the Start and End
   * finder patterns.</p>
   *
   * @param vertices an array of vertices:
   *           vertices[0] x, y top left barcode
   *           vertices[1] x, y bottom left barcode
   *           vertices[2] x, y top right barcode
   *           vertices[3] x, y bottom right barcode
   *           vertices[4] x, y top left codeword area
   *           vertices[5] x, y bottom left codeword area
   *           vertices[6] x, y top right codeword area
   *           vertices[7] x, y bottom right codeword area
   * @return the module size.
   */
  private static float computeModuleWidth(ResultPoint[] vertices) {
    float pixels1 = ResultPoint.distance(vertices[0], vertices[4]);
    float pixels2 = ResultPoint.distance(vertices[1], vertices[5]);
    float moduleWidth1 = (pixels1 + pixels2) / (17 * 2.0f);
    float pixels3 = ResultPoint.distance(vertices[6], vertices[2]);
    float pixels4 = ResultPoint.distance(vertices[7], vertices[3]);
    float moduleWidth2 = (pixels3 + pixels4) / (18 * 2.0f);
    return (moduleWidth1 + moduleWidth2) / 2.0f;
  }

  /**
   * Computes the dimension (number of modules in a row) of the PDF417 Code
   * based on vertices of the codeword area and estimated module size.
   *
   * @param topLeft     of codeword area
   * @param topRight    of codeword area
   * @param bottomLeft  of codeword area
   * @param bottomRight of codeword are
   * @param moduleWidth estimated module size
   * @return the number of modules in a row.
   */
  private static int computeDimension(ResultPoint topLeft,
                                      ResultPoint topRight,
                                      ResultPoint bottomLeft,
                                      ResultPoint bottomRight,
                                      float moduleWidth) {
    int topRowDimension = round(ResultPoint.distance(topLeft, topRight) / moduleWidth);
    int bottomRowDimension = round(ResultPoint.distance(bottomLeft, bottomRight) / moduleWidth);
    return ((((topRowDimension + bottomRowDimension) >> 1) + 8) / 17) * 17;
  }

  private static BitMatrix sampleGrid(BitMatrix matrix,
                                      ResultPoint topLeft,
                                      ResultPoint bottomLeft,
                                      ResultPoint topRight,
                                      ResultPoint bottomRight,
                                      int dimension) throws NotFoundException {

    // Note that unlike the QR Code sampler, we didn't find the center of modules, but the
    // very corners. So there is no 0.5f here; 0.0f is right.
    GridSampler sampler = GridSampler.getInstance();

    return sampler.sampleGrid(
        matrix, 
        dimension, dimension,
        0.0f, // p1ToX
        0.0f, // p1ToY
        dimension, // p2ToX
        0.0f, // p2ToY
        dimension, // p3ToX
        dimension, // p3ToY
        0.0f, // p4ToX
        dimension, // p4ToY
        topLeft.getX(), // p1FromX
        topLeft.getY(), // p1FromY
        topRight.getX(), // p2FromX
        topRight.getY(), // p2FromY
        bottomRight.getX(), // p3FromX
        bottomRight.getY(), // p3FromY
        bottomLeft.getX(), // p4FromX
        bottomLeft.getY()); // p4FromY
  }

  /**
   * Ends up being a bit faster than Math.round(). This merely rounds its
   * argument to the nearest int, where x.5 rounds up.
   */
  private static int round(float d) {
    return (int) (d + 0.5f);
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
    int patternLength = pattern.length;
    boolean isWhite = whiteFirst;

    int counterPosition = 0;
    int patternStart = column;
    for (int x = column; x < column + width; x++) {
      boolean pixel = matrix.get(x, row);
      if (pixel ^ isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
            return new int[]{patternStart, x};
          }
          patternStart += counters[0] + counters[1];
          System.arraycopy(counters, 2, counters, 0, patternLength - 2);
          counters[patternLength - 2] = 0;
          counters[patternLength - 1] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
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
   * @return ratio of total variance between counters and pattern compared to
   *         total pattern size, where the ratio has been multiplied by 256.
   *         So, 0 means no variance (perfect match); 256 means the total
   *         variance between counters and patterns equals the pattern length,
   *         higher values mean even more variance
   */
  private static int patternMatchVariance(int[] counters, int[] pattern, int maxIndividualVariance) {
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
      return Integer.MAX_VALUE;
    }
    // We're going to fake floating-point math in integers. We just need to use more bits.
    // Scale up patternLength so that intermediate values below like scaledCounter will have
    // more "significant digits".
    int unitBarWidth = (total << 8) / patternLength;
    maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> 8;

    int totalVariance = 0;
    for (int x = 0; x < numCounters; x++) {
      int counter = counters[x] << 8;
      int scaledPattern = pattern[x] * unitBarWidth;
      int variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
      if (variance > maxIndividualVariance) {
        return Integer.MAX_VALUE;
      }
      totalVariance += variance;
    }
    return totalVariance / total;
  }

}
