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

        ResultPoint x=null, y=null, z=null, t=null;
        
        final int max_size = right-left;
        
        for (int i = 1; i < max_size; i++){
            ResultPoint a = new ResultPoint(left, down-i);
            ResultPoint b = new ResultPoint(left+i, down);
            z = getBlackPointOnSegment(a, b);
            if (z != null){
                break;
            }
        }
        
        if (z == null){
            throw NotFoundException.getNotFoundInstance();
        }

        //go down right
        for (int i = 1; i < max_size; i++){
            ResultPoint a = new ResultPoint(left, up+i);
            ResultPoint b = new ResultPoint(left+i, up);
            t = getBlackPointOnSegment(a, b);
            if (t != null){
                break;
            }
        }
        
        if (t == null){
            throw NotFoundException.getNotFoundInstance();
        }
        
        //go down left
        for (int i = 1; i < max_size; i++){
            ResultPoint a = new ResultPoint(right, up+i);
            ResultPoint b = new ResultPoint(right-i, up);
            x = getBlackPointOnSegment(a, b);
            if (x != null){
                break;
            }
        }
        
        if (x == null){
            throw NotFoundException.getNotFoundInstance();
        }
        
        //go up left
        for (int i = 1; i < max_size; i++){
            ResultPoint a = new ResultPoint(right, down-i);
            ResultPoint b = new ResultPoint(right-i, down);
            y = getBlackPointOnSegment(a, b);
            if (y != null){
                break;
            }
        }
        
        if (y == null){
            throw NotFoundException.getNotFoundInstance();
        }

        return centerEdges(y, z, x, t);

    } else {
        throw NotFoundException.getNotFoundInstance();
    }
  }
  

    private ResultPoint getBlackPointOnSegment(ResultPoint a, ResultPoint b) {
        int dist = distanceL2(a, b);
        float xStep = (b.getX()-a.getX())/dist;
        float yStep = (b.getY()-a.getY())/dist;
        
        for (int i = 0; i < dist; i++){
            if (image.get(Math.round(a.getX()+i*xStep), Math.round(a.getY()+i*yStep))){
                return new ResultPoint(Math.round(a.getX()+i*xStep), Math.round(a.getY()+i*yStep));
            }
        }
        return null;
    }

    private static int distanceL2(ResultPoint a, ResultPoint b) {
        return (int) Math.round(Math.sqrt((a.getX() - b.getX())
                * (a.getX() - b.getX()) + (a.getY() - b.getY())
                * (a.getY() - b.getY())));
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