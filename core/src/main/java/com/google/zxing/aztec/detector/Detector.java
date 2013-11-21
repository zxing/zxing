/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.aztec.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GridSampler;
import com.google.zxing.common.detector.MathUtils;
import com.google.zxing.common.detector.WhiteRectangleDetector;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

/**
 * Encapsulates logic that can detect an Aztec Code in an image, even if the Aztec Code
 * is rotated or skewed, or partially obscured.
 *
 * @author David Olivier
 * @author Frank Yellin
 */
public final class Detector {

  private final BitMatrix image;

  private boolean compact;
  private int nbLayers;
  private int nbDataBlocks;
  private int nbCenterLayers;
  private int shift;

  public Detector(BitMatrix image) {
    this.image = image;
  }

  public AztecDetectorResult detect() throws NotFoundException {
    return detect(false);
  }

  /**
   * Detects an Aztec Code in an image.
   *
   * @return {@link AztecDetectorResult} encapsulating results of detecting an Aztec Code
   * @throws NotFoundException if no Aztec Code can be found
   */
   public AztecDetectorResult detect(boolean isMirror) throws NotFoundException {

    // 1. Get the center of the aztec matrix
    Point pCenter = getMatrixCenter();

    // 2. Get the center points of the four diagonal points just outside the bull's eye
    //  [topRight, bottomRight, bottomLeft, topLeft]
    ResultPoint[] bullsEyeCorners = getBullsEyeCorners(pCenter);

    if (isMirror) {
      ResultPoint temp = bullsEyeCorners[0];
      bullsEyeCorners[0] = bullsEyeCorners[2];
      bullsEyeCorners[2] = temp;
    }

    // 3. Get the size of the matrix and other parameters from the bull's eye
    extractParameters(bullsEyeCorners);
    
    // 4. Sample the grid
    BitMatrix bits = sampleGrid(image,
                                bullsEyeCorners[shift % 4], 
                                bullsEyeCorners[(shift + 1) % 4],
                                bullsEyeCorners[(shift + 2) % 4], 
                                bullsEyeCorners[(shift + 3) % 4]);

    // 5. Get the corners of the matrix.
    ResultPoint[] corners = getMatrixCornerPoints(bullsEyeCorners);
    
    return new AztecDetectorResult(bits, corners, compact, nbDataBlocks, nbLayers);
  }

  /**
   * Extracts the number of data layers and data blocks from the layer around the bull's eye.
   *
   * @param bullsEyeCorners the array of bull's eye corners
   * @throws NotFoundException in case of too many errors or invalid parameters
   */
  private void extractParameters(ResultPoint[] bullsEyeCorners) throws NotFoundException {
    if (!isValid(bullsEyeCorners[0]) || !isValid(bullsEyeCorners[1]) ||
        !isValid(bullsEyeCorners[2]) || !isValid(bullsEyeCorners[3])) {
      throw NotFoundException.getNotFoundInstance();
    }
    int length = 2 * nbCenterLayers;
    // Get the bits around the bull's eye
    int[] sides = {
        sampleLine(bullsEyeCorners[0], bullsEyeCorners[1], length), // Right side
        sampleLine(bullsEyeCorners[1], bullsEyeCorners[2], length), // Bottom 
        sampleLine(bullsEyeCorners[2], bullsEyeCorners[3], length), // Left side
        sampleLine(bullsEyeCorners[3], bullsEyeCorners[0], length)  // Top 
    };

    // bullsEyeCorners[shift] is the corner of the bulls'eye that has three 
    // orientation marks.  
    // sides[shift] is the row/column that goes from the corner with three
    // orientation marks to the corner with two.
    shift = getRotation(sides, length);

    // Flatten the parameter bits into a single 28- or 40-bit long
    long parameterData = 0;
    for (int i = 0; i < 4; i++) {
      int side = sides[(shift + i) % 4];
      if (compact) {
        // Each side of the form ..XXXXXXX. where Xs are parameter data
        parameterData <<= 7;
        parameterData += (side >> 1) & 0x7F;
      } else {
        // Each side of the form ..XXXXX.XXXXX. where Xs are parameter data
        parameterData <<= 10;
        parameterData += ((side >> 2) & (0x1f << 5)) + ((side >> 1) & 0x1F);
      }
    }
    
    // Corrects parameter data using RS.  Returns just the data portion
    // without the error correction.
    int correctedData = getCorrectedParameterData(parameterData, compact);
    
    if (compact) {
      // 8 bits:  2 bits layers and 6 bits data blocks
      nbLayers = (correctedData >> 6) + 1;
      nbDataBlocks = (correctedData & 0x3F) + 1;
    } else {
      // 16 bits:  5 bits layers and 11 bits data blocks
      nbLayers = (correctedData >> 11) + 1;
      nbDataBlocks = (correctedData & 0x7FF) + 1;
    }
  }

  private static final int[] EXPECTED_CORNER_BITS = {
      0xee0,  // 07340  XXX .XX X.. ...
      0x1dc,  // 00734  ... XXX .XX X..
      0x83b,  // 04073  X.. ... XXX .XX
      0x707,  // 03407 .XX X.. ... XXX
  };

  private static int getRotation(int[] sides, int length) throws NotFoundException {
    // In a normal pattern, we expect to See
    //   **    .*             D       A
    //   *      *
    //
    //   .      *
    //   ..    ..             C       B
    //
    // Grab the 3 bits from each of the sides the form the locator pattern and concatenate
    // into a 12-bit integer.  Start with the bit at A
    int cornerBits = 0;
    for (int side : sides) {
      // XX......X where X's are orientation marks
      int t = ((side >> (length - 2)) << 1) + (side & 1);
      cornerBits = (cornerBits << 3) + t;
    }
    // Mov the bottom bit to the top, so that the three bits of the locator pattern at A are
    // together.  cornerBits is now:
    //  3 orientation bits at A || 3 orientation bits at B || ... || 3 orientation bits at D
    cornerBits = ((cornerBits & 1) << 11) + (cornerBits >> 1);
    // The result shift indicates which element of BullsEyeCorners[] goes into the top-left
    // corner. Since the four rotation values have a Hamming distance of 8, we
    // can easily tolerate two errors.
    for (int shift = 0; shift < 4; shift++) {
      if (Integer.bitCount(cornerBits ^ EXPECTED_CORNER_BITS[shift]) <= 2) {
        return shift;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  /**
   * Corrects the parameter bits using Reed-Solomon algorithm.
   *
   * @param parameterData parameter bits
   * @param compact true if this is a compact Aztec code
   * @throws NotFoundException if the array contains too many errors
   */
  private static int getCorrectedParameterData(long parameterData, boolean compact) throws NotFoundException {
    int numCodewords;
    int numDataCodewords;

    if (compact) {
      numCodewords = 7;
      numDataCodewords = 2;
    } else {
      numCodewords = 10;
      numDataCodewords = 4;
    }

    int numECCodewords = numCodewords - numDataCodewords;
    int[] parameterWords = new int[numCodewords];
    for (int i = numCodewords - 1; i >= 0; --i) {
      parameterWords[i] = (int) parameterData & 0xF;
      parameterData >>= 4;
    }
    try {
      ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(GenericGF.AZTEC_PARAM);
      rsDecoder.decode(parameterWords, numECCodewords);
    } catch (ReedSolomonException ignored) {
      throw NotFoundException.getNotFoundInstance();
    }
    // Toss the error correction.  Just return the data as an integer
    int result = 0;
    for (int i = 0; i < numDataCodewords; i++) {
      result = (result << 4) + parameterWords[i];
    }
    return result;
  }
  
  /**
   * Finds the corners of a bull-eye centered on the passed point.
   * This returns the centers of the diagonal points just outside the bull's eye
   * Returns [topRight, bottomRight, bottomLeft, topLeft]
   * 
   * @param pCenter Center point
   * @return The corners of the bull-eye
   * @throws NotFoundException If no valid bull-eye can be found
   */
  private ResultPoint[] getBullsEyeCorners(Point pCenter) throws NotFoundException {
    
    Point pina = pCenter;
    Point pinb = pCenter;
    Point pinc = pCenter;
    Point pind = pCenter;

    boolean color = true;
    
    for (nbCenterLayers = 1; nbCenterLayers < 9; nbCenterLayers++) {
      Point pouta = getFirstDifferent(pina, color, 1, -1);
      Point poutb = getFirstDifferent(pinb, color, 1, 1);
      Point poutc = getFirstDifferent(pinc, color, -1, 1);
      Point poutd = getFirstDifferent(pind, color, -1, -1);

      //d      a
      //
      //c      b

      if (nbCenterLayers > 2) {
        float q = distance(poutd, pouta) * nbCenterLayers / (distance(pind, pina) * (nbCenterLayers + 2));
        if (q < 0.75 || q > 1.25 || !isWhiteOrBlackRectangle(pouta, poutb, poutc, poutd)) {
          break;
        }
      }

      pina = pouta;
      pinb = poutb;
      pinc = poutc;
      pind = poutd;

      color = !color;
    }

    if (nbCenterLayers != 5 && nbCenterLayers != 7) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    compact = nbCenterLayers == 5;
    
    // Expand the square by .5 pixel in each direction so that we're on the border
    // between the white square and the black square
    ResultPoint pinax = new ResultPoint(pina.getX() + 0.5f, pina.getY() - 0.5f);
    ResultPoint pinbx = new ResultPoint(pinb.getX() + 0.5f, pinb.getY() + 0.5f);
    ResultPoint pincx = new ResultPoint(pinc.getX() - 0.5f, pinc.getY() + 0.5f);
    ResultPoint pindx = new ResultPoint(pind.getX() - 0.5f, pind.getY() - 0.5f);

    // Expand the square so that its corners are the centers of the points
    // just outside the bull's eye.
    return expandSquare(new ResultPoint[]{pinax, pinbx, pincx, pindx},
                        2 * nbCenterLayers - 3,
                        2 * nbCenterLayers);
  }

  /**
   * Finds a candidate center point of an Aztec code from an image
   *
   * @return the center point
   */
  private Point getMatrixCenter() {

    ResultPoint pointA;
    ResultPoint pointB;
    ResultPoint pointC;
    ResultPoint pointD;

    //Get a white rectangle that can be the border of the matrix in center bull's eye or
    try {

      ResultPoint[] cornerPoints = new WhiteRectangleDetector(image).detect();
      pointA = cornerPoints[0];
      pointB = cornerPoints[1];
      pointC = cornerPoints[2];
      pointD = cornerPoints[3];

    } catch (NotFoundException e) {

      // This exception can be in case the initial rectangle is white
      // In that case, surely in the bull's eye, we try to expand the rectangle.
      int cx = image.getWidth() / 2;
      int cy = image.getHeight() / 2;
      pointA = getFirstDifferent(new Point(cx + 7, cy - 7), false, 1, -1).toResultPoint();
      pointB = getFirstDifferent(new Point(cx + 7, cy + 7), false, 1, 1).toResultPoint();
      pointC = getFirstDifferent(new Point(cx - 7, cy + 7), false, -1, 1).toResultPoint();
      pointD = getFirstDifferent(new Point(cx - 7, cy - 7), false, -1, -1).toResultPoint();

    }
    
    //Compute the center of the rectangle
    int cx = MathUtils.round((pointA.getX() + pointD.getX() + pointB.getX() + pointC.getX()) / 4.0f);
    int cy = MathUtils.round((pointA.getY() + pointD.getY() + pointB.getY() + pointC.getY()) / 4.0f);

    // Redetermine the white rectangle starting from previously computed center.
    // This will ensure that we end up with a white rectangle in center bull's eye
    // in order to compute a more accurate center.
    try {
      ResultPoint[] cornerPoints = new WhiteRectangleDetector(image, 15, cx, cy).detect();
      pointA = cornerPoints[0];
      pointB = cornerPoints[1];
      pointC = cornerPoints[2];
      pointD = cornerPoints[3];
    } catch (NotFoundException e) {
      // This exception can be in case the initial rectangle is white
      // In that case we try to expand the rectangle.
      pointA = getFirstDifferent(new Point(cx + 7, cy - 7), false, 1, -1).toResultPoint();
      pointB = getFirstDifferent(new Point(cx + 7, cy + 7), false, 1, 1).toResultPoint();
      pointC = getFirstDifferent(new Point(cx - 7, cy + 7), false, -1, 1).toResultPoint();
      pointD = getFirstDifferent(new Point(cx - 7, cy - 7), false, -1, -1).toResultPoint();
    }
    
    // Recompute the center of the rectangle
    cx = MathUtils.round((pointA.getX() + pointD.getX() + pointB.getX() + pointC.getX()) / 4.0f);
    cy = MathUtils.round((pointA.getY() + pointD.getY() + pointB.getY() + pointC.getY()) / 4.0f);

    return new Point(cx, cy);
  }

  /**
   * Gets the Aztec code corners from the bull's eye corners and the parameters.
   *
   * @param bullsEyeCorners the array of bull's eye corners
   * @return the array of aztec code corners
   */
  private ResultPoint[] getMatrixCornerPoints(ResultPoint[] bullsEyeCorners) {
    return expandSquare(bullsEyeCorners, 2 * nbCenterLayers, getDimension());
  }

  /**
   * Creates a BitMatrix by sampling the provided image.
   * topLeft, topRight, bottomRight, and bottomLeft are the centers of the squares on the
   * diagonal just outside the bull's eye.
   */
  private BitMatrix sampleGrid(BitMatrix image,
                               ResultPoint topLeft,
                               ResultPoint topRight,
                               ResultPoint bottomRight,
                               ResultPoint bottomLeft) throws NotFoundException {
      
    GridSampler sampler = GridSampler.getInstance();
    int dimension = getDimension();

    float low = dimension / 2.0f - nbCenterLayers;
    float high = dimension / 2.0f + nbCenterLayers;

    return sampler.sampleGrid(image,
                              dimension,
                              dimension,
                              low, low,   // topleft
                              high, low,  // topright
                              high, high, // bottomright
                              low, high,  // bottomleft
                              topLeft.getX(), topLeft.getY(),
                              topRight.getX(), topRight.getY(),
                              bottomRight.getX(), bottomRight.getY(),
                              bottomLeft.getX(), bottomLeft.getY());
  }

  /**
   * Samples a line.
   *
   * @param p1   start point (inclusive)
   * @param p2   end point (exclusive)
   * @param size number of bits
   * @return the array of bits as an int (first bit is high-order bit of result)
   */
  private int sampleLine(ResultPoint p1, ResultPoint p2, int size) {
    int result = 0;

    float d = distance(p1, p2);
    float moduleSize = d / size;
    float px = p1.getX();
    float py = p1.getY();
    float dx = moduleSize * (p2.getX() - p1.getX()) / d;
    float dy = moduleSize * (p2.getY() - p1.getY()) / d;
    for (int i = 0; i < size; i++) {
      if (image.get(MathUtils.round(px + i * dx), MathUtils.round(py + i * dy))) {
        result |= 1 << (size - i - 1);
      }
    }
    return result;
  }

  /**
   * @return true if the border of the rectangle passed in parameter is compound of white points only
   *         or black points only
   */
  private boolean isWhiteOrBlackRectangle(Point p1,
                                          Point p2,
                                          Point p3,
                                          Point p4) {

    int corr = 3;

    p1 = new Point(p1.getX() - corr, p1.getY() + corr);
    p2 = new Point(p2.getX() - corr, p2.getY() - corr);
    p3 = new Point(p3.getX() + corr, p3.getY() - corr);
    p4 = new Point(p4.getX() + corr, p4.getY() + corr);

    int cInit = getColor(p4, p1);

    if (cInit == 0) {
      return false;
    }

    int c = getColor(p1, p2);

    if (c != cInit) {
      return false;
    }

    c = getColor(p2, p3);

    if (c != cInit) {
      return false;
    }

    c = getColor(p3, p4);

    return c == cInit;

  }

  /**
   * Gets the color of a segment
   *
   * @return 1 if segment more than 90% black, -1 if segment is more than 90% white, 0 else
   */
  private int getColor(Point p1, Point p2) {
    float d = distance(p1, p2);
    float dx = (p2.getX() - p1.getX()) / d;
    float dy = (p2.getY() - p1.getY()) / d;
    int error = 0;

    float px = p1.getX();
    float py = p1.getY();

    boolean colorModel = image.get(p1.getX(), p1.getY());

    for (int i = 0; i < d; i++) {
      px += dx;
      py += dy;
      if (image.get(MathUtils.round(px), MathUtils.round(py)) != colorModel) {
        error++;
      }
    }

    float errRatio = error / d;

    if (errRatio > 0.1f && errRatio < 0.9f) {
      return 0;
    }

    return (errRatio <= 0.1f) == colorModel ? 1 : -1;
  }

  /**
   * Gets the coordinate of the first point with a different color in the given direction
   */
  private Point getFirstDifferent(Point init, boolean color, int dx, int dy) {
    int x = init.getX() + dx;
    int y = init.getY() + dy;

    while (isValid(x, y) && image.get(x, y) == color) {
      x += dx;
      y += dy;
    }

    x -= dx;
    y -= dy;

    while (isValid(x, y) && image.get(x, y) == color) {
      x += dx;
    }
    x -= dx;

    while (isValid(x, y) && image.get(x, y) == color) {
      y += dy;
    }
    y -= dy;

    return new Point(x, y);
  }

  /**
   * Expand the square represented by the corner points by pushing out equally in all directions
   *
   * @param cornerPoints the corners of the square, which has the bull's eye at its center
   * @param oldSide the original length of the side of the square in the target bit matrix
   * @param newSide the new length of the size of the square in the target bit matrix
   * @return the corners of the expanded square
   */
  private static ResultPoint[] expandSquare(ResultPoint[] cornerPoints, float oldSide, float newSide) {
    float ratio = newSide / (2 * oldSide);
    float dx = cornerPoints[0].getX() - cornerPoints[2].getX();
    float dy = cornerPoints[0].getY() - cornerPoints[2].getY();
    float centerx = (cornerPoints[0].getX() + cornerPoints[2].getX()) / 2.0f;
    float centery = (cornerPoints[0].getY() + cornerPoints[2].getY()) / 2.0f;

    ResultPoint result0 = new ResultPoint(centerx + ratio * dx, centery + ratio * dy);
    ResultPoint result2 = new ResultPoint(centerx - ratio * dx, centery - ratio * dy);

    dx = cornerPoints[1].getX() - cornerPoints[3].getX();
    dy = cornerPoints[1].getY() - cornerPoints[3].getY();
    centerx = (cornerPoints[1].getX() + cornerPoints[3].getX()) / 2.0f;
    centery = (cornerPoints[1].getY() + cornerPoints[3].getY()) / 2.0f;
    ResultPoint result1 = new ResultPoint(centerx + ratio * dx, centery + ratio * dy);
    ResultPoint result3 = new ResultPoint(centerx - ratio * dx, centery - ratio * dy);

    return new ResultPoint[]{result0, result1, result2, result3};
  }

  private boolean isValid(int x, int y) {
    return x >= 0 && x < image.getWidth() && y > 0 && y < image.getHeight();
  }

  private boolean isValid(ResultPoint point) {
    int x = MathUtils.round(point.getX());
    int y = MathUtils.round(point.getY());
    return isValid(x, y);
  }

  private static float distance(Point a, Point b) {
    return MathUtils.distance(a.getX(), a.getY(), b.getX(), b.getY());
  }

  private static float distance(ResultPoint a, ResultPoint b) {
    return MathUtils.distance(a.getX(), a.getY(), b.getX(), b.getY());
  }

  private int getDimension() {
    if (compact) {
      return 4 * nbLayers + 11;
    }
    if (nbLayers <= 4) {
      return 4 * nbLayers + 15;
    }
    return 4 * nbLayers + 2 * ((nbLayers - 4) / 8 + 1) + 15;
  }

  static final class Point {
    private final int x;
    private final int y;

    ResultPoint toResultPoint() {
      return new ResultPoint(getX(), getY());
    }

    Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    int getX() {
      return x;
    }

    int getY() {
      return y;
    }

    @Override
    public String toString() {
      return "<" + x + ' ' + y + '>';
    }
  }
}
