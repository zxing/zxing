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

  private static final int INIT_SIZE = 40;
  private static final int MIN_SIZE = 20;

  private final BitMatrix image;
  private final int height;
  private final int width;

  public WhiteRectangleDetector(BitMatrix image) {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
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

    int left = (width - INIT_SIZE) / 2;
    int right = (width + INIT_SIZE) / 2;
    int up = (height - INIT_SIZE) / 2;
    int down = (height + INIT_SIZE) / 2;
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

      if (right >= width || down >= height || up < 0 || left < 0) {
        sizeExceeded = true;
        break;
      }

      if (aBlackPointFoundOnBorder) {
        atLeastOneBlackPointFoundOnBorder = true;
      }

    }

    if (!sizeExceeded && atLeastOneBlackPointFoundOnBorder) {

      //     t            t
      //z                      x
      //      x    OR    z
      // y                    y

      ResultPoint x = getBlackPoint(up, down, right - 1, false);
      ResultPoint y = getBlackPoint(left, right, down - 1, true);
      ResultPoint z = getBlackPoint(up, down, left + 1, false);
      ResultPoint t = getBlackPoint(left, right, up + 1, true);

      // if the rectangle if perfectly horizontal (mostly in test cases)
      // then we end up with:
      // zt     x
      //
      // y

      if (distance(z, t) < MIN_SIZE) {
        ResultPoint u = getBlackPointInverted(up, down, right - 1, false);
        t = x;
        x = u;
      }

      return centerEdges(y, z, x, t);

    } else {
      throw NotFoundException.getNotFoundInstance();
    }
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

    float yi = y.getX(), yj = y.getY(), zi = z.getX(), zj = z.getY(), xi = x
        .getX(), xj = x.getY(), ti = t.getX(), tj = t.getY();

    final int corr = 1;
    if (yi < width / 2) {
      return new ResultPoint[]{new ResultPoint(ti - corr, tj + corr),
          new ResultPoint(zi + corr, zj + corr),
          new ResultPoint(xi - corr, xj - corr),
          new ResultPoint(yi + corr, yj - corr)};
    } else {
      return new ResultPoint[]{new ResultPoint(ti + corr, tj + corr),
          new ResultPoint(zi + corr, zj - corr),
          new ResultPoint(xi - corr, xj + corr),
          new ResultPoint(yi - corr, yj - corr)};
    }
  }

  // L1 distance (metropolitan distance)
  private static float distance(ResultPoint a, ResultPoint b) {
    return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
  }

  /**
   * Gets the coordinate of an extreme black point of a segment
   *
   * @param a          min value of the scanned coordinate
   * @param b          max value of the scanned coordinate
   * @param fixed      value of fixed coordinate
   * @param horizontal set to true if scan must be horizontal, false if vertical
   * @return {@link ResultPoint} describing the black point. If scan is horizontal,
   *         the returned point is the first encountered if it is on the left of the image,
   *         else the last one. If scan is vertical, the returned point is the first encountered
   *         if it is on the top of the image, else the last one.
   *         {@link ResultPoint} is null if not black point has been found
   */
  private ResultPoint getBlackPoint(int a, int b, int fixed, boolean horizontal) {

    ResultPoint last = null;

    if (horizontal) {
      for (int x = a; x < b; x++) {
        if (image.get(x, fixed)) {
          if (x < width / 2) {
            return new ResultPoint(x, fixed);
          } else {
            while (x < width && image.get(x, fixed)) {
              x++;
            }
            x--;
            last = new ResultPoint(x, fixed);
          }
        }
      }
    } else {
      for (int y = a; y < b; y++) {
        if (image.get(fixed, y)) {
          if (y < height / 2) {
            return new ResultPoint(fixed, y);
          } else {
            while (y < height && image.get(fixed, y)) {
              y++;
            }
            y--;
            last = new ResultPoint(fixed, y);
          }
        }
      }
    }

    return last;
  }

  /**
   * Same as getBlackPoint, but returned point is the last one found.
   *
   * @param a          min value of the scanned coordinate
   * @param b          max value of the scanned coordinate
   * @param fixed      value of fixed coordinate
   * @param horizontal set to true if scan must be horizontal, false if vertical
   * @return {@link ResultPoint} describing the black point.
   */
  private ResultPoint getBlackPointInverted(int a, int b, int fixed, boolean horizontal) {

    if (horizontal) {
      for (int x = b + 1; x >= a; x--) {
        if (image.get(x, fixed)) {
          return new ResultPoint(x, fixed);
        }
      }
    } else {
      for (int y = b + 1; y >= a; y--) {
        if (image.get(fixed, y)) {
          return new ResultPoint(fixed, y);
        }
      }
    }

    return null;
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
      for (int x = a; x < b; x++) {
        if (image.get(x, fixed)) {
          return true;
        }
      }
    } else {
      for (int y = a; y < b; y++) {
        if (image.get(fixed, y)) {
          return true;
				}
			}
		}

		return false;
	}

}