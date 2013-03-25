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
import com.google.zxing.common.PerspectiveTransform;
import com.google.zxing.common.detector.MathUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * <p>Encapsulates logic that can detect a PDF417 Code in an image, even if the
 * PDF417 Code is rotated or skewed, or partially obscured.</p>
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Schweers Informationstechnologie GmbH (hartmut.neubauer@schweers.de)
 * @author creatale GmbH (christoph.schulz@creatale.de)
 */
public final class Detector {

  private static final int INTEGER_MATH_SHIFT = 8;
  private static final int PATTERN_MATCH_RESULT_SCALE_FACTOR = 1 << INTEGER_MATH_SHIFT;
  private static final int MAX_AVG_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
  private static final int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.8f);

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
    int rowStep = 8;
    ResultPoint[] vertices = findVertices(matrix, rowStep);
    if (vertices == null) {
      // Maybe the image is rotated 180 degrees?
      vertices = findVertices180(matrix, rowStep);
      if (vertices != null) {
        correctVertices(matrix, vertices, true);
      }
    } else {
      correctVertices(matrix, vertices, false);
    }

    if (vertices == null) {
      throw NotFoundException.getNotFoundInstance();
    }

    float moduleWidth = computeModuleWidth(vertices);
    if (moduleWidth < 1.0f) {
      throw NotFoundException.getNotFoundInstance();
    }

    int dimension = computeDimension(vertices[12], vertices[14],
        vertices[13], vertices[15], moduleWidth);
    if (dimension < 1) {
      throw NotFoundException.getNotFoundInstance();
    }

    int yDimension = Math.max(computeYDimension(vertices[12], vertices[14],
        vertices[13], vertices[15], moduleWidth), dimension);

    // Deskew and over-sample image.
    BitMatrix linesMatrix = sampleLines(vertices, dimension, yDimension);
    BitMatrix linesGrid = new LinesSampler(linesMatrix, dimension).sample();

    //TODO: verify vertex indices.
    return new DetectorResult(linesGrid, new ResultPoint[]{ 
        vertices[5], vertices[4], vertices[6], vertices[7]});
  }

  /**
   * Locate the vertices and the codewords area of a black blob using the Start
   * and Stop patterns as locators.
   *
   * @param matrix the scanned barcode image.
   * @param rowStep the step size for iterating rows (every n-th row).
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
  private static ResultPoint[] findVertices(BitMatrix matrix, int rowStep) {
    int height = matrix.getHeight();
    int width = matrix.getWidth();

    ResultPoint[] result = new ResultPoint[16];
    boolean found = false;

    int[] counters = new int[START_PATTERN.length];

    // Top Left
    for (int i = 0; i < height; i += rowStep) {
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
      for (int i = height - 1; i > 0; i -= rowStep) {
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
      for (int i = 0; i < height; i += rowStep) {
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
      for (int i = height - 1; i > 0; i -= rowStep) {
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
   *
   * @param matrix the scanned barcode image.
   * @param rowStep the step size for iterating rows (every n-th row).
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
  private static ResultPoint[] findVertices180(BitMatrix matrix, int rowStep) {

    // TODO: Change assumption about barcode location.

    int height = matrix.getHeight();
    int width = matrix.getWidth();
    int halfWidth = width >> 1;

    ResultPoint[] result = new ResultPoint[16];
    boolean found = false;

    int[] counters = new int[START_PATTERN_REVERSE.length];

    // Top Left
    for (int i = height - 1; i > 0; i -= rowStep) {
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
      for (int i = 0; i < height; i += rowStep) {
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
      for (int i = height - 1; i > 0; i -= rowStep) {
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
      for (int i = 0; i < height; i += rowStep) {
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
    int unitBarWidth = (total << INTEGER_MATH_SHIFT) / patternLength;
    maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> 8;

    int totalVariance = 0;
    for (int x = 0; x < numCounters; x++) {
      int counter = counters[x] << INTEGER_MATH_SHIFT;
      int scaledPattern = pattern[x] * unitBarWidth;
      int variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
      if (variance > maxIndividualVariance) {
        return Integer.MAX_VALUE;
      }
      totalVariance += variance;
    }
    return totalVariance / total;
  }

  /**
   * <p>Correct the vertices by searching for top and bottom vertices of wide
   * bars, then locate the intersections between the upper and lower horizontal
   * line and the inner vertices vertical lines.</p>
   *
   * @param matrix the scanned barcode image.
   * @param vertices the vertices vector is extended and the new members are:
   *           vertices[ 8] x,y point on upper border of left wide bar
   *           vertices[ 9] x,y point on lower border of left wide bar
   *           vertices[10] x,y point on upper border of right wide bar
   *           vertices[11] x,y point on lower border of right wide bar
   *           vertices[12] x,y final top left codeword area
   *           vertices[13] x,y final bottom left codeword area
   *           vertices[14] x,y final top right codeword area
   *           vertices[15] x,y final bottom right codeword area
   * @param upsideDown true if rotated by 180 degree.
   */
  private static void correctVertices(BitMatrix matrix,
                                      ResultPoint[] vertices,
                                      boolean upsideDown) throws NotFoundException {

    boolean isLowLeft = Math.abs(vertices[4].getY() - vertices[5].getY()) < 20.0;
    boolean isLowRight = Math.abs(vertices[6].getY() - vertices[7].getY()) < 20.0;
    if (isLowLeft || isLowRight) {
      throw NotFoundException.getNotFoundInstance();
    } else {
      findWideBarTopBottom(matrix, vertices, 0, 0,  8, 17, upsideDown ? 1 : -1);
      findWideBarTopBottom(matrix, vertices, 1, 0,  8, 17, upsideDown ? -1 : 1);
      findWideBarTopBottom(matrix, vertices, 2, 11, 7, 18, upsideDown ? 1 : -1);
      findWideBarTopBottom(matrix, vertices, 3, 11, 7, 18, upsideDown ? -1 : 1);
      findCrossingPoint(vertices, 12, 4, 5, 8, 10, matrix);
      findCrossingPoint(vertices, 13, 4, 5, 9, 11, matrix);
      findCrossingPoint(vertices, 14, 6, 7, 8, 10, matrix);
      findCrossingPoint(vertices, 15, 6, 7, 9, 11, matrix);
    }
  }

  /**
   * <p>Locate the top or bottom of one of the two wide black bars of a guard pattern.</p>
   *
   * <p>Warning: it only searches along the y axis, so the return points would not be
   * right if the barcode is too curved.</p>
   *
   * @param matrix The bit matrix.
   * @param vertices The 16 vertices located by findVertices(); the result
   *           points are stored into vertices[8], ... , vertices[11].
   * @param offsetVertex The offset of the outer vertex and the inner
   *           vertex (+ 4) to be corrected and (+ 8) where the result is stored.
   * @param startWideBar start of a wide bar.
   * @param lenWideBar length of wide bar.
   * @param lenPattern length of the pattern.
   * @param rowStep +1 if corner should be exceeded towards the bottom, -1 towards the top.
   */
  private static void findWideBarTopBottom(BitMatrix matrix,
                                           ResultPoint[] vertices,
                                           int offsetVertex,
                                           int startWideBar,
                                           int lenWideBar,
                                           int lenPattern,
                                           int rowStep) {
    ResultPoint verticeStart = vertices[offsetVertex];
    ResultPoint verticeEnd = vertices[offsetVertex + 4];

    // Start horizontally at the middle of the bar.
    int endWideBar = startWideBar + lenWideBar;
    float barDiff = verticeEnd.getX() - verticeStart.getX();
    float barStart = verticeStart.getX() + (barDiff * startWideBar) / lenPattern;
    float barEnd = verticeStart.getX() + (barDiff * endWideBar) / lenPattern;
    int x = Math.round((barStart + barEnd) / 2.0f);

    // Start vertically between the preliminary vertices.
    int yStart = Math.round(verticeStart.getY());
    int y = yStart;

    // Find offset of thin bar to the right as additional safeguard.
    int nextBarX = (int)Math.max(barStart, barEnd) + 1;
    while (nextBarX < matrix.getWidth()) {
      if (!matrix.get(nextBarX - 1, y) && matrix.get(nextBarX, y)) {
        break;
      }
      nextBarX++;
    }
    nextBarX -= x;

    boolean isEnd = false;
    while (!isEnd) {
      if (matrix.get(x, y)) {
        // If the thin bar to the right ended, stop as well
        isEnd = !matrix.get(x + nextBarX, y) && !matrix.get(x + nextBarX + 1, y);
        y += rowStep;
        if (y <= 0 || y >= matrix.getHeight() - 1) {
          // End of barcode image reached.
          isEnd = true;
        }
      } else {
        // Look sidewise whether black bar continues? (in the case the image is skewed)
        if (x > 0 && matrix.get(x - 1, y)) {
          x--;
        } else if (x < matrix.getWidth() - 1 && matrix.get(x + 1, y)) {
          x++;
        } else {
          // End of pattern regarding big bar and big gap reached.
          isEnd = true;
          if (y != yStart) {
            // Turn back one step, because target has been exceeded.
            y -= rowStep;
          }
        }
      }
    }

    vertices[offsetVertex + 8] = new ResultPoint(x, y);
  }

  /**
   * <p>Finds the intersection of two lines.</p>
   *
   * @param vertices The reference of the vertices vector
   * @param idxResult Index of result point inside the vertices vector.
   * @param idxLineA1
   * @param idxLineA2 Indices two points inside the vertices vector that define the first line.
   * @param idxLineB1
   * @param idxLineB2 Indices two points inside the vertices vector that define the second line.
   * @param matrix: bit matrix, here only for testing whether the result is inside the matrix.
   * @return Returns true when the result is valid and lies inside the matrix. Otherwise throws an
   * exception.
   */
  private static void findCrossingPoint(ResultPoint[] vertices,
                                        int idxResult,
                                        int idxLineA1, int idxLineA2,
                                        int idxLineB1, int idxLineB2,
                                        BitMatrix matrix) throws NotFoundException {
    ResultPoint result = intersection(vertices[idxLineA1], vertices[idxLineA2],
                                      vertices[idxLineB1], vertices[idxLineB2]);
    if (result == null) {
      throw NotFoundException.getNotFoundInstance();
    }

    int x = Math.round(result.getX());
    int y = Math.round(result.getY());
    if (x < 0 || x >= matrix.getWidth() || y < 0 || y >= matrix.getHeight()) {
      throw NotFoundException.getNotFoundInstance();
    }

    vertices[idxResult] = result;
  }

  /**
   * Computes the intersection between two lines.
   */
  private static ResultPoint intersection(ResultPoint a1, ResultPoint a2, ResultPoint b1, ResultPoint b2) {
    float dxa = a1.getX() - a2.getX();
    float dxb = b1.getX() - b2.getX();
    float dya = a1.getY() - a2.getY();
    float dyb = b1.getY() - b2.getY();

    float p = a1.getX() * a2.getY() - a1.getY() * a2.getX();
    float q = b1.getX() * b2.getY() - b1.getY() * b2.getX();
    float denom = dxa * dyb - dya * dxb;
    if (denom == 0) {
      // Lines don't intersect
      return null;
    }

    float x = (p * dxb - dxa * q) / denom;
    float y = (p * dyb - dya * q) / denom;

    return new ResultPoint(x, y);
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
    int topRowDimension = MathUtils.round(ResultPoint.distance(topLeft, topRight) / moduleWidth);
    int bottomRowDimension = MathUtils.round(ResultPoint.distance(bottomLeft, bottomRight) / moduleWidth);
    return ((((topRowDimension + bottomRowDimension) >> 1) + 8) / 17) * 17;
  }

  /**
   * Computes the y dimension (number of modules in a column) of the PDF417 Code
   * based on vertices of the codeword area and estimated module size.
   *
   * @param topLeft     of codeword area
   * @param topRight    of codeword area
   * @param bottomLeft  of codeword area
   * @param bottomRight of codeword are
   * @param moduleWidth estimated module size
   * @return the number of modules in a row.
   */
  private static int computeYDimension(ResultPoint topLeft,
                                       ResultPoint topRight,
                                       ResultPoint bottomLeft,
                                       ResultPoint bottomRight,
                                       float moduleWidth) {
    int leftColumnDimension = MathUtils.round(ResultPoint.distance(topLeft, bottomLeft) / moduleWidth);
    int rightColumnDimension = MathUtils.round(ResultPoint.distance(topRight, bottomRight) / moduleWidth);
    return (leftColumnDimension + rightColumnDimension) >> 1;
  }

  /**
   * Deskew and over-sample image.
   * 
   * @param vertices   vertices from findVertices()
   * @param dimension  x dimension
   * @param yDimension y dimension
   * @return an over-sampled BitMatrix.
   */
  private BitMatrix sampleLines(ResultPoint[] vertices, int dimension, int yDimension) throws NotFoundException {
    int sampleDimensionX = dimension * 8;
    int sampleDimensionY = yDimension * 4;

    PerspectiveTransform transform = PerspectiveTransform
        .quadrilateralToQuadrilateral(0.0f, 0.0f,
            sampleDimensionX, 0.0f, 0.0f,
            sampleDimensionY, sampleDimensionX,
            sampleDimensionY, vertices[12].getX(),
            vertices[12].getY(), vertices[14].getX(),
            vertices[14].getY(), vertices[13].getX(),
            vertices[13].getY(), vertices[15].getX(),
            vertices[15].getY());

    return GridSampler.getInstance().sampleGrid(image.getBlackMatrix(),
        sampleDimensionX, sampleDimensionY, transform);
  }

}
