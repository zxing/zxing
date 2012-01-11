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

package com.google.zxing.common.detector
{

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
public final class WhiteRectangleDetector 
{

  private static  var INIT_SIZE:int = 30;
  private static var CORR:int = 1;

  private  var image:BitMatrix ;
  private  var height:int;
  private  var width:int;
  private  var leftInit:int;
  private  var rightInit:int;
  private var downInit:int;
  private  var upInit:int;

  /**
   * @throws NotFoundException if image is too small
   */
   public function WhiteRectangleDetector(image:BitMatrix , initSize:int=-1, x:int=-1, y:int=-1) 
   {
   	if ((initSize==-1)&&( x==-1)&&( y==-1))
   	{
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
   	else
   {
   
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    var halfsize:int = initSize >> 1;
    leftInit = x - halfsize;
    rightInit = x + halfsize;
    upInit = y - halfsize;
    downInit = y + halfsize;
    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width) {
      throw NotFoundException.getNotFoundInstance();
    }
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
  public function detect():Array {

    var left:int = leftInit;
    var right:int = rightInit;
    var up:int = upInit;
    var down:int = downInit;
    var sizeExceeded:Boolean = false;
    var aBlackPointFoundOnBorder:Boolean = true;
    var atLeastOneBlackPointFoundOnBorder:Boolean = false;

    while (aBlackPointFoundOnBorder) {

      aBlackPointFoundOnBorder = false;

      // .....
      // .   |
      // .....
      var rightBorderNotWhite:Boolean = true;
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
      var bottomBorderNotWhite:Boolean = true;
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
      var leftBorderNotWhite:Boolean = true;
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
      var topBorderNotWhite:Boolean = true;
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

      var maxSize:int = right - left;

      var z:ResultPoint = null;
      for (var i:int = 1; i < maxSize; i++) 
      {
        z = getBlackPointOnSegment(left, down - i, left + i, down);
        
        if (z != null) {
          break;
        }
      }

      if (z == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      var t:ResultPoint = null;
      //go down right
      for (var i4:int = 1; i4 < maxSize; i4++) {
        t = getBlackPointOnSegment(left, up + i4, left + i4, up);
        if (t != null) {
          break;
        }
      }

      if (t == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      var x:ResultPoint = null;
      //go down left
      for (var i5:int = 1; i5 < maxSize; i5++) {
        x = getBlackPointOnSegment(right, up + i5, right - i5, up);
        if (x != null) {
          break;
        }
      }

      if (x == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      var y:ResultPoint = null;
      //go up left
      for (i = 1; i < maxSize; i++) {
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

  /**
   * Ends up being a bit faster than Math.round(). This merely rounds its
   * argument to the nearest int, where x.5 rounds up.
   */
  private static function round(d:Number):int {
    return (int) (d + 0.5);
  }

  private function getBlackPointOnSegment(aX:Number, aY:Number, bX:Number, bY:Number):ResultPoint {
    var dist:int = distanceL2(aX, aY, bX, bY);
    var xStep:Number = (bX - aX) / dist;
    var yStep:Number = (bY - aY) / dist;

    for (var i:int = 0; i < dist; i++) {
      var x:int = round(aX + i * xStep);
      var y:int = round(aY + i * yStep);
      if (image._get(x, y)) {
        return new ResultPoint(x, y);
      }
    }
    return null;
  }

  private static function distanceL2(aX:Number, aY:Number, bX:Number, bY:Number):int {
    var xDiff:Number = aX - bX;
    var yDiff:Number = aY - bY;
    return round(Math.sqrt(xDiff * xDiff + yDiff * yDiff));
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
  private function centerEdges(y:ResultPoint , z:ResultPoint,
                                    x:ResultPoint, t:ResultPoint):Array {

    //
    //       t            t
    //  z                      x
    //        x    OR    z
    //   y                    y
    //

    var yi:Number = y.getX();
    var yj:Number = y.getY();
    var zi:Number = z.getX();
    var zj:Number = z.getY();
    var xi:Number = x.getX();
    var xj:Number = x.getY();
    var ti:Number = t.getX();
    var tj:Number = t.getY();

    if (yi < width / 2) {
      return [
          new ResultPoint(ti - CORR, tj + CORR),
          new ResultPoint(zi + CORR, zj + CORR),
          new ResultPoint(xi - CORR, xj - CORR),
          new ResultPoint(yi + CORR, yj - CORR)];
    } else {
      return [
          new ResultPoint(ti + CORR, tj + CORR),
          new ResultPoint(zi + CORR, zj - CORR),
          new ResultPoint(xi - CORR, xj + CORR),
          new ResultPoint(yi - CORR, yj - CORR)];
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
  private function containsBlackPoint(a:int, b:int, fixed:int, horizontal:Boolean):Boolean {

    if (horizontal) {
      for (var x:int = a; x <= b; x++) {
        if (image._get(x, fixed)) {
          return true;
        }
      }
    } else {
      for (var y:int = a; y <= b; y++) {
        if (image._get(fixed, y)) {
          return true;
        }
      }
    }

    return false;
  }

}
}