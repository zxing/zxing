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

package com.google.zxing.common.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

/**
 * <p>
 * Detects a candidate barcode-like rectangular region within an image. It
 * starts around the center of the image, increases the size of the candidate
 * region until it finds a white rectangular region. By keeping track of the
 * last black points it encountered, it determines the corners of the barcode.
 * </p>
 *
 * @author David Olivier
 */
public final class WhiteRectangleDetector {

  private static final int INIT_SIZE = 30;
  private static final int CORR = 1;

  private final BitMatrix image;
  private final int height;
  private final int width;
  private final int leftInit;
  private final int rightInit;
  private final int downInit;
  private final int upInit;

  /**
   * @throws NotFoundException if image is too small
   */
  public WhiteRectangleDetector(BitMatrix image) throws NotFoundException {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    leftInit = (width - INIT_SIZE) >> 1;
    rightInit = (width + INIT_SIZE) >> 1;
    upInit = (height - INIT_SIZE) >> 1;
    downInit = (height + INIT_SIZE) >> 1;
    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width) {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  /**
   * @throws NotFoundException if image is too small
   */
  public WhiteRectangleDetector(BitMatrix image, int initSize, int x, int y) throws NotFoundException {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    int halfsize = initSize >> 1;
    leftInit = x - halfsize;
    rightInit = x + halfsize;
    upInit = y - halfsize;
    downInit = y + halfsize;
    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width) {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  /**
   * <p>
   * Detects a candidate barcode-like rectangular region within an image. It
   * starts around the center of the image, increases the size of the candidate
   * region until it finds a white rectangular region.
   * </p>
   *
   * @return {@link ResultPoint}[] describing the corners of the rectangular
   *         region. The first and last points are opposed on the diagonal, as
   *         are the second and third. The first point will be the topmost
   *         point and the last, the bottommost. The second point will be
   *         leftmost and the third, the rightmost
   * @throws NotFoundException if no Data Matrix Code can be found
   */
  public ResultPoint[] detect() throws NotFoundException {

    int left = leftInit;
    int right = rightInit;
    int up = upInit;
    int down = downInit;
    boolean sizeExceeded = false;
    boolean aBlackPointFoundOnBorder = true;
    boolean atLeastOneBlackPointFoundOnBorder = false;

    while (aBlackPointFoundOnBorder) {

      aBlackPointFoundOnBorder = false;

      // .....
      // .   |
      // .....
      boolean rightBorderNotWhite = true;
      while (rightBorderNotWhite && right < width) {
        rightBorderNotWhite = containsBlackPoint(up, down, right, false);
        if (rightBorderNotWhite) {
          right++;
          aBlackPointFoundOnBorder = true;
        }
      }

      if (right >= width) {
        sizeExceeded = true;
        break;
      }

      // .....
      // .   .
      // .___.
      boolean bottomBorderNotWhite = true;
      while (bottomBorderNotWhite && down < height) {
        bottomBorderNotWhite = containsBlackPoint(left, right, down, true);
        if (bottomBorderNotWhite) {
          down++;
          aBlackPointFoundOnBorder = true;
        }
      }

      if (down >= height) {
        sizeExceeded = true;
        break;
      }

      // .....
      // |   .
      // .....
      boolean leftBorderNotWhite = true;
      while (leftBorderNotWhite && left >= 0) {
        leftBorderNotWhite = containsBlackPoint(up, down, left, false);
        if (leftBorderNotWhite) {
          left--;
          aBlackPointFoundOnBorder = true;
        }
      }

      if (left < 0) {
        sizeExceeded = true;
        break;
      }

      // .___.
      // .   .
      // .....
      boolean topBorderNotWhite = true;
      while (topBorderNotWhite && up >= 0) {
        topBorderNotWhite = containsBlackPoint(left, right, up, true);
        if (topBorderNotWhite) {
          up--;
          aBlackPointFoundOnBorder = true;
        }
      }

      if (up < 0) {
        sizeExceeded = true;
        break;
      }

      if (aBlackPointFoundOnBorder) {
        atLeastOneBlackPointFoundOnBorder = true;
      }

    }

    if (!sizeExceeded && atLeastOneBlackPointFoundOnBorder) {

      int maxSize = right - left;

      ResultPoint z = null;
      for (int i = 1; i < maxSize; i++) {
        z = getBlackPointOnSegment(left, down - i, left + i, down);
        if (z != null) {
          break;
        }
      }

      if (z == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      ResultPoint t = null;
      //go down right
      for (int i = 1; i < maxSize; i++) {
        t = getBlackPointOnSegment(left, up + i, left + i, up);
        if (t != null) {
          break;
        }
      }

      if (t == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      ResultPoint x = null;
      //go down left
      for (int i = 1; i < maxSize; i++) {
        x = getBlackPointOnSegment(right, up + i, right - i, up);
        if (x != null) {
          break;
        }
      }

      if (x == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      ResultPoint y = null;
      //go up left
      for (int i = 1; i < maxSize; i++) {
        y = getBlackPointOnSegment(right, down - i, right - i, down);
        if (y != null) {
          break;
        }
      }

      if (y == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      return centerEdges(y, z, x, t);

    } else {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  private ResultPoint getBlackPointOnSegment(float aX, float aY, float bX, float bY) {
    int dist = MathUtils.round(MathUtils.distance(aX, aY, bX, bY));
    float xStep = (bX - aX) / dist;
    float yStep = (bY - aY) / dist;

    for (int i = 0; i < dist; i++) {
      int x = MathUtils.round(aX + i * xStep);
      int y = MathUtils.round(aY + i * yStep);
      if (image.get(x, y)) {
        return new ResultPoint(x, y);
      }
    }
    return null;
  }

  /**
   * recenters the points of a constant distance towards the center
   *
   * @param y bottom most point
   * @param z left most point
   * @param x right most point
   * @param t top most point
   * @return {@link ResultPoint}[] describing the corners of the rectangular
   *         region. The first and last points are opposed on the diagonal, as
   *         are the second and third. The first point will be the topmost
   *         point and the last, the bottommost. The second point will be
   *         leftmost and the third, the rightmost
   */
  private ResultPoint[] centerEdges(ResultPoint y, ResultPoint z,
                                    ResultPoint x, ResultPoint t) {

    //
    //       t            t
    //  z                      x
    //        x    OR    z
    //   y                    y
    //

    float yi = y.getX();
    float yj = y.getY();
    float zi = z.getX();
    float zj = z.getY();
    float xi = x.getX();
    float xj = x.getY();
    float ti = t.getX();
    float tj = t.getY();

    if (yi < width / 2) {
      return new ResultPoint[]{
          new ResultPoint(ti - CORR, tj + CORR),
          new ResultPoint(zi + CORR, zj + CORR),
          new ResultPoint(xi - CORR, xj - CORR),
          new ResultPoint(yi + CORR, yj - CORR)};
    } else {
      return new ResultPoint[]{
          new ResultPoint(ti + CORR, tj + CORR),
          new ResultPoint(zi + CORR, zj - CORR),
          new ResultPoint(xi - CORR, xj + CORR),
          new ResultPoint(yi - CORR, yj - CORR)};
    }
  }

  /**
   * Determines whether a segment contains a black point
   *
   * @param a          min value of the scanned coordinate
   * @param b          max value of the scanned coordinate
   * @param fixed      value of fixed coordinate
   * @param horizontal set to true if scan must be horizontal, false if vertical
   * @return true if a black point has been found, else false.
   */
  private boolean containsBlackPoint(int a, int b, int fixed, boolean horizontal) {

    if (horizontal) {
      for (int x = a; x <= b; x++) {
        if (image.get(x, fixed)) {
          return true;
        }
      }
    } else {
      for (int y = a; y <= b; y++) {
        if (image.get(fixed, y)) {
          return true;
        }
      }
    }

    return false;
  }

}